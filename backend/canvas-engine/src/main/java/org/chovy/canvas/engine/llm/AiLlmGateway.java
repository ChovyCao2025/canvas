package org.chovy.canvas.engine.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.domain.ai.AiPromptTemplateService;
import org.chovy.canvas.domain.ai.AiProviderModelRegistryService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
/**
 * AiLlmGateway 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
 */
public class AiLlmGateway {

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_PROVIDER_DISABLED = "PROVIDER_DISABLED";
    public static final String STATUS_UNSUPPORTED_PROVIDER = "UNSUPPORTED_PROVIDER";
    public static final String STATUS_INVALID_JSON = "INVALID_JSON";
    public static final String STATUS_SCHEMA_MISMATCH = "SCHEMA_MISMATCH";
    public static final String STATUS_CONFIG_ERROR = "CONFIG_ERROR";
    public static final String STATUS_PROVIDER_ERROR = "PROVIDER_ERROR";
    public static final String STATUS_TIMEOUT = "TIMEOUT";

    private static final long DEFAULT_PROVIDER_ID = 1L;
    private static final int DEFAULT_TIMEOUT_MS = 3_000;
    private static final int MAX_TOKENS_LIMIT = 8_000;

    private final AiProviderModelRegistryService providerRegistry;
    private final AiPromptTemplateService templateService;
    private final AiUsageAuditService auditService;
    private final ObjectMapper objectMapper;
    private final List<LlmClient> clients;

    /**
     * 初始化 AiLlmGateway 实例。
     *
     * @param providerRegistry provider registry 参数，用于 AiLlmGateway 流程中的校验、计算或对象转换。
     * @param templateService 依赖组件，用于完成数据访问或外部能力调用。
     * @param auditService 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clients 依赖组件，用于完成数据访问或外部能力调用。
     */
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

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 evaluate 流程生成的业务结果。
     */
    public Mono<AiLlmResult> evaluate(Long tenantId, AiLlmRequest request) {
        long startedAt = System.nanoTime();
        Long scopedTenantId = normalizeTenantId(tenantId);
        AiPromptTemplateService.TemplateDetail template =
                templateService.requireEnabledTemplate(scopedTenantId, request.templateId());
        String prompt = templateService.renderTemplate(
                blankToDefault(request.promptOverride(), template.promptTemplate()),
                request.variables());

        ProviderPlan plan = providerPlan(scopedTenantId, request);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        JsonNode outputSchema = outputSchema(request, template);

        LlmClient.LlmRequest llmRequest = new LlmClient.LlmRequest(
                plan.provider().endpoint(),
                plan.modelKey(),
                prompt,
                outputSchema,
                template.defaultValues(),
                boundedParams(request.params()),
                boundedTimeoutMs(request.timeoutMs()),
                plan.provider().apiKey());

        // 汇总前面计算出的状态和明细，返回给调用方。
        return client.complete(llmRequest)
                .timeout(Duration.ofMillis(boundedTimeoutMs(request.timeoutMs())))
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .map(response -> acceptedOrFallback(scopedTenantId, request, template, prompt,
                        outputSchema, plan.modelKey(), plan.providerId(), response, startedAt))
                .onErrorResume(java.util.concurrent.TimeoutException.class,
                        ex -> Mono.just(fallback(scopedTenantId, request, template, prompt, plan.modelKey(),
                                plan.providerId(), STATUS_TIMEOUT, "AI provider request timed out", startedAt)))
                .onErrorResume(LlmInvalidJsonException.class,
                        ex -> Mono.just(fallback(scopedTenantId, request, template, prompt, plan.modelKey(),
                                plan.providerId(), STATUS_INVALID_JSON, message(ex), startedAt)))
                .onErrorResume(ex -> Mono.just(fallback(scopedTenantId, request, template, prompt, plan.modelKey(),
                        plan.providerId(), STATUS_PROVIDER_ERROR, message(ex), startedAt)));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param request 请求对象，承载本次操作的输入参数。
     * @param template template 参数，用于 acceptedOrFallback 流程中的校验、计算或对象转换。
     * @param prompt prompt 参数，用于 acceptedOrFallback 流程中的校验、计算或对象转换。
     * @param modelKey 业务键，用于在同一租户下定位资源。
     * @param providerId 业务对象 ID，用于定位具体记录。
     * @param response response 参数，用于 acceptedOrFallback 流程中的校验、计算或对象转换。
     * @param startedAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 acceptedOrFallback 流程生成的业务结果。
     */
    private AiLlmResult acceptedOrFallback(Long tenantId,
                                           AiLlmRequest request,
                                           AiPromptTemplateService.TemplateDetail template,
                                           String prompt,
                                           JsonNode outputSchema,
                                           String modelKey,
                                           Long providerId,
                                           LlmClient.LlmResponse response,
                                           long startedAt) {
        JsonNode output = response == null ? null : response.output();
        if (output == null || !output.isObject()) {
            return fallback(tenantId, request, template, prompt, modelKey, providerId,
                    STATUS_INVALID_JSON, "AI provider did not return a JSON object", startedAt);
        }
        if (!templateService.matchesSchema(outputSchema, output)) {
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 providerPlan 流程生成的业务结果。
     */
    private ProviderPlan providerPlan(Long tenantId, AiLlmRequest request) {
        Long providerId = request.providerId() == null ? DEFAULT_PROVIDER_ID : request.providerId();
        try {
            AiProviderModelRegistryService.ProviderCallView provider =
                    providerRegistry.requireEnabledProviderForCall(tenantId, providerId);
            String modelKey = blankToDefault(request.modelKey(), providerRegistry.defaultModelKey(tenantId, provider.id()));
            return new ProviderPlan(provider.id(), provider, modelKey, null, null);
        } catch (IllegalArgumentException e) {
            String status = disabledProvider(e) ? STATUS_PROVIDER_DISABLED : STATUS_CONFIG_ERROR;
            return new ProviderPlan(providerId, null, blankToDefault(request.modelKey(), "mock-marketing-v1"),
                    status, e.getMessage());
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param providerType 类型标识，用于选择对应处理分支。
     * @return 返回 clientFor 流程生成的业务结果。
     */
    private LlmClient clientFor(String providerType) {
        return clients.stream()
                .filter(client -> client.supports(providerType))
                .findFirst()
                .orElse(null);
    }

    private static JsonNode outputSchema(AiLlmRequest request, AiPromptTemplateService.TemplateDetail template) {
        JsonNode override = request.schemaOverride();
        return override == null || override.isNull() ? template.outputSchema() : override;
    }

    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param request 请求对象，承载本次操作的输入参数。
     * @param template template 参数，用于 success 流程中的校验、计算或对象转换。
     * @param prompt prompt 参数，用于 success 流程中的校验、计算或对象转换。
     * @param modelKey 业务键，用于在同一租户下定位资源。
     * @param providerId 业务对象 ID，用于定位具体记录。
     * @param output output 参数，用于 success 流程中的校验、计算或对象转换。
     * @param promptTokens prompt tokens 参数，用于 success 流程中的校验、计算或对象转换。
     * @param completionTokens completion tokens 参数，用于 success 流程中的校验、计算或对象转换。
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     * @param startedAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 success 流程生成的业务结果。
     */
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

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param request 请求对象，承载本次操作的输入参数。
     * @param template template 参数，用于 fallback 流程中的校验、计算或对象转换。
     * @param prompt prompt 参数，用于 fallback 流程中的校验、计算或对象转换。
     * @param modelKey 业务键，用于在同一租户下定位资源。
     * @param providerId 业务对象 ID，用于定位具体记录。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     * @param startedAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 fallback 流程生成的业务结果。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param request 请求对象，承载本次操作的输入参数。
     * @param template template 参数，用于 finish 流程中的校验、计算或对象转换。
     * @param prompt prompt 参数，用于 finish 流程中的校验、计算或对象转换。
     * @param modelKey 业务键，用于在同一租户下定位资源。
     * @param providerId 业务对象 ID，用于定位具体记录。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param fallbackUsed fallback used 参数，用于 finish 流程中的校验、计算或对象转换。
     * @param output output 参数，用于 finish 流程中的校验、计算或对象转换。
     * @param promptTokens prompt tokens 参数，用于 finish 流程中的校验、计算或对象转换。
     * @param completionTokens completion tokens 参数，用于 finish 流程中的校验、计算或对象转换。
     * @param errorCode 业务编码，用于匹配对应类型或状态。
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     * @param startedAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 finish 流程生成的业务结果。
     */
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
        // 准备本次处理所需的上下文和中间变量。
        long latencyMs = Math.max(1L, (System.nanoTime() - startedAt) / 1_000_000L);
        AiLlmResult result = new AiLlmResult(
                status,
                fallbackUsed,
                providerId,
                template.id(),
                modelKey,
                prompt,
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return result;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param providerType 类型标识，用于选择对应处理分支。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeProviderType(String providerType) {
        return providerType == null ? "" : providerType.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static Long normalizeTenantId(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param timeoutMs 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static int boundedTimeoutMs(Integer timeoutMs) {
        if (timeoutMs == null || timeoutMs <= 0) {
            return DEFAULT_TIMEOUT_MS;
        }
        return Math.min(timeoutMs, 30_000);
    }

    private static Map<String, Object> boundedParams(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> bounded = new LinkedHashMap<>();
        params.forEach((key, value) -> {
            if (key == null || value == null) {
                return;
            }
            String normalizedKey = key.trim();
            if (normalizedKey.isEmpty()
                    || "maxTokens".equals(normalizedKey)
                    || "max_tokens".equals(normalizedKey)
                    || "temperature".equals(normalizedKey)) {
                return;
            }
            bounded.put(normalizedKey, value);
        });
        Integer maxTokens = intParam(firstPresent(params.get("maxTokens"), params.get("max_tokens")));
        if (maxTokens != null) {
            bounded.put("max_tokens", Math.max(1, Math.min(maxTokens, MAX_TOKENS_LIMIT)));
        }
        Double temperature = doubleParam(params.get("temperature"));
        if (temperature != null) {
            bounded.put("temperature", Math.max(0.0, Math.min(temperature, 2.0)));
        }
        return Map.copyOf(bounded);
    }

    private static Object firstPresent(Object first, Object second) {
        return first == null ? second : first;
    }

    private static Integer intParam(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Double doubleParam(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static boolean disabledProvider(IllegalArgumentException ex) {
        String text = ex == null || ex.getMessage() == null ? "" : ex.getMessage().toLowerCase(Locale.ROOT);
        return text.contains("disabled");
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param defaultValue 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param throwable throwable 参数，用于 message 流程中的校验、计算或对象转换。
     * @return 返回 message 生成的文本或业务键。
     */
    private static String message(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        Throwable cause = throwable.getCause() == null ? throwable : throwable.getCause();
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }

    /**
     * ProviderPlan 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
     */
    private record ProviderPlan(
            Long providerId,
            AiProviderModelRegistryService.ProviderCallView provider,
            String modelKey,
            String fallbackStatus,
            String message) {
    }

    /**
     * AiLlmRequest 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
     */
    public record AiLlmRequest(
            Long providerId,
            Long templateId,
            String modelKey,
            String promptOverride,
            JsonNode variables,
            JsonNode schemaOverride,
            Map<String, Object> params,
            Integer timeoutMs,
            Long canvasId,
            String executionId,
            String nodeId) {
    }

    /**
     * AiLlmResult 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
     */
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
