package org.chovy.canvas.infrastructure.doris;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.dal.dataobject.CanvasExecutionTraceDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Apache Doris Stream Load writer for execution traces.
 */
@Slf4j
@Component
public class DorisStreamLoader {

    private final boolean enabled;
    private final String streamLoadUrl;
    private final String username;
    private final String password;
    private final Duration timeout;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter DORIS_DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    public DorisStreamLoader(
            @Value("${canvas.doris.enabled:false}") boolean enabled,
            @Value("${canvas.doris.stream-load-url:http://localhost:8040/api/canvas_ods/canvas_execution_trace/_stream_load}") String streamLoadUrl,
            @Value("${canvas.doris.username:root}") String username,
            @Value("${canvas.doris.password:}") String password,
            @Value("${canvas.doris.stream-load-timeout-ms:3000}") long timeoutMs) {
        this.enabled = enabled;
        this.streamLoadUrl = streamLoadUrl;
        this.username = username;
        this.password = password;
        this.timeout = Duration.ofMillis(timeoutMs);
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
        this.objectMapper = new ObjectMapper();
    }

    DorisStreamLoader(boolean enabled,
                      String streamLoadUrl,
                      String username,
                      String password,
                      Duration timeout,
                      HttpClient httpClient,
                      ObjectMapper objectMapper) {
        this.enabled = enabled;
        this.streamLoadUrl = streamLoadUrl;
        this.username = username;
        this.password = password;
        this.timeout = timeout;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public boolean load(List<CanvasExecutionTraceDO> traces) {
        if (!enabled || traces == null || traces.isEmpty()) {
            return false;
        }
        try {
            String body = toJsonLines(traces);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(streamLoadUrl))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("format", "json")
                    .header("read_json_by_line", "true")
                    .header("label", "trace_" + UUID.randomUUID())
                    .header("Authorization", authorizationHeader())
                    .PUT(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300 && streamLoadSucceeded(response.body())) {
                log.debug("[DORIS_STREAM_LOAD] wrote {} traces", traces.size());
                return true;
            }
            log.warn("[DORIS_STREAM_LOAD] failed status={} body={}", response.statusCode(), response.body());
            return false;
        } catch (Exception e) {
            log.warn("[DORIS_STREAM_LOAD] error: {}", e.getMessage());
            return false;
        }
    }

    String toJsonLines(List<CanvasExecutionTraceDO> traces) {
        return traces.stream()
                .map(this::toJsonLine)
                .collect(Collectors.joining("\n"));
    }

    private String toJsonLine(CanvasExecutionTraceDO trace) {
        try {
            return objectMapper.writeValueAsString(toRow(trace));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize trace row", e);
        }
    }

    private Map<String, Object> toRow(CanvasExecutionTraceDO trace) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("trace_id", trace.getId() == null ? 0L : trace.getId());
        row.put("tenant_id", trace.getTenantId());
        row.put("execution_id", trace.getExecutionId());
        row.put("node_id", trace.getNodeId());
        row.put("node_type", trace.getNodeType());
        row.put("node_name", trace.getNodeName());
        row.put("status", trace.getStatus());
        row.put("input_data", trace.getInputData());
        row.put("output_data", trace.getOutputData());
        row.put("error_msg", trace.getErrorMsg());
        row.put("started_at", formatDateTime(trace.getStartedAt()));
        row.put("finished_at", formatDateTime(trace.getFinishedAt()));
        row.put("duration_ms", trace.getDurationMs());
        row.put("created_at", formatDateTime(firstNonNull(trace.getFinishedAt(), trace.getStartedAt(), LocalDateTime.now())));
        return row;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : DORIS_DATETIME_FORMATTER.format(dateTime);
    }

    private String authorizationHeader() {
        String token = username + ":" + (password == null ? "" : password);
        return "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    private boolean streamLoadSucceeded(String body) {
        return body == null
                || body.isBlank()
                || body.contains("\"Status\":\"Success\"")
                || body.contains("\"Status\":\"Publish Timeout\"");
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
