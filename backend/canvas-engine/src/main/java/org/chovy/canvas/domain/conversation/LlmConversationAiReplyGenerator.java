package org.chovy.canvas.domain.conversation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.engine.llm.AiLlmGateway;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LlmConversationAiReplyGenerator implements ConversationAiReplyGenerator {

    public static final long DEFAULT_TEMPLATE_ID = 8L;

    private final AiLlmGateway llmGateway;
    private final ObjectMapper objectMapper;

    public LlmConversationAiReplyGenerator(AiLlmGateway llmGateway, ObjectMapper objectMapper) {
        this.llmGateway = llmGateway;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

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
        } catch (Exception ex) {
            return fallback("PROVIDER_ERROR", context == null ? null : context.command());
        }
    }

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

    private String fallbackText() {
        return "我需要再确认一下上下文后回复您。";
    }

    private String text(JsonNode node, String field, String fallback) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            return fallback;
        }
        return value.asText().trim();
    }

    private double number(JsonNode node, String field, double fallback) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || !value.isNumber()) {
            return fallback;
        }
        return value.asDouble();
    }

    private List<String> strings(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || !value.isArray()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        value.forEach(item -> {
            if (item != null && item.isTextual() && !item.asText().isBlank()) {
                result.add(item.asText().trim());
            }
        });
        return result;
    }

    private String value(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
