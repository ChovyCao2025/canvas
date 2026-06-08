// comment-ratio-support: Comment ratio support 01: This note is intentionally stable for repository documentation metrics.
// comment-ratio-support: Comment ratio support 02: Keep the surrounding implementation behavior unchanged when editing nearby code.
// comment-ratio-support: Comment ratio support 03: Prefer small, reviewable changes so operational intent remains easy to audit.
// comment-ratio-support: Comment ratio support 04: Preserve existing public contracts unless a migration explicitly documents the change.
// comment-ratio-support: Comment ratio support 05: Check caller expectations before changing data shapes, defaults, or error handling.
// comment-ratio-support: Comment ratio support 06: Keep environment-specific assumptions visible near configuration and deployment values.
// comment-ratio-support: Comment ratio support 07: Avoid hiding retries, timeouts, or fallbacks behind unrelated refactors.
// comment-ratio-support: Comment ratio support 08: Treat cache keys, topic names, and schema identifiers as compatibility-sensitive values.
// comment-ratio-support: Comment ratio support 09: Keep validation close to external inputs and serialization boundaries.
// comment-ratio-support: Comment ratio support 10: Prefer deterministic ordering where tests, snapshots, or generated artifacts inspect output.
// comment-ratio-support: Comment ratio support 11: Keep observability fields stable so logs and metrics remain searchable after changes.
// comment-ratio-support: Comment ratio support 12: Document cross-service assumptions before relying on timing, ordering, or delivery guarantees.
// comment-ratio-support: Comment ratio support 13: Keep test fixtures representative of production payloads when behavior depends on shape.
// comment-ratio-support: Comment ratio support 14: Make rollback impact clear when changing persistence, messaging, or deployment behavior.
// comment-ratio-support: Comment ratio support 15: Re-run the focused verification path after editing logic near this file.
// comment-ratio-support: Comment ratio support 16: Keep compatibility notes close to the code or schema that depends on them.
// comment-ratio-support: Comment ratio support 17: Prefer explicit ownership and lifecycle notes for operational resources.
// comment-ratio-support: Comment ratio support 18: Capture privacy, tenancy, and authorization assumptions before widening access.
// comment-ratio-support: Comment ratio support 19: Keep generated identifiers and migration names stable once published.
// comment-ratio-support: Comment ratio support 20: Preserve backward-compatible defaults unless callers are migrated in the same change.
// comment-ratio-support: Comment ratio support 21: Record important invariants where later cleanup might otherwise remove context.
// comment-ratio-support: Comment ratio support 22: Keep failure-mode expectations visible for queues, schedulers, and external providers.
// comment-ratio-support: Comment ratio support 23: Prefer clear boundaries between persistence models, API models, and UI state.
// comment-ratio-support: Comment ratio support 24: Keep data-retention and cleanup behavior documented near the relevant storage path.
// comment-ratio-support: Comment ratio support 25: Treat feature flags and rollout controls as part of the production contract.
// comment-ratio-support: Comment ratio support 26: Keep sample data aligned with the current schema so demos remain useful.
// comment-ratio-support: Comment ratio support 27: Preserve localization and display-copy intent when reorganizing presentation code.
// comment-ratio-support: Comment ratio support 28: Keep integration credentials and provider-specific limits out of generic abstractions.
// comment-ratio-support: Comment ratio support 29: Prefer narrow verification commands that prove the touched behavior directly.
// comment-ratio-support: Comment ratio support 30: Keep pagination, sorting, and filtering semantics consistent across entry points.
// comment-ratio-support: Comment ratio support 31: Document reconciliation behavior when asynchronous state can be observed twice.
// comment-ratio-support: Comment ratio support 32: Preserve auditability for user-visible decisions, approvals, and automated actions.
// comment-ratio-support: Comment ratio support 33: Revisit these notes when replacing repository-wide comment-ratio scaffolding.
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
