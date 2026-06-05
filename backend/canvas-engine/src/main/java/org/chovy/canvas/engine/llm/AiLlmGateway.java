package org.chovy.canvas.engine.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.domain.ai.AiPromptTemplateService;
import org.chovy.canvas.domain.ai.AiProviderModelRegistryService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AiLlmGateway {

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_PROVIDER_DISABLED = "PROVIDER_DISABLED";
    public static final String STATUS_UNSUPPORTED_PROVIDER = "UNSUPPORTED_PROVIDER";
    public static final String STATUS_INVALID_JSON = "INVALID_JSON";
    public static final String STATUS_SCHEMA_MISMATCH = "SCHEMA_MISMATCH";
    public static final String STATUS_PROVIDER_ERROR = "PROVIDER_ERROR";
    public static final String STATUS_TIMEOUT = "TIMEOUT";

    private static final long DEFAULT_PROVIDER_ID = 1L;
    private static final int DEFAULT_TIMEOUT_MS = 3_000;

    private final AiProviderModelRegistryService providerRegistry;
    private final AiPromptTemplateService templateService;
    private final AiUsageAuditService auditService;
    private final ObjectMapper objectMapper;
    private final List<LlmClient> clients;

    public AiLlmGateway(AiProviderModelRegistryService providerRegistry,
                        AiPromptTemplateService templateService,
                        AiUsageAuditService auditService,
                        ObjectMapper objectMapper,
                        List<LlmClient> clients) {
        this.providerRegistry = providerRegistry;
        this.templateService = templateService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.clients = clients == null ? List.of() : List.copyOf(clients);
    }

    public Mono<AiLlmResult> evaluate(Long tenantId, AiLlmRequest request) {
        long startedAt = System.nanoTime();
        Long scopedTenantId = normalizeTenantId(tenantId);
        AiPromptTemplateService.TemplateDetail template =
                templateService.requireEnabledTemplate(scopedTenantId, request.templateId());
        String prompt = templateService.renderTemplate(
                blankToDefault(request.promptOverride(), template.promptTemplate()),
                request.variables());

        ProviderPlan plan = providerPlan(scopedTenantId, request);
        if (plan.fallbackStatus() != null) {
            return Mono.just(fallback(scopedTenantId, request, template, prompt, plan.modelKey(),
                    plan.providerId(), plan.fallbackStatus(), plan.message(), startedAt));
        }
        if (LlmProviderType.MOCK.equals(normalizeProviderType(plan.provider().providerType()))) {
            return Mono.just(success(scopedTenantId, request, template, prompt, plan.modelKey(), plan.providerId(),
                    template.defaultValues().deepCopy(), null, null, "mock provider returned template defaults", startedAt));
        }

        LlmClient client = clientFor(plan.provider().providerType());
        if (client == null) {
            return Mono.just(fallback(scopedTenantId, request, template, prompt, plan.modelKey(), plan.providerId(),
                    STATUS_UNSUPPORTED_PROVIDER, "unsupported AI provider type: " + plan.provider().providerType(), startedAt));
        }

        LlmClient.LlmRequest llmRequest = new LlmClient.LlmRequest(
                plan.provider().endpoint(),
                plan.modelKey(),
                prompt,
                template.outputSchema(),
                template.defaultValues(),
                request.params(),
                boundedTimeoutMs(request.timeoutMs()));

        return client.complete(llmRequest)
                .timeout(Duration.ofMillis(boundedTimeoutMs(request.timeoutMs())))
                .map(response -> acceptedOrFallback(scopedTenantId, request, template, prompt,
                        plan.modelKey(), plan.providerId(), response, startedAt))
                .onErrorResume(java.util.concurrent.TimeoutException.class,
                        ex -> Mono.just(fallback(scopedTenantId, request, template, prompt, plan.modelKey(),
                                plan.providerId(), STATUS_TIMEOUT, "AI provider request timed out", startedAt)))
                .onErrorResume(ex -> Mono.just(fallback(scopedTenantId, request, template, prompt, plan.modelKey(),
                        plan.providerId(), STATUS_PROVIDER_ERROR, message(ex), startedAt)));
    }

    private AiLlmResult acceptedOrFallback(Long tenantId,
                                           AiLlmRequest request,
                                           AiPromptTemplateService.TemplateDetail template,
                                           String prompt,
                                           String modelKey,
                                           Long providerId,
                                           LlmClient.LlmResponse response,
                                           long startedAt) {
        JsonNode output = response == null ? null : response.output();
        if (output == null || !output.isObject()) {
            return fallback(tenantId, request, template, prompt, modelKey, providerId,
                    STATUS_INVALID_JSON, "AI provider did not return a JSON object", startedAt);
        }
        if (!templateService.matchesSchema(template.outputSchema(), output)) {
            return fallback(tenantId, request, template, prompt, modelKey, providerId,
                    STATUS_SCHEMA_MISMATCH, "AI provider output did not satisfy template schema", startedAt);
        }
        return success(tenantId, request, template, prompt, modelKey, providerId,
                output.deepCopy(),
                response.promptTokens(),
                response.completionTokens(),
                "AI provider output accepted",
                startedAt);
    }

    private ProviderPlan providerPlan(Long tenantId, AiLlmRequest request) {
        Long providerId = request.providerId() == null ? DEFAULT_PROVIDER_ID : request.providerId();
        try {
            AiProviderModelRegistryService.ProviderView provider =
                    providerRegistry.requireEnabledProvider(tenantId, providerId);
            String modelKey = blankToDefault(request.modelKey(), providerRegistry.defaultModelKey(tenantId, provider.id()));
            return new ProviderPlan(provider.id(), provider, modelKey, null, null);
        } catch (IllegalArgumentException e) {
            return new ProviderPlan(providerId, null, blankToDefault(request.modelKey(), "mock-marketing-v1"),
                    STATUS_PROVIDER_DISABLED, e.getMessage());
        }
    }

    private LlmClient clientFor(String providerType) {
        return clients.stream()
                .filter(client -> client.supports(providerType))
                .findFirst()
                .orElse(null);
    }

    private AiLlmResult success(Long tenantId,
                                AiLlmRequest request,
                                AiPromptTemplateService.TemplateDetail template,
                                String prompt,
                                String modelKey,
                                Long providerId,
                                JsonNode output,
                                Integer promptTokens,
                                Integer completionTokens,
                                String message,
                                long startedAt) {
        return finish(tenantId, request, template, prompt, modelKey, providerId, STATUS_SUCCESS, false,
                output, promptTokens, completionTokens, null, message, startedAt);
    }

    private AiLlmResult fallback(Long tenantId,
                                 AiLlmRequest request,
                                 AiPromptTemplateService.TemplateDetail template,
                                 String prompt,
                                 String modelKey,
                                 Long providerId,
                                 String status,
                                 String message,
                                 long startedAt) {
        return finish(tenantId, request, template, prompt, modelKey, providerId, status, true,
                template.defaultValues().deepCopy(), null, null, status, message, startedAt);
    }

    private AiLlmResult finish(Long tenantId,
                               AiLlmRequest request,
                               AiPromptTemplateService.TemplateDetail template,
                               String prompt,
                               String modelKey,
                               Long providerId,
                               String status,
                               boolean fallbackUsed,
                               JsonNode output,
                               Integer promptTokens,
                               Integer completionTokens,
                               String errorCode,
                               String message,
                               long startedAt) {
        long latencyMs = Math.max(1L, (System.nanoTime() - startedAt) / 1_000_000L);
        AiLlmResult result = new AiLlmResult(
                status,
                fallbackUsed,
                providerId,
                template.id(),
                modelKey,
                prompt,
                output == null ? objectMapper.createObjectNode() : output.deepCopy(),
                message,
                latencyMs,
                promptTokens,
                completionTokens);
        auditService.record(new AiUsageAuditService.AiUsageAuditEvent(
                Instant.now(),
                tenantId,
                request.canvasId(),
                request.executionId(),
                request.nodeId(),
                providerId,
                template.id(),
                modelKey,
                status,
                fallbackUsed,
                latencyMs,
                promptTokens,
                completionTokens,
                result.output().deepCopy(),
                errorCode,
                message));
        return result;
    }

    private static String normalizeProviderType(String providerType) {
        return providerType == null ? "" : providerType.trim().toUpperCase(Locale.ROOT);
    }

    private static Long normalizeTenantId(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private static int boundedTimeoutMs(Integer timeoutMs) {
        if (timeoutMs == null || timeoutMs <= 0) {
            return DEFAULT_TIMEOUT_MS;
        }
        return Math.min(timeoutMs, 30_000);
    }

    private static String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static String message(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        Throwable cause = throwable.getCause() == null ? throwable : throwable.getCause();
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }

    private record ProviderPlan(
            Long providerId,
            AiProviderModelRegistryService.ProviderView provider,
            String modelKey,
            String fallbackStatus,
            String message) {
    }

    public record AiLlmRequest(
            Long providerId,
            Long templateId,
            String modelKey,
            String promptOverride,
            JsonNode variables,
            Map<String, Object> params,
            Integer timeoutMs,
            Long canvasId,
            String executionId,
            String nodeId) {
    }

    public record AiLlmResult(
            String status,
            boolean fallbackUsed,
            Long providerId,
            Long templateId,
            String modelKey,
            String renderedPrompt,
            JsonNode output,
            String message,
            long latencyMs,
            Integer promptTokens,
            Integer completionTokens) {
    }
}
