package com.networkmonitor;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventLog {
    private final List<Map<String,String>> events = new CopyOnWriteArrayList<>();
    public void addEvent(String target, String status, String message) {
        Map<String,String> e = new LinkedHashMap<>();
        e.put("time", Instant.now().toString()); e.put("target", target); e.put("status", status); e.put("message", message);
        events.add(0, e);
        if (events.size() > 200) events.remove(events.size()-1);
    }
    public List<Map<String,String>> getEvents(){ return new ArrayList<>(events); }
}
