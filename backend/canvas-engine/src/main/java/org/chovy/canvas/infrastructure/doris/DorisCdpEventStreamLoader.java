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
/**
 * DorisCdpEventStreamLoader 封装本模块的核心职责、输入输出结构和协作边界。
 */
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
    /**
     * 初始化 DorisCdpEventStreamLoader 实例。
     *
     * @param enabled enabled 参数，用于 DorisCdpEventStreamLoader 流程中的校验、计算或对象转换。
     * @param streamLoadUrl stream load url 参数，用于 DorisCdpEventStreamLoader 流程中的校验、计算或对象转换。
     * @param username 操作人标识，用于审计和权限判断。
     * @param password password 参数，用于 DorisCdpEventStreamLoader 流程中的校验、计算或对象转换。
     * @param timeoutMs 时间参数，用于计算窗口、过期或审计时间。
     */
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

    /**
     * 初始化 DorisCdpEventStreamLoader 实例。
     *
     * @param enabled enabled 参数，用于 DorisCdpEventStreamLoader 流程中的校验、计算或对象转换。
     * @param streamLoadUrl stream load url 参数，用于 DorisCdpEventStreamLoader 流程中的校验、计算或对象转换。
     * @param username 操作人标识，用于审计和权限判断。
     * @param password password 参数，用于 DorisCdpEventStreamLoader 流程中的校验、计算或对象转换。
     * @param timeout 时间参数，用于计算窗口、过期或审计时间。
     * @param httpClient 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
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
    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param event event 参数，用于 writeAccepted 流程中的校验、计算或对象转换。
     */
    public void writeAccepted(CdpEventLogDO event) {
        boolean loaded = load(List.of(event));
        if (enabled && !loaded) {
            throw new IllegalStateException("Doris CDP event Stream Load failed");
        }
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param events events 参数，用于 load 流程中的校验、计算或对象转换。
     * @return 返回 load 的布尔判断结果。
     */
    public boolean load(List<CdpEventLogDO> events) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
                    // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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
            // 汇总前面计算出的状态和明细，返回给调用方。
            return false;
        }
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param events events 参数，用于 toJsonLines 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    String toJsonLines(List<CdpEventLogDO> events) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return events.stream()
                .map(this::toJsonLine)
                .collect(Collectors.joining("\n"));
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param event event 参数，用于 toJsonLine 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private String toJsonLine(CdpEventLogDO event) {
        try {
            return objectMapper.writeValueAsString(toRow(event));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize CDP event row", e);
        }
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param event event 参数，用于 toRow 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param raw raw 参数，用于 jsonOrNull 流程中的校验、计算或对象转换。
     * @return 返回 jsonOrNull 流程生成的业务结果。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param dateTime 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 format date time 生成的文本或业务键。
     */
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : DORIS_DATETIME_FORMATTER.format(dateTime);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 authorization header 生成的文本或业务键。
     */
    private String authorizationHeader() {
        String token = username + ":" + (password == null ? "" : password);
        return "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param body 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 stream load succeeded 的布尔判断结果。
     */
    private boolean streamLoadSucceeded(String body) {
        return body == null
                || body.isBlank()
                || body.contains("\"Status\":\"Success\"")
                || body.contains("\"Status\":\"Publish Timeout\"");
    }
}
