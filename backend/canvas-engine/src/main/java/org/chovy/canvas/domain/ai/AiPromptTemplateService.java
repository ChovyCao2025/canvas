package org.chovy.canvas.domain.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiPromptTemplateService {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{\\s*([^}]+?)\\s*}|\\{\\{\\s*([^}]+?)\\s*}}");

    private final ObjectMapper objectMapper;
    private final AtomicLong templateIdSequence = new AtomicLong(100L);
    private final ConcurrentMap<Long, TemplateRegistration> templates = new ConcurrentHashMap<>();

    public AiPromptTemplateService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        seedBuiltInTemplates();
    }

    public List<TemplateSummary> listTemplates(Long tenantId) {
        return templates.values().stream()
                .filter(template -> template.enabled())
                .filter(template -> AiProviderModelRegistryService.visibleToTenant(template.tenantId(), tenantId))
                .sorted(Comparator.comparing(TemplateRegistration::tenantId, Comparator.nullsFirst(Long::compareTo))
                        .thenComparing(TemplateRegistration::id))
                .map(this::toSummary)
                .toList();
    }

    public TemplateDetail createTemplate(Long tenantId, TemplateCreateRequest req) {
        Long scopedTenantId = AiProviderModelRegistryService.normalizeTenantId(tenantId);
        JsonNode outputSchema = requireObject(req.outputSchema(), "outputSchema");
        JsonNode defaultValues = requireObject(req.defaultValues(), "defaultValues");
        long id = templateIdSequence.incrementAndGet();
        TemplateRegistration template = new TemplateRegistration(
                id,
                scopedTenantId,
                requireText(req.name(), "name"),
                requireText(req.category(), "category"),
                requireText(req.promptTemplate(), "promptTemplate"),
                outputSchema.deepCopy(),
                defaultValues.deepCopy(),
                req.enabled() == null || req.enabled());
        templates.put(id, template);
        return toDetail(template);
    }

    public TemplateDetail getTemplate(Long tenantId, Long templateId) {
        return toDetail(requireVisibleTemplate(tenantId, templateId));
    }

    public void disableTemplate(Long tenantId, Long templateId) {
        TemplateRegistration existing = requireVisibleTemplate(tenantId, templateId);
        Long scopedTenantId = AiProviderModelRegistryService.normalizeTenantId(tenantId);
        if (!Objects.equals(existing.tenantId(), scopedTenantId)) {
            throw new IllegalArgumentException("Built-in AI prompt templates cannot be disabled");
        }
        templates.put(templateId, new TemplateRegistration(
                existing.id(),
                existing.tenantId(),
                existing.name(),
                existing.category(),
                existing.promptTemplate(),
                existing.outputSchema(),
                existing.defaultValues(),
                false));
    }

    public TemplateDetail requireEnabledTemplate(Long tenantId, Long templateId) {
        TemplateRegistration template = requireVisibleTemplate(tenantId, templateId);
        if (!template.enabled()) {
            throw new IllegalArgumentException("AI prompt template is disabled: " + templateId);
        }
        return toDetail(template);
    }

    public RenderResult render(Long tenantId, RenderRequest req) {
        TemplateDetail template = requireEnabledTemplate(tenantId, req.templateId());
        String source = blankToDefault(req.promptOverride(), template.promptTemplate());
        String rendered = renderTemplate(source, req.variables());
        return new RenderResult(template.id(), rendered);
    }

    public String renderTemplate(String template, JsonNode variables) {
        JsonNode scopedVariables = variables == null || variables.isNull() ? objectMapper.createObjectNode() : variables;
        Matcher matcher = VARIABLE_PATTERN.matcher(template == null ? "" : template);
        StringBuilder rendered = new StringBuilder();
        while (matcher.find()) {
            String path = matcher.group(1) == null ? matcher.group(2) : matcher.group(1);
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(resolveVariable(scopedVariables, path.trim())));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    public boolean matchesSchema(JsonNode outputSchema, JsonNode value) {
        if (outputSchema == null || outputSchema.isNull()) {
            return true;
        }
        String type = outputSchema.path("type").asText("");
        if ("object".equals(type) && (value == null || !value.isObject())) {
            return false;
        }
        JsonNode required = outputSchema.path("required");
        if (!required.isArray()) {
            return true;
        }
        for (JsonNode field : required) {
            String fieldName = field.asText();
            if (fieldName.isBlank() || value == null || !value.hasNonNull(fieldName)) {
                return false;
            }
        }
        return true;
    }

    private TemplateRegistration requireVisibleTemplate(Long tenantId, Long templateId) {
        TemplateRegistration template = templates.get(templateId);
        if (template == null || !AiProviderModelRegistryService.visibleToTenant(template.tenantId(), tenantId)) {
            throw new IllegalArgumentException("AI prompt template not found: " + templateId);
        }
        return template;
    }

    private String resolveVariable(JsonNode variables, String path) {
        JsonNode node = variables;
        for (String segment : path.split("\\.")) {
            if (segment.isBlank()) {
                return "";
            }
            node = node == null ? null : node.path(segment);
            if (node == null || node.isMissingNode() || node.isNull()) {
                return "";
            }
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isValueNode()) {
            return node.asText();
        }
        return node.toString();
    }

    private TemplateSummary toSummary(TemplateRegistration template) {
        return new TemplateSummary(
                template.id(),
                template.tenantId(),
                template.name(),
                template.category(),
                template.enabled());
    }

    private TemplateDetail toDetail(TemplateRegistration template) {
        return new TemplateDetail(
                template.id(),
                template.tenantId(),
                template.name(),
                template.category(),
                template.promptTemplate(),
                template.outputSchema().deepCopy(),
                template.defaultValues().deepCopy(),
                template.enabled());
    }

    private JsonNode requireObject(JsonNode value, String fieldName) {
        if (value == null || !value.isObject()) {
            throw new IllegalArgumentException(fieldName + " must be a JSON object");
        }
        return value;
    }

    private static String requireText(String value, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return trimmed;
    }

    private static String blankToDefault(String value, String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed == null ? defaultValue : trimmed;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void seedBuiltInTemplates() {
        templates.put(1L, builtIn(
                1L,
                "Text Generation",
                "text_generate",
                "Create a ${channelType} message for ${userProfile.name} about ${productInfo.name}.",
                object("type", "object",
                        "properties", object("text", object("type", "string"), "tone", object("type", "string")),
                        "required", objectMapper.createArrayNode().add("text").add("tone")),
                object("text", "Your exclusive benefit is ready.", "tone", "warm")));
        templates.put(2L, builtIn(
                2L,
                "Smart Scoring",
                "scoring",
                "Score user ${userId} from behavior ${behaviorData}.",
                object("type", "object",
                        "properties", object("score", object("type", "number"), "band", object("type", "string")),
                        "required", objectMapper.createArrayNode().add("score").add("band")),
                object("score", 50, "band", "medium")));
    }

    private TemplateRegistration builtIn(Long id,
                                         String name,
                                         String category,
                                         String promptTemplate,
                                         JsonNode outputSchema,
                                         JsonNode defaultValues) {
        return new TemplateRegistration(id, null, name, category, promptTemplate, outputSchema, defaultValues, true);
    }

    private ObjectNode object(Object... values) {
        ObjectNode node = objectMapper.createObjectNode();
        for (int i = 0; i < values.length; i += 2) {
            String field = String.valueOf(values[i]);
            Object value = values[i + 1];
            node.set(field, objectMapper.valueToTree(value));
        }
        return node;
    }

    private record TemplateRegistration(
            Long id,
            Long tenantId,
            String name,
            String category,
            String promptTemplate,
            JsonNode outputSchema,
            JsonNode defaultValues,
            boolean enabled) {
    }

    public record TemplateCreateRequest(
            String name,
            String category,
            String promptTemplate,
            JsonNode outputSchema,
            JsonNode defaultValues,
            Boolean enabled) {
    }

    public record RenderRequest(Long templateId, String promptOverride, JsonNode variables) {
    }

    public record RenderResult(Long templateId, String renderedPrompt) {
    }

    public record TemplateSummary(Long id, Long tenantId, String name, String category, boolean enabled) {
    }

    public record TemplateDetail(
            Long id,
            Long tenantId,
            String name,
            String category,
            String promptTemplate,
            JsonNode outputSchema,
            JsonNode defaultValues,
            boolean enabled) {
    }
}
