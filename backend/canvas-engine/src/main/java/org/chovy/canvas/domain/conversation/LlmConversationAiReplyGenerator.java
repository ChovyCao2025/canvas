package org.chovy.canvas.domain.conversation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.engine.llm.AiLlmGateway;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * LlmConversationAiReplyGenerator 编排 domain.conversation 场景的领域业务规则。
 */
@Service
public class LlmConversationAiReplyGenerator implements ConversationAiReplyGenerator {

    public static final long DEFAULT_TEMPLATE_ID = 8L;

    private final AiLlmGateway llmGateway;
    private final ObjectMapper objectMapper;

    /**
     * 创建 LlmConversationAiReplyGenerator 实例并注入 domain.conversation 场景依赖。
     * @param llmGateway llm gateway 参数，用于 LlmConversationAiReplyGenerator 流程中的校验、计算或对象转换。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public LlmConversationAiReplyGenerator(AiLlmGateway llmGateway, ObjectMapper objectMapper) {
        this.llmGateway = llmGateway;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    /**
     * 基于会话回复上下文调用统一 LLM 网关生成建议回复。
     *
     * <p>方法只负责把会话提示上下文、模型参数和默认模板组装为一次 LLM 评估请求，并把返回 JSON 中的回复文本、
     * 语气、意图、置信度和佐证片段转换为业务结果；不会直接发送消息或写入会话状态。网关无结果或调用异常时返回
     * 带兜底文案和 fallback 标记的结果，供上层决定是否展示或继续人工处理。</p>
     *
     * @param context 本次 AI 回复生成的租户、命令参数和提示上下文
     * @return 结构化回复建议；当提供方不可用时返回兜底建议而不是抛出异常
     */
    @Override
    public ConversationAiReplyGenerationResult generate(ConversationAiReplyGenerationContext context) {
        try {
            ConversationAiReplyGenerateCommand command = context.command();
            AiLlmGateway.AiLlmResult result = llmGateway.evaluate(context.tenantId(), new AiLlmGateway.AiLlmRequest(
                    command == null ? null : command.providerId(),
                    command == null || command.templateId() == null ? DEFAULT_TEMPLATE_ID : command.templateId(),
                    command == null ? null : command.modelKey(),
                    null,
                    objectMapper.valueToTree(context.promptContext()),
                    null,
                    command == null ? null : command.params(),
                    command == null ? null : command.timeoutMs(),
                    null,
                    null,
                    "scrm-ai-reply-assistant")).block();
            if (result == null) {
                return fallback("NO_RESULT", command);
            }
            JsonNode output = result.output();
            return new ConversationAiReplyGenerationResult(
                    text(output, "suggestedReplyText", text(output, "replyText", fallbackText())),
                    text(output, "tone", command == null ? "neutral" : value(command.tone(), "neutral")),
                    text(output, "intent", command == null ? "GENERAL" : value(command.intent(), "GENERAL")),
                    number(output, "confidence", result.fallbackUsed() ? 0.35 : 0.7),
                    strings(output, "riskFlags"),
                    strings(output, "groundingSnippets"),
                    result.providerId(),
                    result.templateId(),
                    result.modelKey(),
                    result.status(),
                    result.fallbackUsed());
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception ex) {
            return fallback("PROVIDER_ERROR", context == null ? null : context.command());
        }
    }

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回 fallback 流程生成的业务结果。
     */
    private ConversationAiReplyGenerationResult fallback(String status, ConversationAiReplyGenerateCommand command) {
        return new ConversationAiReplyGenerationResult(
                fallbackText(),
                command == null ? "neutral" : value(command.tone(), "neutral"),
                command == null ? "NEEDS_CONTEXT" : value(command.intent(), "NEEDS_CONTEXT"),
                0.35,
                List.of("GENERATOR_FALLBACK"),
                List.of(),
                command == null ? null : command.providerId(),
                command == null || command.templateId() == null ? DEFAULT_TEMPLATE_ID : command.templateId(),
                command == null ? "fallback" : value(command.modelKey(), "fallback"),
                status,
                true);
    }

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @return 返回 fallback text 生成的文本或业务键。
     */
    private String fallbackText() {
        return "我需要再确认一下上下文后回复您。";
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
     * 执行 number 流程，围绕 number 完成校验、计算或结果组装。
     *
     * @param node node 参数，用于 number 流程中的校验、计算或对象转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @param fallback fallback 参数，用于 number 流程中的校验、计算或对象转换。
     * @return 返回 number 计算得到的数量、金额或指标值。
     */
    private double number(JsonNode node, String field, double fallback) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || !value.isNumber()) {
            return fallback;
        }
        return value.asDouble();
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
