package org.chovy.canvas.infrastructure.doris;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.dal.dataobject.CdpEventLogDO;
import org.chovy.canvas.domain.warehouse.CdpWarehouseEventSink;
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

@Slf4j
@Component
public class DorisCdpEventStreamLoader implements CdpWarehouseEventSink {

    private static final DateTimeFormatter DORIS_DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final boolean enabled;
    private final String streamLoadUrl;
    private final String username;
    private final String password;
    private final Duration timeout;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public DorisCdpEventStreamLoader(
            @Value("${canvas.doris.enabled:false}") boolean enabled,
            @Value("${canvas.doris.cdp-event-stream-load-url:http://localhost:8040/api/canvas_ods/cdp_event_log/_stream_load}") String streamLoadUrl,
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

    DorisCdpEventStreamLoader(boolean enabled,
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

    @Override
    public void writeAccepted(CdpEventLogDO event) {
        boolean loaded = load(List.of(event));
        if (enabled && !loaded) {
            throw new IllegalStateException("Doris CDP event Stream Load failed");
        }
    }

    public boolean load(List<CdpEventLogDO> events) {
        if (!enabled || events == null || events.isEmpty()) {
            return false;
        }
        try {
            String body = toJsonLines(events);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(streamLoadUrl))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("format", "json")
                    .header("read_json_by_line", "true")
                    .header("label", "cdp_event_" + UUID.randomUUID())
                    .header("Authorization", authorizationHeader())
                    .PUT(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300 && streamLoadSucceeded(response.body())) {
                log.debug("[DORIS_CDP_EVENT_LOAD] wrote {} events", events.size());
                return true;
            }
            log.warn("[DORIS_CDP_EVENT_LOAD] failed status={} body={}", response.statusCode(), response.body());
            return false;
        } catch (Exception e) {
            log.warn("[DORIS_CDP_EVENT_LOAD] error: {}", e.getMessage());
            return false;
        }
    }

    String toJsonLines(List<CdpEventLogDO> events) {
        return events.stream()
                .map(this::toJsonLine)
                .collect(Collectors.joining("\n"));
    }

    private String toJsonLine(CdpEventLogDO event) {
        try {
            return objectMapper.writeValueAsString(toRow(event));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize CDP event row", e);
        }
    }

    private Map<String, Object> toRow(CdpEventLogDO event) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("tenant_id", event.getTenantId());
        row.put("event_log_id", event.getId() == null ? 0L : event.getId());
        row.put("message_id", event.getMessageId());
        row.put("event_code", event.getEventCode());
        row.put("user_id", event.getUserId());
        row.put("anonymous_id", event.getAnonymousId());
        row.put("session_id", event.getSessionId());
        row.put("device_id", event.getDeviceId());
        row.put("platform", event.getPlatform());
        row.put("properties", jsonOrNull(event.getProperties()));
        row.put("event_time", formatDateTime(event.getEventTime()));
        row.put("received_at", formatDateTime(event.getReceivedAt()));
        return row;
    }

    private JsonNode jsonOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(raw);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("CDP event properties JSON is invalid", e);
        }
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
}
