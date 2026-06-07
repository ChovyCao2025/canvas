package org.chovy.canvas.domain.template;

import org.chovy.canvas.common.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MessageTemplateService {

    private static final Set<String> SUPPORTED_CHANNELS = Set.of("SMS", "EMAIL", "PUSH", "WECHAT", "IN_APP", "WEBHOOK");
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([A-Za-z][A-Za-z0-9_]*)\\s*}}");

    private final TemplateRepository repository;

    public MessageTemplateService(TemplateRepository repository) {
        this.repository = repository;
    }

    public Template create(TenantContext tenant, TemplateDraft draft) {
        Long tenantId = requireTenantId(tenant);
        String templateCode = normalizeTemplateCode(draft.templateCode());
        String channel = normalizeChannel(draft.channel());
        requireText(draft.displayName(), "display name is required");
        requireText(draft.body(), "template body is required");

        Template template = new Template(
                tenantId,
                templateCode,
                draft.displayName().trim(),
                channel,
                draft.body(),
                extractVariables(draft.body()),
                "DRAFT",
                operator(tenant));
        repository.insert(template);
        return template;
    }

    public List<Template> search(TenantContext tenant, String keyword, String channel) {
        Long tenantId = requireTenantId(tenant);
        return repository.search(tenantId, normalizeKeyword(keyword), normalizeOptionalChannel(channel));
    }

    public PreviewResult preview(TenantContext tenant, String templateCode, Map<String, Object> context) {
        Long tenantId = requireTenantId(tenant);
        Template template = repository.get(tenantId, normalizeTemplateCode(templateCode));
        if (template == null) {
            throw new IllegalArgumentException("template not found: " + templateCode);
        }
        Map<String, Object> safeContext = context == null ? Map.of() : context;
        LinkedHashSet<String> missing = new LinkedHashSet<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(template.body());
        StringBuffer rendered = new StringBuffer();
        while (matcher.find()) {
            String variable = matcher.group(1);
            Object value = safeContext.get(variable);
            if (value == null) {
                missing.add(variable);
                matcher.appendReplacement(rendered, Matcher.quoteReplacement(matcher.group(0)));
            } else {
                matcher.appendReplacement(rendered, Matcher.quoteReplacement(String.valueOf(value)));
            }
        }
        matcher.appendTail(rendered);
        return new PreviewResult(rendered.toString(), List.copyOf(missing));
    }

    public static List<String> extractVariables(String body) {
        if (body == null || body.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> variables = new LinkedHashSet<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(body);
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }
        return List.copyOf(variables);
    }

    private static Long requireTenantId(TenantContext tenant) {
        if (tenant == null || tenant.tenantId() == null) {
            throw new SecurityException("AUTH_003: missing tenant context");
        }
        return tenant.tenantId();
    }

    private static String operator(TenantContext tenant) {
        if (tenant.username() == null || tenant.username().isBlank()) {
            return "unknown";
        }
        return tenant.username().trim();
    }

    private static String normalizeTemplateCode(String templateCode) {
        requireText(templateCode, "template code is required");
        String normalized = Objects.requireNonNull(templateCode).trim().toLowerCase(Locale.ROOT);
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
        if (channel == null || channel.isBlank()) {
            return null;
        }
        return normalizeChannel(channel);
    }

    private static String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    public record TemplateDraft(String templateCode, String displayName, String channel, String body) {
    }

    public record Template(
            Long tenantId,
            String templateCode,
            String displayName,
            String channel,
            String body,
            List<String> variables,
            String status,
            String createdBy) {
    }

    public record PreviewResult(String renderedBody, List<String> missingVariables) {
    }

    public interface TemplateRepository {
        void insert(Template template);

        List<Template> search(Long tenantId, String keyword, String channel);

        Template get(Long tenantId, String templateCode);
    }
}
