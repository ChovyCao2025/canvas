package org.chovy.canvas.domain.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
/**
 * AiPromptEvaluationService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class AiPromptEvaluationService {

    private final AiProviderModelRegistryService providerRegistry;
    private final AiPromptTemplateService templateService;
    private final CopyOnWriteArrayList<EvaluationAuditEvent> auditEvents = new CopyOnWriteArrayList<>();

    /**
     * 初始化 AiPromptEvaluationService 实例。
     *
     * @param providerRegistry provider registry 参数，用于 AiPromptEvaluationService 流程中的校验、计算或对象转换。
     * @param templateService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public AiPromptEvaluationService(AiProviderModelRegistryService providerRegistry,
                                     AiPromptTemplateService templateService) {
        this.providerRegistry = providerRegistry;
        this.templateService = templateService;
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param req 请求对象，承载本次操作的输入参数。
     * @return 返回 evaluate 流程生成的业务结果。
     */
    public EvaluationResult evaluate(Long tenantId, EvaluationRequest req) {
        // 准备本次处理所需的上下文和中间变量。
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
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return result;
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @return 返回符合条件的数据列表或视图。
     */
    public List<EvaluationAuditEvent> recentAudits() {
        return List.copyOf(auditEvents);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param defaultValue 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String blankToDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    /**
     * EvaluationRequest 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record EvaluationRequest(
            Long providerId,
            Long templateId,
            String modelKey,
            String promptOverride,
            JsonNode variables,
            JsonNode mockOutput) {
    }

    /**
     * EvaluationResult 承载对应领域的业务规则、流程编排和结果转换。
     */
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

    /**
     * EvaluationAuditEvent 承载对应领域的业务规则、流程编排和结果转换。
     */
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
