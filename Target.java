package com.networkmonitor;

import java.time.Instant;

public class Target {
    private final String id;
    private final String name;
    private final String address;
    private final int port;
    private final String type;
    private volatile String status = "PENDING";
    private volatile long responseTime = -1;
    private volatile String lastChecked = "";
    private volatile String message = "Waiting for first check";
    private long checks;
    private long successes;

    public Target(String id, String name, String address, int port, String type) {
        this.id = id; this.name = name; this.address = address; this.port = port; this.type = type;
    }
    public String getId(){ return id; }
    public String getName(){ return name; }
    public String getAddress(){ return address; }
    public int getPort(){ return port; }
    public String getType(){ return type; }
    public String getStatus(){ return status; }
    public long getResponseTime(){ return responseTime; }
    public String getLastChecked(){ return lastChecked; }
    public String getMessage(){ return message; }
    public synchronized long getChecks(){ return checks; }
    public synchronized long getSuccesses(){ return successes; }
    public synchronized double getAvailability(){ return checks == 0 ? 0 : successes * 100.0 / checks; }
    public synchronized void record(CheckResult result) {
        checks++;
        if (result.isUp()) successes++;
        status = result.isUp() ? "UP" : "DOWN";
        responseTime = result.getResponseTime();
        lastChecked = Instant.now().toString();
        message = result.getMessage();
    }
}
