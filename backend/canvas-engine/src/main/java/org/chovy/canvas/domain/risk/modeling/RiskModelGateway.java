package org.chovy.canvas.domain.risk.modeling;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 风控模型网关，调用最新活跃模型并执行超时兜底和主体数据脱敏策略。
 */
public class RiskModelGateway {

    private final RiskModelRegistryService registry;
    private final RiskModelClient client;
    private final ObjectMapper objectMapper;

    /**
     * 使用默认 ObjectMapper 创建模型网关。
     */
    public RiskModelGateway(RiskModelRegistryService registry, RiskModelClient client) {
        this(registry, client, new ObjectMapper());
    }

    /**
     * 创建模型网关。
     */
    public RiskModelGateway(RiskModelRegistryService registry, RiskModelClient client, ObjectMapper objectMapper) {
        this.registry = registry;
        this.client = client;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    /**
     * 调用指定模型的最新活跃版本并返回评分结果。
     */
    public RiskModelResult score(RiskModelRequest request) {
        RiskModelDefinition model = registry.latestActive(request.modelKey())
                .orElseThrow(() -> new IllegalArgumentException("No active risk model: " + request.modelKey()));
        try {
            String response = client.score(new RiskModelClientCall(
                    model.modelKey(),
                    model.version(),
                    model.endpoint(),
                    model.timeout(),
                    payload(request, model)));
            return parseResponse(response, model.version());
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RiskModelTimeoutException ex) {
            // 模型超时降级为注册的兜底分，确保决策仍能满足 SLA。
            return new RiskModelResult(model.fallbackScore(), List.of("MODEL_TIMEOUT"), model.version(), true);
        }
    }

    /**
     * 构造模型调用载荷，并按模型授权决定是否发送原始主体数据。
     */
    private Map<String, Object> payload(RiskModelRequest request, RiskModelDefinition model) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenantId", request.tenantId());
        payload.put("modelKey", model.modelKey());
        payload.put("modelVersion", model.version());
        payload.put("features", request.features());
        // 只有通过 PII 审批的模型才会收到原始主体属性。
        payload.put("subject", model.rawPiiApproved() ? request.subject() : maskedSubject(request.subject()));
        return payload;
    }

    /**
     * 对主体属性逐字段脱敏。
     */
    private Map<String, Object> maskedSubject(Map<String, Object> subject) {
        Map<String, Object> masked = new LinkedHashMap<>();
        subject.forEach((key, value) -> masked.put(key, mask(value)));
        return masked;
    }

    /**
     * 对单个主体属性执行脱敏，非字符串保持原值。
     */
    private Object mask(Object value) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!(value instanceof String text)) {
            return value;
        }
        if (text.startsWith("+") && text.length() > 4) {
            return "***" + text.substring(text.length() - 4);
        }
        if (text.contains("@")) {
            return maskText(text);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return maskText(text);
    }

    /**
     * 保留首尾字符并隐藏中间内容。
     */
    private String maskText(String text) {
        if (text.length() <= 2) {
            return "***";
        }
        return text.charAt(0) + "***" + text.charAt(text.length() - 1);
    }

    /**
     * 解析模型响应 JSON，缺少模型版本时回退到注册版本。
     */
    private RiskModelResult parseResponse(String response, int fallbackVersion) {
        try {
            JsonNode root = objectMapper.readTree(response);
            int score = (int) Math.round(root.path("score").asDouble());
            // 缺失 modelVersion 时回退到活跃注册版本，保证追踪链路连续。
            int modelVersion = root.path("modelVersion").asInt(fallbackVersion);
            List<String> explanations = new ArrayList<>();
            JsonNode explanationNode = root.path("explanations");
            if (explanationNode.isArray()) {
                explanationNode.forEach(item -> explanations.add(item.asText()));
            }
            return new RiskModelResult(score, List.copyOf(explanations), modelVersion, false);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid risk model response", ex);
        }
    }
}
