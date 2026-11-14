package com.networkmonitor;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

public class NetworkMonitorServer {
    private final Map<String,Target> targets = new ConcurrentHashMap<>();
    private final EventLog eventLog = new EventLog();
    private final ConcurrencyController controller = new ConcurrencyController(5);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private HttpServer server;
    private final Path webRoot;
    private final int port;

    public NetworkMonitorServer(Path webRoot, int port) { this.webRoot = webRoot; this.port = port; }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/api/targets", this::handleTargets);
        server.createContext("/api/events", this::handleEvents);
        server.createContext("/api/stats", this::handleStats);
        server.createContext("/", this::handleStatic);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        scheduler.scheduleAtFixedRate(this::runChecks, 0, 15, TimeUnit.SECONDS);
        System.out.println("Real-Time Network Monitor is running at http://localhost:" + port);
    }

    private void runChecks() {
        for (Target target : targets.values()) controller.submit(() -> {
            Checker checker = target.getType().equals("TCP") ? new TCPChecker() : new EndpointChecker();
            String previous = target.getStatus();
            CheckResult result = checker.check(target);
            target.record(result);
            if (!target.getStatus().equals(previous)) eventLog.addEvent(target.getName(), target.getStatus(), result.getMessage());
        });
    }

    private void handleTargets(HttpExchange x) throws IOException {
        addCors(x);
        String method = x.getRequestMethod();
        if (method.equals("OPTIONS")) { send(x, 204, ""); return; }
        if (method.equals("GET")) {
            List<String> rows = new ArrayList<>();
            for (Target t : targets.values()) rows.add(targetJson(t));
            sendJson(x, 200, "[" + String.join(",", rows) + "]"); return;
        }
        if (method.equals("POST")) {
            String body = new String(x.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String name = field(body,"name"), address = field(body,"address"), type = field(body,"type").toUpperCase(Locale.ROOT);
            int port = 0;
            try { port = Integer.parseInt(field(body,"port")); } catch(Exception ignored) {}
            if (name.isBlank() || address.isBlank() || !(type.equals("TCP") || type.equals("HTTP"))) { sendJson(x,400,"{\"error\":\"Name, address and valid check type are required\"}"); return; }
            if (type.equals("TCP") && (port < 1 || port > 65535)) { sendJson(x,400,"{\"error\":\"TCP port must be between 1 and 65535\"}"); return; }
            if (type.equals("HTTP") && !(address.startsWith("http://") || address.startsWith("https://"))) { sendJson(x,400,"{\"error\":\"HTTP endpoint must start with http:// or https://\"}"); return; }
            String id = UUID.randomUUID().toString();
            targets.put(id, new Target(id,name,address,port,type));
            sendJson(x,201,targetJson(targets.get(id))); runChecks(); return;
        }
        if (method.equals("DELETE")) {
            String id = query(x).get("id");
            Target removed = targets.remove(id);
            if (removed == null) sendJson(x,404,"{\"error\":\"Target not found\"}"); else sendJson(x,200,"{\"ok\":true}");
            return;
        }
        sendJson(x,405,"{\"error\":\"Method not allowed\"}");
    }

    private void handleEvents(HttpExchange x) throws IOException {
        addCors(x); List<String> out = new ArrayList<>();
        for (Map<String,String> e : eventLog.getEvents()) out.add("{\"time\":"+q(e.get("time"))+",\"target\":"+q(e.get("target"))+",\"status\":"+q(e.get("status"))+",\"message\":"+q(e.get("message"))+"}");
        sendJson(x,200,"["+String.join(",",out)+"]");
    }
    private void handleStats(HttpExchange x) throws IOException {
        addCors(x); int up=0,down=0,pending=0; double availability=0; long response=0; int measured=0;
        for(Target t:targets.values()){ if(t.getStatus().equals("UP"))up++; else if(t.getStatus().equals("DOWN"))down++; else pending++; if(t.getChecks()>0){availability+=t.getAvailability(); if(t.getResponseTime()>=0){response+=t.getResponseTime();measured++;}} }
        int count=targets.size(); double avgAvailability=count==0?0:availability/count; long avgResponse=measured==0?0:response/measured;
        sendJson(x,200,"{\"total\":"+count+",\"up\":"+up+",\"down\":"+down+",\"pending\":"+pending+",\"availability\":"+String.format(Locale.US,"%.1f",avgAvailability)+",\"avgResponse\":"+avgResponse+"}");
    }
    private void handleStatic(HttpExchange x) throws IOException {
        String path=x.getRequestURI().getPath(); if(path.equals("/"))path="/index.html";
        Path file=webRoot.resolve(path.substring(1)).normalize();
        if(!file.startsWith(webRoot.normalize()) || !Files.exists(file) || Files.isDirectory(file)){send(x,404,"Not found");return;}
        String name=file.getFileName().toString(); String type=name.endsWith(".css")?"text/css":name.endsWith(".js")?"application/javascript":"text/html";
        x.getResponseHeaders().set("Content-Type",type+"; charset=utf-8"); byte[] data=Files.readAllBytes(file); x.sendResponseHeaders(200,data.length); try(OutputStream o=x.getResponseBody()){o.write(data);}
    }
    private static String targetJson(Target t){return "{\"id\":"+q(t.getId())+",\"name\":"+q(t.getName())+",\"address\":"+q(t.getAddress())+",\"port\":"+t.getPort()+",\"type\":"+q(t.getType())+",\"status\":"+q(t.getStatus())+",\"responseTime\":"+t.getResponseTime()+",\"lastChecked\":"+q(t.getLastChecked())+",\"message\":"+q(t.getMessage())+",\"availability\":"+String.format(Locale.US,"%.1f",t.getAvailability())+"}";}
    private static String field(String json,String key){Matcher m=Pattern.compile("\\\""+Pattern.quote(key)+"\\\"\\s*:\\s*(?:\\\"([^\\\"]*)\\\"|([0-9]+))").matcher(json);return m.find()?(m.group(1)!=null?m.group(1):m.group(2)):"";}
    private static String q(String s){if(s==null)return "null";return "\""+s.replace("\\","\\\\").replace("\"","\\\"").replace("\n"," ").replace("\r"," ")+"\"";}
    private static Map<String,String> query(HttpExchange x){Map<String,String> m=new HashMap<>();String q=x.getRequestURI().getRawQuery();if(q!=null)for(String p:q.split("&")){String[] a=p.split("=",2);if(a.length==2)m.put(a[0],a[1]);}return m;}
    private static void addCors(HttpExchange x){x.getResponseHeaders().set("Access-Control-Allow-Origin","*");x.getResponseHeaders().set("Access-Control-Allow-Methods","GET,POST,DELETE,OPTIONS");x.getResponseHeaders().set("Access-Control-Allow-Headers","Content-Type");}
    private static void sendJson(HttpExchange x,int code,String body)throws IOException{x.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");send(x,code,body);}
    private static void send(HttpExchange x,int code,String body)throws IOException{byte[] b=body.getBytes(StandardCharsets.UTF_8);x.sendResponseHeaders(code,b.length);try(OutputStream o=x.getResponseBody()){o.write(b);}}
    public static void main(String[] args)throws Exception{Path root=Path.of(args.length>0?args[0]:"src/main/resources").toAbsolutePath();int port=args.length>1?Integer.parseInt(args[1]):8080;new NetworkMonitorServer(root,port).start();}
}
