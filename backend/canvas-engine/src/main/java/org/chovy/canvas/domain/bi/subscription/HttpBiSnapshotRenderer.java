package org.chovy.canvas.domain.bi.subscription;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class HttpBiSnapshotRenderer implements BiSnapshotRenderer {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final List<String> endpointUrls;
    private final int timeoutMs;
    private final AtomicInteger nextEndpointIndex = new AtomicInteger();

    @Autowired
    public HttpBiSnapshotRenderer(WebClient.Builder webClientBuilder,
                                  ObjectMapper objectMapper,
                                  @Value("${canvas.bi.delivery.snapshot.renderer.enabled:false}") boolean enabled,
                                  @Value("${canvas.bi.delivery.snapshot.renderer.url:}") String endpointUrl,
                                  @Value("${canvas.bi.delivery.snapshot.renderer.urls:}") String endpointUrls,
                                  @Value("${canvas.bi.delivery.snapshot.renderer.timeout-ms:15000}") int timeoutMs) {
        this.webClientBuilder = webClientBuilder;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.endpointUrls = endpointUrls(endpointUrl, endpointUrls);
        this.timeoutMs = Math.max(1000, timeoutMs);
    }

    public HttpBiSnapshotRenderer(WebClient.Builder webClientBuilder,
                                  ObjectMapper objectMapper,
                                  boolean enabled,
                                  String endpointUrl,
                                  int timeoutMs) {
        this(webClientBuilder, objectMapper, enabled, endpointUrl, "", timeoutMs);
    }

    @Override
    public boolean configured() {
        return enabled && !endpointUrls.isEmpty();
    }

    @Override
    public BiSnapshotRenderResult render(BiSnapshotRenderRequest request) {
        if (!configured()) {
            throw new IllegalStateException("BI snapshot renderer is not configured");
        }
        RuntimeException lastFailure = null;
        int startIndex = Math.floorMod(nextEndpointIndex.getAndIncrement(), endpointUrls.size());
        for (int attempt = 0; attempt < endpointUrls.size(); attempt++) {
            String endpointUrl = endpointUrls.get((startIndex + attempt) % endpointUrls.size());
            try {
                return render(endpointUrl, request);
            } catch (RuntimeException e) {
                lastFailure = e;
            }
        }
        if (endpointUrls.size() == 1 && lastFailure != null) {
            throw lastFailure;
        }
        throw new IllegalStateException("BI snapshot renderer cluster failed across "
                + endpointUrls.size() + " endpoint(s)", lastFailure);
    }

    private BiSnapshotRenderResult render(String endpointUrl, BiSnapshotRenderRequest request) {
        Map<String, Object> response = postRenderRequest(endpointUrl, requestBody(request));
        String contentType = stringValue(response.get("contentType"));
        String format = normalizeFormat(stringValue(response.getOrDefault("format", request.format())));
        if (!hasText(contentType)) {
            contentType = contentType(format);
        }
        String base64 = stringValue(firstValue(response, "base64", "bytes", "data"));
        if (!hasText(base64)) {
            throw new IllegalStateException("BI snapshot renderer response did not include base64 image data");
        }
        return new BiSnapshotRenderResult(format, contentType, Base64.getDecoder().decode(stripDataUrl(base64)));
    }

    private List<String> endpointUrls(String primaryEndpointUrl, String clusterEndpointUrls) {
        List<String> urls = new ArrayList<>();
        addEndpointUrl(urls, primaryEndpointUrl);
        if (hasText(clusterEndpointUrls)) {
            for (String endpointUrl : clusterEndpointUrls.split(",")) {
                addEndpointUrl(urls, endpointUrl);
            }
        }
        return List.copyOf(urls);
    }

    private void addEndpointUrl(List<String> urls, String endpointUrl) {
        if (!hasText(endpointUrl)) {
            return;
        }
        String normalized = endpointUrl.trim();
        if (!urls.contains(normalized)) {
            urls.add(normalized);
        }
    }

    protected Map<String, Object> postRenderRequest(String url, Map<String, Object> body) {
        String json = webClientBuilder.build()
                .post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofMillis(timeoutMs));
        if (!hasText(json)) {
            throw new IllegalStateException("BI snapshot renderer returned empty response");
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("BI snapshot renderer returned invalid JSON", e);
        }
    }

    private Map<String, Object> requestBody(BiSnapshotRenderRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("html", request.html());
        body.put("resourceUrl", request.resourceUrl());
        body.put("format", normalizeFormat(request.format()));
        body.put("width", request.width());
        body.put("height", request.height());
        body.put("scale", request.scale());
        body.put("metadata", request.metadata());
        return body;
    }

    private Object firstValue(Map<String, Object> values, String... keys) {
        for (String key : keys) {
            if (values.containsKey(key)) {
                return values.get(key);
            }
        }
        return null;
    }

    private String stripDataUrl(String value) {
        int comma = value.indexOf(',');
        if (value.startsWith("data:") && comma >= 0) {
            return value.substring(comma + 1);
        }
        return value;
    }

    private String normalizeFormat(String value) {
        String format = value == null || value.isBlank() ? "PNG" : value.trim().toUpperCase(Locale.ROOT);
        return "JPG".equals(format) ? "JPEG" : format;
    }

    private String contentType(String format) {
        return "JPEG".equals(normalizeFormat(format)) ? "image/jpeg" : "image/png";
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank() && !"null".equalsIgnoreCase(value);
    }
}
