package org.chovy.canvas.domain.warehouse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;

@Service
public class HttpCdpWarehouseExternalRealtimeJobProbeClient
        implements CdpWarehouseExternalRealtimeJobProbeClient {

    private static final String RUNNING = "RUNNING";
    private static final String PAUSED = "PAUSED";
    private static final String FAILED = "FAILED";
    private static final String STOPPED = "STOPPED";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Duration timeout;

    @Autowired
    public HttpCdpWarehouseExternalRealtimeJobProbeClient(
            ObjectMapper objectMapper,
            @Value("${canvas.warehouse.external-realtime-job-probe.http-timeout-ms:3000}") long timeoutMs) {
        this(objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(timeoutMs, 1)))
                .build(), Duration.ofMillis(Math.max(timeoutMs, 1)));
    }

    HttpCdpWarehouseExternalRealtimeJobProbeClient(ObjectMapper objectMapper,
                                                  HttpClient httpClient,
                                                  Duration timeout) {
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.httpClient = httpClient;
        this.timeout = timeout == null ? Duration.ofSeconds(3) : timeout;
    }

    @Override
    public ProbeResult probe(ProbeTarget target) {
        if (target == null) {
            throw new IllegalArgumentException("probe target is required");
        }
        String endpointUrl = required(target.endpointUrl(), "endpointUrl");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpointUrl))
                    .timeout(timeout)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return new ProbeResult(FAILED,
                        "external probe returned HTTP " + response.statusCode(),
                        payload(target, response.statusCode(), null, response.body()),
                        engineJobId(target));
            }
            JsonNode json = parse(response.body());
            String runtimeStatus = runtimeStatus(target.engineType(), json);
            return new ProbeResult(runtimeStatus,
                    message(target.engineType(), runtimeStatus, json),
                    payload(target, response.statusCode(), json, response.body()),
                    engineJobId(target));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("external probe failed: " + e.getMessage(), e);
        }
    }

    private String runtimeStatus(String engineType, JsonNode json) {
        String engine = upper(engineType);
        if ("FLINK_REST".equals(engine)) {
            return flinkStatus(text(json, "state", "jobStatus", "status"));
        }
        if ("KAFKA_CONNECT".equals(engine)) {
            return kafkaConnectStatus(json);
        }
        if ("DORIS_ROUTINE_LOAD".equals(engine)) {
            return dorisStatus(text(json, "state", "job_state", "JobState", "status", "Status"));
        }
        return genericStatus(text(json, "runtimeStatus", "runtime_status", "state", "status", "health"));
    }

    private String flinkStatus(String value) {
        String state = upper(value);
        if ("FAILED".equals(state) || "FAILING".equals(state)) {
            return FAILED;
        }
        if ("CANCELED".equals(state) || "CANCELLED".equals(state) || "FINISHED".equals(state)) {
            return STOPPED;
        }
        if ("SUSPENDED".equals(state)) {
            return PAUSED;
        }
        return RUNNING;
    }

    private String kafkaConnectStatus(JsonNode json) {
        String connectorState = upper(text(path(json, "connector"), "state"));
        if ("FAILED".equals(connectorState)) {
            return FAILED;
        }
        if ("PAUSED".equals(connectorState)) {
            return PAUSED;
        }
        if ("DESTROYED".equals(connectorState) || "STOPPED".equals(connectorState)) {
            return STOPPED;
        }
        JsonNode tasks = json == null ? null : json.get("tasks");
        if (tasks != null && tasks.isArray()) {
            boolean allPaused = tasks.size() > 0;
            for (JsonNode task : tasks) {
                String taskState = upper(text(task, "state"));
                if ("FAILED".equals(taskState)) {
                    return FAILED;
                }
                allPaused = allPaused && "PAUSED".equals(taskState);
            }
            if (allPaused) {
                return PAUSED;
            }
        }
        return RUNNING;
    }

    private String dorisStatus(String value) {
        String state = upper(value);
        if ("FAILED".equals(state) || "CANCELLED".equals(state) || "CANCELED".equals(state)
                || "ERROR".equals(state) || "ABORTED".equals(state)) {
            return FAILED;
        }
        if ("PAUSED".equals(state) || "NEED_SCHEDULE".equals(state)) {
            return PAUSED;
        }
        if ("STOPPED".equals(state) || "FINISHED".equals(state)) {
            return STOPPED;
        }
        return RUNNING;
    }

    private String genericStatus(String value) {
        String state = upper(value);
        if ("FAILED".equals(state) || "FAIL".equals(state) || "DOWN".equals(state) || "ERROR".equals(state)) {
            return FAILED;
        }
        if ("PAUSED".equals(state)) {
            return PAUSED;
        }
        if ("STOPPED".equals(state) || "FINISHED".equals(state)) {
            return STOPPED;
        }
        return RUNNING;
    }

    private String message(String engineType, String runtimeStatus, JsonNode json) {
        String detail = text(json, "message", "error", "errorMessage");
        if (detail != null && !detail.isBlank()) {
            return detail;
        }
        return upper(engineType) + " probe mapped to " + runtimeStatus;
    }

    private JsonNode parse(String body) {
        if (body == null || body.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            throw new IllegalStateException("external probe response JSON is invalid", e);
        }
    }

    private String payload(ProbeTarget target, int httpStatus, JsonNode json, String body) {
        try {
            var node = objectMapper.createObjectNode();
            node.put("source", "EXTERNAL_REALTIME_JOB_PROBE");
            node.put("probeTargetId", target.id());
            node.put("engineType", target.engineType());
            node.put("endpointUrl", target.endpointUrl());
            node.put("httpStatus", httpStatus);
            if (json != null) {
                node.set("response", json);
            } else if (body != null && !body.isBlank()) {
                node.put("responseBody", body);
            }
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return "{\"source\":\"EXTERNAL_REALTIME_JOB_PROBE\"}";
        }
    }

    private String engineJobId(ProbeTarget target) {
        if (hasText(target.externalJobId())) {
            return target.externalJobId().trim();
        }
        if (hasText(target.connectorName())) {
            return target.connectorName().trim();
        }
        return null;
    }

    private JsonNode path(JsonNode json, String field) {
        return json == null ? null : json.get(field);
    }

    private String text(JsonNode json, String... fields) {
        if (json == null || fields == null) {
            return null;
        }
        for (String field : fields) {
            JsonNode value = json.get(field);
            if (value != null && !value.isNull()) {
                return value.asText();
            }
        }
        return null;
    }

    private String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private String upper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
