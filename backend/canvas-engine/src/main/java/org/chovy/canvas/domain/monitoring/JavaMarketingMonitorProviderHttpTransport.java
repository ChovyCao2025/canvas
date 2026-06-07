package org.chovy.canvas.domain.monitoring;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JavaMarketingMonitorProviderHttpTransport implements MarketingMonitorProviderHttpTransport {

    private final HttpClient httpClient;

    public JavaMarketingMonitorProviderHttpTransport() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build());
    }

    JavaMarketingMonitorProviderHttpTransport(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public MarketingMonitorProviderHttpResponse execute(MarketingMonitorProviderHttpRequest request) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(request.uri())
                .timeout(Duration.ofSeconds(30));
        request.headers().forEach(builder::header);
        if ("POST".equals(request.method())) {
            builder.POST(HttpRequest.BodyPublishers.ofString(request.body()));
        } else {
            builder.GET();
        }
        try {
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            Map<String, String> headers = new LinkedHashMap<>();
            response.headers().map().forEach((key, values) -> {
                if (!values.isEmpty()) {
                    headers.put(key, values.get(0));
                }
            });
            return new MarketingMonitorProviderHttpResponse(response.statusCode(), response.body(), headers);
        } catch (IOException ex) {
            throw new IllegalStateException("monitoring provider HTTP request failed", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("monitoring provider HTTP request interrupted", ex);
        }
    }
}
