package org.chovy.canvas.domain.marketing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class HttpMarketingIntegrationContractProbeClient implements MarketingIntegrationContractProbeClient {

    static final String PROBLEM_TYPE_URI = "urn:canvas:marketing-integration:http-probe";

    private final HttpClient httpClient;
    private final String baseUrl;

    @Autowired
    public HttpMarketingIntegrationContractProbeClient(
            @Value("${canvas.marketing-integrations.probe.base-url:}") String baseUrl) {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build(), baseUrl);
    }

    HttpMarketingIntegrationContractProbeClient(HttpClient httpClient, String baseUrl) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
    }

    @Override
    public ProbeResult probe(ProbeTarget target) {
        URI uri = probeUri(target);
        String method = stringMetadata(target, "probeMethod", "GET").toUpperCase(Locale.ROOT);
        if (!"GET".equals(method) && !"HEAD".equals(method)) {
            throw new IllegalArgumentException("unsupported integration probe method: " + method);
        }
        long started = System.nanoTime();
        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofMillis(timeoutMs(target.timeoutMs())))
                    .method(method, HttpRequest.BodyPublishers.noBody())
                    .header("User-Agent", "canvas-marketing-integration-probe/1.0")
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long latencyMs = Math.max(0L, Duration.ofNanos(System.nanoTime() - started).toMillis());
            String status = statusFor(response.statusCode());
            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("transport", "http");
            evidence.put("method", method);
            evidence.put("uriScheme", uri.getScheme());
            evidence.put("uriHost", uri.getHost());
            evidence.put("uriPath", uri.getPath());
            evidence.put("responseBytes", response.body() == null ? 0 : response.body().length());
            return new ProbeResult(
                    status,
                    response.statusCode(),
                    latencyMs,
                    PROBLEM_TYPE_URI,
                    "PASS".equals(status) ? null : "provider probe returned HTTP " + response.statusCode(),
                    "PASS".equals(status) ? "Provider health endpoint passed" : "Provider health endpoint did not pass",
                    evidence);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("integration contract probe interrupted", e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(), e);
        }
    }

    private URI probeUri(ProbeTarget target) {
        String override = stringMetadata(target, "probeUrl", null);
        if (override != null) {
            return absoluteHttpUri(override);
        }
        String apiRoot = required(target.apiRoot(), "apiRoot");
        URI root = URI.create(apiRoot);
        if (!isAbsoluteHttp(root)) {
            if (baseUrl.isBlank()) {
                throw new IllegalArgumentException("relative integration apiRoot requires canvas.marketing-integrations.probe.base-url");
            }
            root = URI.create(baseUrl).resolve(apiRoot.startsWith("/") ? apiRoot.substring(1) : apiRoot);
        }
        String probePath = stringMetadata(target, "probePath", null);
        URI uri = probePath == null ? root : root.resolve(probePath);
        return absoluteHttpUri(uri.toString());
    }

    private URI absoluteHttpUri(String value) {
        URI uri = URI.create(required(value, "probeUrl"));
        if (!isAbsoluteHttp(uri)) {
            throw new IllegalArgumentException("integration probe URL must be absolute HTTP(S)");
        }
        return uri;
    }

    private static boolean isAbsoluteHttp(URI uri) {
        String scheme = uri.getScheme();
        return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    }

    private static String statusFor(int httpStatus) {
        if (httpStatus >= 200 && httpStatus <= 299) {
            return "PASS";
        }
        if (httpStatus >= 300 && httpStatus <= 399) {
            return "WARN";
        }
        return "FAIL";
    }

    private static int timeoutMs(Integer value) {
        if (value == null || value <= 0) {
            return 5000;
        }
        return Math.max(1000, Math.min(value, 60000));
    }

    private static String stringMetadata(ProbeTarget target, String key, String fallback) {
        if (target == null || target.metadata() == null) {
            return fallback;
        }
        Object value = target.metadata().get(key);
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? fallback : text;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
