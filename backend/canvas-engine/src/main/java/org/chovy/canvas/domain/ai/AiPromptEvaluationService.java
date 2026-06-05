package org.chovy.canvas.domain.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class AiPromptEvaluationService {

    private final AiProviderModelRegistryService providerRegistry;
    private final AiPromptTemplateService templateService;
    private final CopyOnWriteArrayList<EvaluationAuditEvent> auditEvents = new CopyOnWriteArrayList<>();

    public AiPromptEvaluationService(AiProviderModelRegistryService providerRegistry,
                                     AiPromptTemplateService templateService) {
        this.providerRegistry = providerRegistry;
        this.templateService = templateService;
    }

    public EvaluationResult evaluate(Long tenantId, EvaluationRequest req) {
        long startedAt = System.nanoTime();
        AiPromptTemplateService.TemplateDetail template = templateService.requireEnabledTemplate(tenantId, req.templateId());
        String renderedPrompt = templateService.renderTemplate(
                blankToDefault(req.promptOverride(), template.promptTemplate()),
                req.variables());

        AiProviderModelRegistryService.ProviderView provider = null;
        String modelKey = req.modelKey();
        String status = "FALLBACK";
        boolean fallbackUsed = true;
        String message = "mock evaluation used template defaults";
        JsonNode output = template.defaultValues().deepCopy();

        try {
            if (req.providerId() != null) {
                provider = providerRegistry.requireEnabledProvider(tenantId, req.providerId());
                modelKey = blankToDefault(modelKey, providerRegistry.defaultModelKey(tenantId, provider.id()));
            } else {
                modelKey = blankToDefault(modelKey, "mock-marketing-v1");
            }

            if (req.mockOutput() != null && !req.mockOutput().isNull()) {
                if (templateService.matchesSchema(template.outputSchema(), req.mockOutput())) {
                    output = req.mockOutput().deepCopy();
                    status = "SUCCESS";
                    fallbackUsed = false;
                    message = "mock output accepted";
                } else {
                    status = "INVALID_JSON";
                    message = "mock output did not satisfy template schema; defaults returned";
                }
            }
        } catch (IllegalArgumentException e) {
            status = "PROVIDER_DISABLED";
            message = e.getMessage();
        }

        long latencyMs = Math.max(1L, (System.nanoTime() - startedAt) / 1_000_000L);
        EvaluationResult result = new EvaluationResult(
                status,
                fallbackUsed,
                provider == null ? req.providerId() : provider.id(),
                template.id(),
                modelKey,
                renderedPrompt,
                output,
                message,
                latencyMs);
        auditEvents.add(new EvaluationAuditEvent(
                Instant.now(),
                AiProviderModelRegistryService.normalizeTenantId(tenantId),
                result.providerId(),
                result.templateId(),
                result.modelKey(),
                result.status(),
                result.fallbackUsed(),
                result.latencyMs(),
                result.message()));
        return result;
    }

    public List<EvaluationAuditEvent> recentAudits() {
        return List.copyOf(auditEvents);
    }

    private static String blankToDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    public record EvaluationRequest(
            Long providerId,
            Long templateId,
            String modelKey,
            String promptOverride,
            JsonNode variables,
            JsonNode mockOutput) {
    }

    public record EvaluationResult(
            String status,
            boolean fallbackUsed,
            Long providerId,
            Long templateId,
            String modelKey,
            String renderedPrompt,
            JsonNode output,
            String message,
            long latencyMs) {
    }

    public record EvaluationAuditEvent(
            Instant createdAt,
            Long tenantId,
            Long providerId,
            Long templateId,
            String modelKey,
            String status,
            boolean fallbackUsed,
            long latencyMs,
            String message) {
    }
}
