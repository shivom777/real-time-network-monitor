package com.networkmonitor;

import java.util.concurrent.*;

public class ConcurrencyController {
    private final ExecutorService pool;
    public ConcurrencyController(int limit) { pool = Executors.newFixedThreadPool(limit); }
    public void submit(Runnable task) { pool.submit(task); }
    public void shutdown(){ pool.shutdownNow(); }
}
