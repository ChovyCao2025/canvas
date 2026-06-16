package org.chovy.canvas.marketing.domain;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.chovy.canvas.marketing.api.MessageTemplateFacade;

/**
 * 维护MessageTemplate相关的内存业务目录。
 */
public class MessageTemplateCatalog {

    private static final Set<String> SUPPORTED_CHANNELS = Set.of("SMS", "EMAIL", "PUSH", "WECHAT", "IN_APP",
            "WEBHOOK");
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([A-Za-z][A-Za-z0-9_]*)\\s*}}");

    private final Map<Long, Map<String, MessageTemplateFacade.TemplateView>> tenants = new LinkedHashMap<>();

    /**
     * 创建业务对象。
     */
    public MessageTemplateFacade.TemplateView create(Long tenantId, String actor,
                                                     MessageTemplateFacade.TemplateDraft draft) {
        Objects.requireNonNull(draft, "template draft is required");
        String templateCode = normalizeTemplateCode(draft.templateCode());
        String channel = normalizeChannel(draft.channel());
        requireText(draft.displayName(), "display name is required");
        requireText(draft.body(), "template body is required");

        MessageTemplateFacade.TemplateView template = new MessageTemplateFacade.TemplateView(
                tenantId,
                templateCode,
                draft.displayName().trim(),
                channel,
                draft.body(),
                extractVariables(draft.body()),
                "DRAFT",
                actor);
        tenantTemplates(tenantId).put(templateCode, template);
        return template;
    }

    /**
     * 执行search业务操作。
     */
    public List<MessageTemplateFacade.TemplateView> search(Long tenantId, String keyword, String channel) {
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedChannel = normalizeOptionalChannel(channel);
        return tenantTemplates(tenantId).values().stream()
                .filter(template -> normalizedKeyword == null
                        || template.templateCode().contains(normalizedKeyword.toLowerCase(Locale.ROOT))
                        || template.displayName().toLowerCase(Locale.ROOT)
                        .contains(normalizedKeyword.toLowerCase(Locale.ROOT)))
                .filter(template -> normalizedChannel == null || normalizedChannel.equals(template.channel()))
                .toList();
    }

    /**
     * 执行preview业务操作。
     */
    public MessageTemplateFacade.PreviewView preview(Long tenantId, String templateCode, Map<String, Object> context) {
        String normalizedCode = normalizeTemplateCode(templateCode);
        MessageTemplateFacade.TemplateView template = tenantTemplates(tenantId).get(normalizedCode);
        if (template == null) {
            throw new IllegalArgumentException("template not found: " + templateCode);
        }

        LinkedHashSet<String> missingVariables = new LinkedHashSet<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(template.body());
        StringBuffer renderedBody = new StringBuffer();
        while (matcher.find()) {
            String variable = matcher.group(1);
            Object value = context.get(variable);
            if (value == null) {
                missingVariables.add(variable);
                matcher.appendReplacement(renderedBody, Matcher.quoteReplacement(matcher.group(0)));
            } else {
                matcher.appendReplacement(renderedBody, Matcher.quoteReplacement(String.valueOf(value)));
            }
        }
        matcher.appendTail(renderedBody);
        return new MessageTemplateFacade.PreviewView(renderedBody.toString(), List.copyOf(missingVariables));
    }

    /**
     * 执行tenantTemplates业务操作。
     */
    private Map<String, MessageTemplateFacade.TemplateView> tenantTemplates(Long tenantId) {
        return tenants.computeIfAbsent(tenantId, ignored -> new LinkedHashMap<>());
    }

    /**
     * 执行extractVariables业务操作。
     */
    private static List<String> extractVariables(String body) {
        LinkedHashSet<String> variables = new LinkedHashSet<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(body);
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }
        return List.copyOf(variables);
    }

    /**
     * 规范化templateCode输入值。
     */
    private static String normalizeTemplateCode(String templateCode) {
        requireText(templateCode, "template code is required");
        String normalized = templateCode.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9][a-z0-9_-]{0,127}")) {
            throw new IllegalArgumentException("invalid template code: " + templateCode);
        }
        return normalized;
    }

    /**
     * 规范化channel输入值。
     */
    private static String normalizeChannel(String channel) {
        requireText(channel, "template channel is required");
        String normalized = channel.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_CHANNELS.contains(normalized)) {
            throw new IllegalArgumentException("unsupported template channel " + normalized);
        }
        return normalized;
    }

    /**
     * 规范化optionalChannel输入值。
     */
    private static String normalizeOptionalChannel(String channel) {
        return channel == null || channel.isBlank() ? null : normalizeChannel(channel);
    }

    /**
     * 规范化keyword输入值。
     */
    private static String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    /**
     * 校验并返回text必填值。
     */
    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
