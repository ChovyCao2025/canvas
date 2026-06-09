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

/**
 * HttpCdpWarehouseExternalRealtimeJobProbeClient 编排 domain.warehouse 场景的领域业务规则。
 */
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

    /**
     * 创建 HttpCdpWarehouseExternalRealtimeJobProbeClient 实例并注入 domain.warehouse 场景依赖。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param timeoutMs 时间参数，用于计算窗口、过期或审计时间。
     */
    @Autowired
    public HttpCdpWarehouseExternalRealtimeJobProbeClient(
            ObjectMapper objectMapper,
            @Value("${canvas.warehouse.external-realtime-job-probe.http-timeout-ms:3000}") long timeoutMs) {
        this(objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(timeoutMs, 1)))
                .build(), Duration.ofMillis(Math.max(timeoutMs, 1)));
    }

    /**
     * 执行 HttpCdpWarehouseExternalRealtimeJobProbeClient 流程，围绕 http cdp warehouse external realtime job probe client 完成校验、计算或结果组装。
     *
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param httpClient 依赖组件，用于完成数据访问或外部能力调用。
     * @param timeout 时间参数，用于计算窗口、过期或审计时间。
     */
    HttpCdpWarehouseExternalRealtimeJobProbeClient(ObjectMapper objectMapper,
                                                  HttpClient httpClient,
                                                  Duration timeout) {
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.httpClient = httpClient;
        this.timeout = timeout == null ? Duration.ofSeconds(3) : timeout;
    }

    /**
     * 对外部实时作业运行时执行一次 HTTP 探测。
     *
     * <p>方法向目标 endpoint 发起 GET 请求，按 Flink REST、Kafka Connect、Doris Routine Load 或通用字段解析运行状态，
     * 并把原始响应摘要写入 payload。非 2xx 响应会返回 FAILED 探测结果；网络或解析异常会抛出运行时异常供扫描服务记录失败。</p>
     *
     * @param target 探测目标，包含 endpoint、引擎类型和作业标识
     * @return 外部运行时状态、诊断消息、响应载荷和引擎作业 ID
     */
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException e) {
            throw e;
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            throw new IllegalStateException("external probe failed: " + e.getMessage(), e);
        }
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param engineType 类型标识，用于选择对应处理分支。
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回流程执行后的业务结果。
     */
    private String runtimeStatus(String engineType, JsonNode json) {
        // 准备本次处理所需的上下文和中间变量。
        String engine = upper(engineType);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if ("FLINK_REST".equals(engine)) {
            return flinkStatus(text(json, "state", "jobStatus", "status"));
        }
        if ("KAFKA_CONNECT".equals(engine)) {
            return kafkaConnectStatus(json);
        }
        if ("DORIS_ROUTINE_LOAD".equals(engine)) {
            return dorisStatus(text(json, "state", "job_state", "JobState", "status", "Status"));
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return genericStatus(text(json, "runtimeStatus", "runtime_status", "state", "status", "health"));
    }

    /**
     * 执行 flinkStatus 流程，围绕 flink status 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 flink status 生成的文本或业务键。
     */
    private String flinkStatus(String value) {
        // 准备本次处理所需的上下文和中间变量。
        String state = upper(value);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if ("FAILED".equals(state) || "FAILING".equals(state)) {
            return FAILED;
        }
        if ("CANCELED".equals(state) || "CANCELLED".equals(state) || "FINISHED".equals(state)) {
            return STOPPED;
        }
        if ("SUSPENDED".equals(state)) {
            return PAUSED;
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return RUNNING;
    }

    /**
     * 执行 kafkaConnectStatus 流程，围绕 kafka connect status 完成校验、计算或结果组装。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回 kafka connect status 生成的文本或业务键。
     */
    private String kafkaConnectStatus(JsonNode json) {
        String connectorState = upper(text(path(json, "connector"), "state"));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
            // 遍历候选数据并按业务规则筛选、转换或聚合。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return RUNNING;
    }

    /**
     * 执行 dorisStatus 流程，围绕 doris status 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 doris status 生成的文本或业务键。
     */
    private String dorisStatus(String value) {
        // 准备本次处理所需的上下文和中间变量。
        String state = upper(value);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return RUNNING;
    }

    /**
     * 执行 genericStatus 流程，围绕 generic status 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 generic status 生成的文本或业务键。
     */
    private String genericStatus(String value) {
        // 准备本次处理所需的上下文和中间变量。
        String state = upper(value);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if ("FAILED".equals(state) || "FAIL".equals(state) || "DOWN".equals(state) || "ERROR".equals(state)) {
            return FAILED;
        }
        if ("PAUSED".equals(state)) {
            return PAUSED;
        }
        if ("STOPPED".equals(state) || "FINISHED".equals(state)) {
            return STOPPED;
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return RUNNING;
    }

    /**
     * 执行 message 流程，围绕 message 完成校验、计算或结果组装。
     *
     * @param engineType 类型标识，用于选择对应处理分支。
     * @param runtimeStatus 业务状态，用于筛选或推进状态流转。
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回 message 生成的文本或业务键。
     */
    private String message(String engineType, String runtimeStatus, JsonNode json) {
        String detail = text(json, "message", "error", "errorMessage");
        if (detail != null && !detail.isBlank()) {
            return detail;
        }
        return upper(engineType) + " probe mapped to " + runtimeStatus;
    }

    /**
     * 解析并校验输入数据。
     *
     * @param body 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private JsonNode parse(String body) {
        if (body == null || body.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(body);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            throw new IllegalStateException("external probe response JSON is invalid", e);
        }
    }

    /**
     * 执行 payload 流程，围绕 payload 完成校验、计算或结果组装。
     *
     * @param target target 参数，用于 payload 流程中的校验、计算或对象转换。
     * @param httpStatus 业务状态，用于筛选或推进状态流转。
     * @param json JSON 字符串，承载结构化配置或明细。
     * @param body 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 payload 生成的文本或业务键。
     */
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
            // 根据前序判断结果进入后续条件分支。
            } else if (body != null && !body.isBlank()) {
                node.put("responseBody", body);
            }
            return objectMapper.writeValueAsString(node);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            return "{\"source\":\"EXTERNAL_REALTIME_JOB_PROBE\"}";
        }
    }

    /**
     * 执行 engineJobId 流程，围绕 engine job id 完成校验、计算或结果组装。
     *
     * @param target target 参数，用于 engineJobId 流程中的校验、计算或对象转换。
     * @return 返回 engine job id 生成的文本或业务键。
     */
    private String engineJobId(ProbeTarget target) {
        if (hasText(target.externalJobId())) {
            return target.externalJobId().trim();
        }
        if (hasText(target.connectorName())) {
            return target.connectorName().trim();
        }
        return null;
    }

    /**
     * 执行 path 流程，围绕 path 完成校验、计算或结果组装。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 path 流程生成的业务结果。
     */
    private JsonNode path(JsonNode json, String field) {
        return json == null ? null : json.get(field);
    }

    /**
     * 执行 text 流程，围绕 text 完成校验、计算或结果组装。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @param fields fields 参数，用于 text 流程中的校验、计算或对象转换。
     * @return 返回 text 生成的文本或业务键。
     */
    private String text(JsonNode json, String... fields) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (json == null || fields == null) {
            return null;
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String field : fields) {
            JsonNode value = json.get(field);
            if (value != null && !value.isNull()) {
                return value.asText();
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return null;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 required 生成的文本或业务键。
     */
    private String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    /**
     * 执行 upper 流程，围绕 upper 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 upper 生成的文本或业务键。
     */
    private String upper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
