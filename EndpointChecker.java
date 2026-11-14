package com.networkmonitor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class EndpointChecker extends Checker {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).followRedirects(HttpClient.Redirect.NORMAL).build();
    @Override public CheckResult check(Target target) {
        long start = System.nanoTime();
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(target.getAddress())).timeout(Duration.ofSeconds(6)).GET().build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            long ms = (System.nanoTime() - start) / 1_000_000;
            int code = response.statusCode();
            boolean ok = code >= 200 && code < 400;
            return new CheckResult(ok, ms, "HTTP " + code);
        } catch (Exception e) {
            long ms = (System.nanoTime() - start) / 1_000_000;
            return new CheckResult(false, ms, e.getClass().getSimpleName() + ": endpoint unavailable");
        }
    }
}
