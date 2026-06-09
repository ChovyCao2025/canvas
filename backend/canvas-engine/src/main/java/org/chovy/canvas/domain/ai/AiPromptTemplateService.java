package org.chovy.canvas.domain.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.chovy.canvas.dal.dataobject.AiPromptTemplateDO;
import org.chovy.canvas.dal.mapper.AiPromptTemplateMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
    private final AiPromptTemplateMapper templateMapper;
    private final AtomicLong templateIdSequence = new AtomicLong(100L);
    private final ConcurrentMap<Long, TemplateRegistration> templates = new ConcurrentHashMap<>();

    public AiPromptTemplateService(ObjectMapper objectMapper) {
        this(objectMapper, null, true);
    }

    @Autowired
    public AiPromptTemplateService(ObjectMapper objectMapper, AiPromptTemplateMapper templateMapper) {
        this(objectMapper, templateMapper, false);
    }

    private AiPromptTemplateService(ObjectMapper objectMapper,
                                    AiPromptTemplateMapper templateMapper,
                                    boolean seedMemory) {
        this.objectMapper = objectMapper;
        this.templateMapper = templateMapper;
        if (seedMemory) {
            seedBuiltInTemplates();
        }
    }

    public List<TemplateSummary> listTemplates(Long tenantId) {
        if (mapperBacked()) {
            return templateMapper.selectList(templateScope(tenantId))
                    .stream()
                    .filter(template -> enabled(template.getEnabled()))
                    .filter(template -> AiProviderModelRegistryService.visibleToTenant(template.getTenantId(), tenantId))
                    .sorted(Comparator.comparing(AiPromptTemplateDO::getTenantId, Comparator.nullsFirst(Long::compareTo))
                            .thenComparing(AiPromptTemplateDO::getId))
                    .map(this::toSummary)
                    .toList();
        }
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
        if (mapperBacked()) {
            AiPromptTemplateDO row = new AiPromptTemplateDO();
            row.setTenantId(scopedTenantId);
            row.setName(requireText(req.name(), "name"));
            row.setCategory(requireText(req.category(), "category"));
            row.setPromptTemplate(requireText(req.promptTemplate(), "promptTemplate"));
            row.setOutputSchema(writeJson(outputSchema));
            row.setDefaultValues(writeJson(defaultValues));
            row.setEnabled(req.enabled() == null || req.enabled() ? 1 : 0);
            templateMapper.insert(row);
            return toDetail(row);
        }
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
        if (mapperBacked()) {
            return toDetail(requireVisibleTemplateRow(tenantId, templateId));
        }
        return toDetail(requireVisibleTemplate(tenantId, templateId));
    }

    public TemplateDetail updateTemplate(Long tenantId, Long templateId, TemplateCreateRequest req) {
        Long scopedTenantId = AiProviderModelRegistryService.normalizeTenantId(tenantId);
        if (mapperBacked()) {
            AiPromptTemplateDO existing = requireVisibleTemplateRow(tenantId, templateId);
            if (!Objects.equals(existing.getTenantId(), scopedTenantId)) {
                throw new IllegalArgumentException("Built-in AI prompt templates cannot be updated");
            }
            existing.setName(blankToDefault(req.name(), existing.getName()));
            existing.setCategory(blankToDefault(req.category(), existing.getCategory()));
            existing.setPromptTemplate(blankToDefault(req.promptTemplate(), existing.getPromptTemplate()));
            if (req.outputSchema() != null) {
                existing.setOutputSchema(writeJson(requireObject(req.outputSchema(), "outputSchema")));
            }
            if (req.defaultValues() != null) {
                existing.setDefaultValues(writeJson(requireObject(req.defaultValues(), "defaultValues")));
            }
            existing.setEnabled(req.enabled() == null ? existing.getEnabled() : (req.enabled() ? 1 : 0));
            templateMapper.updateById(existing);
            return toDetail(existing);
        }
        TemplateRegistration existing = requireVisibleTemplate(tenantId, templateId);
        if (!Objects.equals(existing.tenantId(), scopedTenantId)) {
            throw new IllegalArgumentException("Built-in AI prompt templates cannot be updated");
        }
        TemplateRegistration updated = new TemplateRegistration(
                existing.id(),
                existing.tenantId(),
                blankToDefault(req.name(), existing.name()),
                blankToDefault(req.category(), existing.category()),
                blankToDefault(req.promptTemplate(), existing.promptTemplate()),
                req.outputSchema() == null ? existing.outputSchema() : requireObject(req.outputSchema(), "outputSchema"),
                req.defaultValues() == null ? existing.defaultValues() : requireObject(req.defaultValues(), "defaultValues"),
                req.enabled() == null ? existing.enabled() : req.enabled());
        templates.put(templateId, updated);
        return toDetail(updated);
    }

    public void disableTemplate(Long tenantId, Long templateId) {
        if (mapperBacked()) {
            AiPromptTemplateDO existing = requireVisibleTemplateRow(tenantId, templateId);
            Long scopedTenantId = AiProviderModelRegistryService.normalizeTenantId(tenantId);
            if (!Objects.equals(existing.getTenantId(), scopedTenantId)) {
                throw new IllegalArgumentException("Built-in AI prompt templates cannot be disabled");
            }
            existing.setEnabled(0);
            templateMapper.updateById(existing);
            return;
        }
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
        if (mapperBacked()) {
            AiPromptTemplateDO template = requireVisibleTemplateRow(tenantId, templateId);
            if (!enabled(template.getEnabled())) {
                throw new IllegalArgumentException("AI prompt template is disabled: " + templateId);
            }
            return toDetail(template);
        }
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

    private AiPromptTemplateDO requireVisibleTemplateRow(Long tenantId, Long templateId) {
        AiPromptTemplateDO template = templateMapper.selectById(templateId);
        if (template == null || !AiProviderModelRegistryService.visibleToTenant(template.getTenantId(), tenantId)) {
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

    private TemplateSummary toSummary(AiPromptTemplateDO template) {
        return new TemplateSummary(
                template.getId(),
                template.getTenantId(),
                template.getName(),
                template.getCategory(),
                enabled(template.getEnabled()));
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

    private TemplateDetail toDetail(AiPromptTemplateDO template) {
        return new TemplateDetail(
                template.getId(),
                template.getTenantId(),
                template.getName(),
                template.getCategory(),
                template.getPromptTemplate(),
                readObject(template.getOutputSchema(), "outputSchema"),
                readObject(template.getDefaultValues(), "defaultValues"),
                enabled(template.getEnabled()));
    }

    private JsonNode requireObject(JsonNode value, String fieldName) {
        if (value == null || !value.isObject()) {
            throw new IllegalArgumentException(fieldName + " must be a JSON object");
        }
        return value;
    }

    private JsonNode readObject(String json, String fieldName) {
        try {
            JsonNode value = objectMapper.readTree(json == null || json.isBlank() ? "{}" : json);
            return requireObject(value, fieldName).deepCopy();
        } catch (Exception ex) {
            throw new IllegalArgumentException(fieldName + " is not valid JSON", ex);
        }
    }

    private String writeJson(JsonNode value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("template JSON is not serializable", ex);
        }
    }

    private LambdaQueryWrapper<AiPromptTemplateDO> templateScope(Long tenantId) {
        Long scopedTenantId = AiProviderModelRegistryService.normalizeTenantId(tenantId);
        return new LambdaQueryWrapper<AiPromptTemplateDO>()
                .and(wrapper -> wrapper
                        .isNull(AiPromptTemplateDO::getTenantId)
                        .or()
                        .eq(AiPromptTemplateDO::getTenantId, 0L)
                        .or()
                        .eq(AiPromptTemplateDO::getTenantId, scopedTenantId));
    }

    private boolean mapperBacked() {
        return templateMapper != null;
    }

    private static boolean enabled(Integer enabled) {
        return enabled == null || enabled != 0;
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
        templates.put(3L, builtIn(
                3L,
                "BI Ask Data Planner",
                "bi_ask_data",
                """
                        Convert the user BI question into a JSON query plan.
                        Only use the supplied BI semantic catalog. Do not invent tables, fields, metrics, or SQL.
                        Question: ${question}
                        Requested dataset key: ${requestedDatasetKey}
                        Semantic catalog: ${datasets}
                        Return datasetKey, dimensions, metrics, filters, sorts, limit, and explanation.
                        """,
                object("type", "object",
                        "properties", object(
                                "datasetKey", object("type", "string"),
                                "dimensions", object("type", "array"),
                                "metrics", object("type", "array"),
                                "filters", object("type", "array"),
                                "sorts", object("type", "array"),
                                "limit", object("type", "number"),
                                "explanation", object("type", "string")),
                        "required", objectMapper.createArrayNode()
                                .add("datasetKey")
                                .add("dimensions")
                                .add("metrics")
                                .add("filters")
                                .add("sorts")
                                .add("limit")
                                .add("explanation")),
                object(
                        "datasetKey", "canvas_daily_stats",
                        "dimensions", List.of("stat_date"),
                        "metrics", List.of("total_executions"),
                        "filters", List.of(),
                        "sorts", List.of(),
                        "limit", 100,
                        "explanation", "Default BI semantic-layer query plan.")));
        templates.put(4L, builtIn(
                4L,
                "BI Interpretation Agent",
                "bi_interpretation",
                """
                        Explain the BI subject using only the supplied semantic query and result.
                        Subject type: ${subjectType}
                        Subject key: ${subjectKey}
                        Question: ${question}
                        Query: ${query}
                        Result: ${result}
                        Semantic catalog: ${datasets}
                        Return summary, keyFindings, and recommendations.
                        """,
                object("type", "object",
                        "properties", object(
                                "summary", object("type", "string"),
                                "keyFindings", object("type", "array"),
                                "recommendations", object("type", "array")),
                        "required", objectMapper.createArrayNode()
                                .add("summary")
                                .add("keyFindings")
                                .add("recommendations")),
                object(
                        "summary", "The BI result is available for semantic-layer interpretation.",
                        "keyFindings", List.of("Review the returned metrics and dimensions."),
                        "recommendations", List.of("Compare against recent baselines before acting."))));
        templates.put(5L, builtIn(
                5L,
                "BI Report Agent",
                "bi_report",
                """
                        Generate a BI report from validated semantic query sections.
                        Report type: ${reportType}
                        Title: ${title}
                        Sections: ${sections}
                        Semantic catalog: ${datasets}
                        Return title, executiveSummary, sections, and nextActions.
                        """,
                object("type", "object",
                        "properties", object(
                                "title", object("type", "string"),
                                "executiveSummary", object("type", "string"),
                                "sections", object("type", "array"),
                                "nextActions", object("type", "array")),
                        "required", objectMapper.createArrayNode()
                                .add("title")
                                .add("executiveSummary")
                                .add("sections")
                                .add("nextActions")),
                object(
                        "title", "BI Report",
                        "executiveSummary", "The validated BI sections are ready for review.",
                        "sections", List.of(java.util.Map.of("title", "Overview", "body", "Review the attached BI data.")),
                        "nextActions", List.of("Review metric changes with the owning team."))));
        templates.put(6L, builtIn(
                6L,
                "BI Dashboard Draft Agent",
                "bi_dashboard_draft",
                """
                        Generate a dashboard draft using only the supplied BI semantic catalog.
                        Prompt: ${prompt}
                        Requested dataset key: ${requestedDatasetKey}
                        Semantic catalog: ${datasets}
                        Return dashboard, charts, and explanation.
                        """,
                object("type", "object",
                        "properties", object(
                                "dashboard", object("type", "object"),
                                "charts", object("type", "array"),
                                "explanation", object("type", "string")),
                        "required", objectMapper.createArrayNode()
                                .add("dashboard")
                                .add("charts")
                                .add("explanation")),
                object(
                        "dashboard", java.util.Map.of(
                                "dashboardKey", "ai-canvas-daily-stats",
                                "title", "AI Canvas Daily Stats",
                                "description", "AI-generated semantic-layer dashboard draft.",
                                "datasetKey", "canvas_daily_stats",
                                "widgets", List.of(java.util.Map.of(
                                        "widgetKey", "trend-executions",
                                        "title", "Execution Trend",
                                        "chartType", "LINE",
                                        "dimensions", List.of("stat_date"),
                                        "metrics", List.of("total_executions"),
                                        "gridX", 0,
                                        "gridY", 0,
                                        "gridW", 12,
                                        "gridH", 6,
                                        "stylePreset", "time-series")),
                                "filters", List.of(),
                                "interactions", List.of(),
                                "subscriptionChannels", List.of(),
                                "embedScopes", List.of()),
                        "charts", List.of(),
                        "explanation", "Default dashboard draft generated from semantic metadata.")));
        templates.put(7L, builtIn(
                7L,
                "BI Insight Agent",
                "bi_insight",
                """
                        Discover BI trends, anomalies, and opportunities from validated semantic results.
                        Question: ${question}
                        Dataset: ${dataset}
                        Query: ${query}
                        Current result: ${currentResult}
                        Baseline result: ${baselineResult}
                        Return trends, anomalies, and opportunities.
                        """,
                object("type", "object",
                        "properties", object(
                                "trends", object("type", "array"),
                                "anomalies", object("type", "array"),
                                "opportunities", object("type", "array")),
                        "required", objectMapper.createArrayNode()
                                .add("trends")
                                .add("anomalies")
                                .add("opportunities")),
                object(
                        "trends", List.of("Review current metric movement."),
                        "anomalies", List.of(),
                        "opportunities", List.of("Investigate segments with positive movement."))));
        templates.put(9L, builtIn(
                9L,
                "Marketing Monitor Inference Agent",
                "marketing_monitor_inference",
                """
                        Analyze this monitored marketing mention. Return JSON only.
                        Mention context: ${text}
                        Brand key: ${brandKey}
                        Source type: ${sourceType}
                        Language: ${language}
                        Metadata: ${metadata}
                        Return sentimentLabel, sentimentScore, confidence, entities, topics, riskFlags, and evidence.
                        """,
                object("type", "object",
                        "properties", object(
                                "sentimentLabel", object("type", "string"),
                                "sentimentScore", object("type", "number"),
                                "confidence", object("type", "number"),
                                "entities", object("type", "array"),
                                "topics", object("type", "array"),
                                "riskFlags", object("type", "array"),
                                "evidence", object("type", "object")),
                        "required", objectMapper.createArrayNode()
                                .add("sentimentLabel")
                                .add("sentimentScore")
                                .add("confidence")
                                .add("entities")
                                .add("topics")
                                .add("riskFlags")
                                .add("evidence")),
                object(
                        "sentimentLabel", "NEUTRAL",
                        "sentimentScore", 0,
                        "confidence", 0.35,
                        "entities", List.of(),
                        "topics", List.of(),
                        "riskFlags", List.of("GENERATOR_FALLBACK"),
                        "evidence", java.util.Map.of("summary", "Default monitoring inference fallback."))));
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
