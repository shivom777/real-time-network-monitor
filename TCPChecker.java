package com.networkmonitor;

import java.net.InetSocketAddress;
import java.net.Socket;

public class TCPChecker extends Checker {
    @Override public CheckResult check(Target target) {
        long start = System.nanoTime();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(target.getAddress(), target.getPort()), 3000);
            long ms = (System.nanoTime() - start) / 1_000_000;
            return new CheckResult(true, ms, "TCP connection accepted");
        } catch (Exception e) {
            long ms = (System.nanoTime() - start) / 1_000_000;
            return new CheckResult(false, ms, e.getClass().getSimpleName() + ": connection failed");
        }
    }
}
