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

    /**
     * 创建 DorisStreamLoader 实例并注入 infrastructure.doris 场景依赖。
     * @param enabled enabled 参数，用于 DorisStreamLoader 流程中的校验、计算或对象转换。
     * @param streamLoadUrl stream load url 参数，用于 DorisStreamLoader 流程中的校验、计算或对象转换。
     * @param username 操作人标识，用于审计和权限判断。
     * @param password password 参数，用于 DorisStreamLoader 流程中的校验、计算或对象转换。
     * @param timeoutMs 时间参数，用于计算窗口、过期或审计时间。
     */
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

    /**
     * 执行 DorisStreamLoader 流程，围绕 doris stream loader 完成校验、计算或结果组装。
     *
     * @param enabled enabled 参数，用于 DorisStreamLoader 流程中的校验、计算或对象转换。
     * @param streamLoadUrl stream load url 参数，用于 DorisStreamLoader 流程中的校验、计算或对象转换。
     * @param username 操作人标识，用于审计和权限判断。
     * @param password password 参数，用于 DorisStreamLoader 流程中的校验、计算或对象转换。
     * @param timeout 时间参数，用于计算窗口、过期或审计时间。
     * @param httpClient 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
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

    /**
     * load 处理 infrastructure.doris 场景的业务逻辑。
     * @param traces traces 参数，用于 load 流程中的校验、计算或对象转换。
     * @return 返回 load 的布尔判断结果。
     */
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            log.warn("[DORIS_STREAM_LOAD] error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param traces traces 参数，用于 toJsonLines 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    String toJsonLines(List<CanvasExecutionTraceDO> traces) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return traces.stream()
                .map(this::toJsonLine)
                .collect(Collectors.joining("\n"));
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param trace trace 参数，用于 toJsonLine 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private String toJsonLine(CanvasExecutionTraceDO trace) {
        try {
            return objectMapper.writeValueAsString(toRow(trace));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize trace row", e);
        }
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param trace trace 参数，用于 toRow 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 执行 formatDateTime 流程，围绕 format date time 完成校验、计算或结果组装。
     *
     * @param dateTime 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 format date time 生成的文本或业务键。
     */
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : DORIS_DATETIME_FORMATTER.format(dateTime);
    }

    /**
     * 执行 authorizationHeader 流程，围绕 authorization header 完成校验、计算或结果组装。
     *
     * @return 返回 authorization header 生成的文本或业务键。
     */
    private String authorizationHeader() {
        String token = username + ":" + (password == null ? "" : password);
        return "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 执行 streamLoadSucceeded 流程，围绕 stream load succeeded 完成校验、计算或结果组装。
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

    /**
     * 执行 firstNonNull 流程，围绕 first non null 完成校验、计算或结果组装。
     *
     * @param values values 参数，用于 firstNonNull 流程中的校验、计算或对象转换。
     * @return 返回 firstNonNull 流程生成的业务结果。
     */
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
