package org.chovy.canvas.domain.warehouse;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
/**
 * CdpWarehousePrivacyAudienceBitmapRebuildAutomationService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class CdpWarehousePrivacyAudienceBitmapRebuildAutomationService {

    private static final String ASSET_KEY = "AUDIENCE_BITMAP_VERSION";
    private static final String PASS = "PASS";
    private static final String FAIL = "FAIL";
    private static final String SKIPPED = "SKIPPED";
    private static final int DEFAULT_SCAN_LIMIT = 50;
    private static final int MAX_SCAN_LIMIT = 100;
    private static final int DEFAULT_AUDIENCE_LIMIT = 100;
    private static final int MAX_AUDIENCE_LIMIT = 1000;
    private static final String DEFAULT_ACTOR = "privacy-audience-rebuild-automation";

    private final CdpWarehousePrivacyErasureService erasureService;
    private final CdpWarehousePrivacyAudienceBitmapRebuildService rebuildService;

    /**
     * 初始化 CdpWarehousePrivacyAudienceBitmapRebuildAutomationService 实例。
     *
     * @param erasureService 依赖组件，用于完成数据访问或外部能力调用。
     * @param rebuildService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehousePrivacyAudienceBitmapRebuildAutomationService(
            CdpWarehousePrivacyErasureService erasureService,
            CdpWarehousePrivacyAudienceBitmapRebuildService rebuildService) {
        this.erasureService = erasureService;
        this.rebuildService = rebuildService;
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
    public AutomationResult run(Long tenantId, AutomationCommand command) {
        // 准备本次处理所需的上下文和中间变量。
        Long scopedTenantId = tenantId == null ? 0L : tenantId;
        AutomationCommand scopedCommand = command == null
                ? new AutomationCommand(null, null, null, null)
                : command;
        String actor = actor(scopedCommand.actor());
        int scanLimit = bound(scopedCommand.scanLimit(), DEFAULT_SCAN_LIMIT, MAX_SCAN_LIMIT);
        int audienceLimit = bound(scopedCommand.audienceLimit(), DEFAULT_AUDIENCE_LIMIT, MAX_AUDIENCE_LIMIT);
        boolean retryFailed = Boolean.TRUE.equals(scopedCommand.retryFailed());

        List<CdpWarehousePrivacyErasureService.ErasureRequestView> requests =
                erasureService.recent(scopedTenantId, null, scanLimit);
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        List<RequestAutomationResult> results = safeRequests(requests).stream()
                .map(request -> processRequest(scopedTenantId, request, actor, audienceLimit, retryFailed))
                .toList();
        int eligible = (int) results.stream().filter(RequestAutomationResult::eligible).count();
        int triggered = (int) results.stream().filter(result -> "TRIGGERED".equals(result.action())).count();
        int failed = (int) results.stream().filter(result -> FAIL.equals(result.status())).count();
        int skipped = results.size() - triggered;
        String status = failed > 0 ? FAIL : (triggered > 0 ? PASS : SKIPPED);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new AutomationResult(scopedTenantId, status, results.size(), eligible, triggered, skipped, failed, results);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param request 请求对象，承载本次操作的输入参数。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param audienceLimit audience limit 参数，用于 processRequest 流程中的校验、计算或对象转换。
     * @param retryFailed retry failed 参数，用于 processRequest 流程中的校验、计算或对象转换。
     * @return 返回 processRequest 流程生成的业务结果。
     */
    private RequestAutomationResult processRequest(Long tenantId,
                                                   CdpWarehousePrivacyErasureService.ErasureRequestView request,
                                                   String actor,
                                                   int audienceLimit,
                                                   boolean retryFailed) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (request == null || request.id() == null) {
            return new RequestAutomationResult(null, "SKIP", SKIPPED, false, "request is missing", null);
        }
        if (PASS.equals(normalizeStatus(request.status()))) {
            return new RequestAutomationResult(request.id(), "SKIP", SKIPPED, false,
                    "request already passed", null);
        }
        CdpWarehousePrivacyErasureService.AssetProofView audienceProof = audienceProof(request);
        if (audienceProof == null) {
            return new RequestAutomationResult(request.id(), "SKIP", SKIPPED, false,
                    "audience bitmap proof is missing", null);
        }
        String audienceStatus = normalizeStatus(audienceProof.status());
        if (PASS.equals(audienceStatus) || SKIPPED.equals(audienceStatus)) {
            return new RequestAutomationResult(request.id(), "SKIP", SKIPPED, false,
                    "audience bitmap proof is already terminal", null);
        }
        if (FAIL.equals(audienceStatus) && !retryFailed) {
            return new RequestAutomationResult(request.id(), "SKIP", SKIPPED, false,
                    "audience bitmap proof failed; set retryFailed=true to retry", null);
        }
        if (!upstreamPassed(request)) {
            return new RequestAutomationResult(request.id(), "SKIP", SKIPPED, false,
                    "upstream erasure proofs are not all PASS or SKIPPED", null);
        }

        CdpWarehousePrivacyAudienceBitmapRebuildService.AudienceBitmapRebuildCommand rebuildCommand =
                new CdpWarehousePrivacyAudienceBitmapRebuildService.AudienceBitmapRebuildCommand(
                        actor, audienceLimit, null);
        try {
            CdpWarehousePrivacyAudienceBitmapRebuildService.AudienceBitmapRebuildResult rebuild =
                    rebuildService.rebuild(tenantId, request.id(), rebuildCommand);
            String status = normalizeStatus(rebuild == null ? null : rebuild.status());
            return new RequestAutomationResult(request.id(), "TRIGGERED", status, true,
                    "audience bitmap rebuild proof triggered", rebuild);
        } catch (RuntimeException e) {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return new RequestAutomationResult(request.id(), "TRIGGERED", FAIL, true,
                    "audience bitmap rebuild proof failed: " + message(e), null);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 upstream passed 的布尔判断结果。
     */
    private boolean upstreamPassed(CdpWarehousePrivacyErasureService.ErasureRequestView request) {
        return safeProofs(request.assetProofs()).stream()
                .filter(proof -> proof != null && !ASSET_KEY.equalsIgnoreCase(proof.assetKey()))
                .allMatch(proof -> {
                    String status = normalizeStatus(proof.status());
                    return PASS.equals(status) || SKIPPED.equals(status);
                });
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 audienceProof 流程生成的业务结果。
     */
    private CdpWarehousePrivacyErasureService.AssetProofView audienceProof(
            CdpWarehousePrivacyErasureService.ErasureRequestView request) {
        return safeProofs(request.assetProofs()).stream()
                .filter(proof -> proof != null && ASSET_KEY.equalsIgnoreCase(proof.assetKey()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param requests requests 参数，用于 safeRequests 流程中的校验、计算或对象转换。
     * @return 返回 safe requests 汇总后的集合、分页或映射视图。
     */
    private List<CdpWarehousePrivacyErasureService.ErasureRequestView> safeRequests(
            List<CdpWarehousePrivacyErasureService.ErasureRequestView> requests) {
        return requests == null ? List.of() : requests;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param proofs proofs 参数，用于 safeProofs 流程中的校验、计算或对象转换。
     * @return 返回 safe proofs 汇总后的集合、分页或映射视图。
     */
    private List<CdpWarehousePrivacyErasureService.AssetProofView> safeProofs(
            List<CdpWarehousePrivacyErasureService.AssetProofView> proofs) {
        return proofs == null ? List.of() : proofs;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 bound 流程中的校验、计算或对象转换。
     * @param max max 参数，用于 bound 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int bound(Integer value, int fallback, int max) {
        int scoped = value == null || value <= 0 ? fallback : value;
        return Math.min(scoped, max);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param actor 操作人标识，用于审计和权限判断。
     * @return 返回 actor 生成的文本或业务键。
     */
    private String actor(String actor) {
        return actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor.trim();
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeStatus(String status) {
        return status == null || status.isBlank() ? FAIL : status.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param e e 参数，用于 message 流程中的校验、计算或对象转换。
     * @return 返回 message 生成的文本或业务键。
     */
    private String message(RuntimeException e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    /**
     * AutomationCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record AutomationCommand(
            String actor,
            Integer scanLimit,
            Integer audienceLimit,
            Boolean retryFailed) {
    }

    /**
     * AutomationResult 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record AutomationResult(
            Long tenantId,
            String status,
            int scanned,
            int eligible,
            int triggered,
            int skipped,
            int failed,
            List<RequestAutomationResult> requestResults) {
        public AutomationResult {
            requestResults = requestResults == null ? List.of() : List.copyOf(requestResults);
        }
    }

    /**
     * RequestAutomationResult 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record RequestAutomationResult(
            Long requestId,
            String action,
            String status,
            boolean eligible,
            String reason,
            CdpWarehousePrivacyAudienceBitmapRebuildService.AudienceBitmapRebuildResult rebuildResult) {
    }
}
