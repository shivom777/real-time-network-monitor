package com.networkmonitor;

public class CheckResult {
    private final boolean up;
    private final long responseTime;
    private final String message;
    public CheckResult(boolean up, long responseTime, String message) {
        this.up = up; this.responseTime = responseTime; this.message = message;
    }
    public boolean isUp(){ return up; }
    public long getResponseTime(){ return responseTime; }
    public String getMessage(){ return message; }
}
