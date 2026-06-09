package org.chovy.canvas.domain.monitoring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.engine.llm.AiLlmGateway;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LlmMarketingMonitorInferenceGenerator 编排 domain.monitoring 场景的领域业务规则。
 */
@Service
public class LlmMarketingMonitorInferenceGenerator implements MarketingMonitorInferenceGenerator {

    public static final long DEFAULT_TEMPLATE_ID = 9L;

    private final AiLlmGateway llmGateway;
    private final ObjectMapper objectMapper;

    /**
     * 创建 LlmMarketingMonitorInferenceGenerator 实例并注入 domain.monitoring 场景依赖。
     * @param llmGateway llm gateway 参数，用于 LlmMarketingMonitorInferenceGenerator 流程中的校验、计算或对象转换。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public LlmMarketingMonitorInferenceGenerator(AiLlmGateway llmGateway, ObjectMapper objectMapper) {
        this.llmGateway = llmGateway;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    /**
     * 基于监控内容上下文生成结构化推断结果。
     *
     * <p>方法只负责调用统一 LLM 网关并把模板输出映射为情感、实体、主题、风险标记和证据字段；
     * 不拉取外部内容、不写入监控结果，也不保证模型判断一定正确。网关返回空结果时返回 {@code null}。</p>
     *
     * @param context 推断所需的租户、模型命令和提示上下文
     * @return LLM 输出映射后的监控推断结果；无模型结果时返回 {@code null}
     */
    @Override
    public MarketingMonitorInferenceGenerationResult generate(MarketingMonitorInferenceGenerationContext context) {
        MarketingMonitorInferenceCommand command = context == null ? null : context.command();
        AiLlmGateway.AiLlmResult result = llmGateway.evaluate(context.tenantId(), new AiLlmGateway.AiLlmRequest(
                command == null ? null : command.providerId(),
                command == null || command.templateId() == null ? DEFAULT_TEMPLATE_ID : command.templateId(),
                command == null ? null : command.modelKey(),
                null,
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                objectMapper.valueToTree(context.promptContext()),
                null,
                command == null ? null : command.params(),
                command == null ? null : command.timeoutMs(),
                null,
                null,
                "marketing-monitor-inference")).block();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (result == null) {
            return null;
        }
        JsonNode output = result.output();
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new MarketingMonitorInferenceGenerationResult(
                result.providerId(),
                result.templateId(),
                result.modelKey(),
                value(command == null ? null : command.modelVersion(), "llm_v1"),
                value(result.status(), "UNKNOWN"),
                result.fallbackUsed(),
                text(output, "sentimentLabel", "NEUTRAL"),
                decimal(output, "sentimentScore", BigDecimal.ZERO),
                decimal(output, "confidence", BigDecimal.valueOf(result.fallbackUsed() ? 0.35 : 0.70)),
                entities(output),
                strings(output, "topics"),
                strings(output, "riskFlags"),
                evidence(output),
                result.latencyMs());
    }

    /**
     * 执行 text 流程，围绕 text 完成校验、计算或结果组装。
     *
     * @param node node 参数，用于 text 流程中的校验、计算或对象转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @param fallback fallback 参数，用于 text 流程中的校验、计算或对象转换。
     * @return 返回 text 生成的文本或业务键。
     */
    private String text(JsonNode node, String field, String fallback) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            return fallback;
        }
        return value.asText().trim();
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param node node 参数，用于 decimal 流程中的校验、计算或对象转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @param fallback fallback 参数，用于 decimal 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private BigDecimal decimal(JsonNode node, String field, BigDecimal fallback) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || !value.isNumber()) {
            return fallback;
        }
        return value.decimalValue();
    }

    /**
     * 执行 entities 流程，围绕 entities 完成校验、计算或结果组装。
     *
     * @param node node 参数，用于 entities 流程中的校验、计算或对象转换。
     * @return 返回 entities 流程生成的业务结果。
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> entities(JsonNode node) {
        JsonNode value = node == null ? null : node.get("entities");
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (value == null || !value.isArray()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        value.forEach(item -> {
            if (item != null && item.isObject()) {
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                result.add(objectMapper.convertValue(item, Map.class));
            }
        });
        return result;
    }

    /**
     * 执行 strings 流程，围绕 strings 完成校验、计算或结果组装。
     *
     * @param node node 参数，用于 strings 流程中的校验、计算或对象转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 strings 汇总后的集合、分页或映射视图。
     */
    private List<String> strings(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (value == null || !value.isArray()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        value.forEach(item -> {
            if (item != null && item.isTextual() && !item.asText().isBlank()) {
                result.add(item.asText().trim());
            }
        });
        // 汇总前面计算出的状态和明细，返回给调用方。
        return result;
    }

    /**
     * 执行 evidence 流程，围绕 evidence 完成校验、计算或结果组装。
     *
     * @param node node 参数，用于 evidence 流程中的校验、计算或对象转换。
     * @return 返回 evidence 流程生成的业务结果。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> evidence(JsonNode node) {
        JsonNode value = node == null ? null : node.get("evidence");
        if (value == null || !value.isObject()) {
            return Map.of();
        }
        return objectMapper.convertValue(value, LinkedHashMap.class);
    }

    /**
     * 执行 value 流程，围绕 value 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 value 流程中的校验、计算或对象转换。
     * @return 返回 value 生成的文本或业务键。
     */
    private String value(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
