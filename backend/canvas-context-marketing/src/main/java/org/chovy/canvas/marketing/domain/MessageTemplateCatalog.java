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

public class MessageTemplateCatalog {

    private static final Set<String> SUPPORTED_CHANNELS = Set.of("SMS", "EMAIL", "PUSH", "WECHAT", "IN_APP",
            "WEBHOOK");
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([A-Za-z][A-Za-z0-9_]*)\\s*}}");

    private final Map<Long, Map<String, MessageTemplateFacade.TemplateView>> tenants = new LinkedHashMap<>();

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

    private Map<String, MessageTemplateFacade.TemplateView> tenantTemplates(Long tenantId) {
        return tenants.computeIfAbsent(tenantId, ignored -> new LinkedHashMap<>());
    }

    private static List<String> extractVariables(String body) {
        LinkedHashSet<String> variables = new LinkedHashSet<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(body);
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }
        return List.copyOf(variables);
    }

    private static String normalizeTemplateCode(String templateCode) {
        requireText(templateCode, "template code is required");
        String normalized = templateCode.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9][a-z0-9_-]{0,127}")) {
            throw new IllegalArgumentException("invalid template code: " + templateCode);
        }
        return normalized;
    }

    private static String normalizeChannel(String channel) {
        requireText(channel, "template channel is required");
        String normalized = channel.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_CHANNELS.contains(normalized)) {
            throw new IllegalArgumentException("unsupported template channel " + normalized);
        }
        return normalized;
    }

    private static String normalizeOptionalChannel(String channel) {
        return channel == null || channel.isBlank() ? null : normalizeChannel(channel);
    }

    private static String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
