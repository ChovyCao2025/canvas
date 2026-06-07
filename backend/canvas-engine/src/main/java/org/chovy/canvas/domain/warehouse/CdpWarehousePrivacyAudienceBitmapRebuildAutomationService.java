package org.chovy.canvas.domain.warehouse;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
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

    public CdpWarehousePrivacyAudienceBitmapRebuildAutomationService(
            CdpWarehousePrivacyErasureService erasureService,
            CdpWarehousePrivacyAudienceBitmapRebuildService rebuildService) {
        this.erasureService = erasureService;
        this.rebuildService = rebuildService;
    }

    public AutomationResult run(Long tenantId, AutomationCommand command) {
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
        List<RequestAutomationResult> results = safeRequests(requests).stream()
                .map(request -> processRequest(scopedTenantId, request, actor, audienceLimit, retryFailed))
                .toList();
        int eligible = (int) results.stream().filter(RequestAutomationResult::eligible).count();
        int triggered = (int) results.stream().filter(result -> "TRIGGERED".equals(result.action())).count();
        int failed = (int) results.stream().filter(result -> FAIL.equals(result.status())).count();
        int skipped = results.size() - triggered;
        String status = failed > 0 ? FAIL : (triggered > 0 ? PASS : SKIPPED);
        return new AutomationResult(scopedTenantId, status, results.size(), eligible, triggered, skipped, failed, results);
    }

    private RequestAutomationResult processRequest(Long tenantId,
                                                   CdpWarehousePrivacyErasureService.ErasureRequestView request,
                                                   String actor,
                                                   int audienceLimit,
                                                   boolean retryFailed) {
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
            return new RequestAutomationResult(request.id(), "TRIGGERED", FAIL, true,
                    "audience bitmap rebuild proof failed: " + message(e), null);
        }
    }

    private boolean upstreamPassed(CdpWarehousePrivacyErasureService.ErasureRequestView request) {
        return safeProofs(request.assetProofs()).stream()
                .filter(proof -> proof != null && !ASSET_KEY.equalsIgnoreCase(proof.assetKey()))
                .allMatch(proof -> {
                    String status = normalizeStatus(proof.status());
                    return PASS.equals(status) || SKIPPED.equals(status);
                });
    }

    private CdpWarehousePrivacyErasureService.AssetProofView audienceProof(
            CdpWarehousePrivacyErasureService.ErasureRequestView request) {
        return safeProofs(request.assetProofs()).stream()
                .filter(proof -> proof != null && ASSET_KEY.equalsIgnoreCase(proof.assetKey()))
                .findFirst()
                .orElse(null);
    }

    private List<CdpWarehousePrivacyErasureService.ErasureRequestView> safeRequests(
            List<CdpWarehousePrivacyErasureService.ErasureRequestView> requests) {
        return requests == null ? List.of() : requests;
    }

    private List<CdpWarehousePrivacyErasureService.AssetProofView> safeProofs(
            List<CdpWarehousePrivacyErasureService.AssetProofView> proofs) {
        return proofs == null ? List.of() : proofs;
    }

    private int bound(Integer value, int fallback, int max) {
        int scoped = value == null || value <= 0 ? fallback : value;
        return Math.min(scoped, max);
    }

    private String actor(String actor) {
        return actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor.trim();
    }

    private String normalizeStatus(String status) {
        return status == null || status.isBlank() ? FAIL : status.trim().toUpperCase(Locale.ROOT);
    }

    private String message(RuntimeException e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    public record AutomationCommand(
            String actor,
            Integer scanLimit,
            Integer audienceLimit,
            Boolean retryFailed) {
    }

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

    public record RequestAutomationResult(
            Long requestId,
            String action,
            String status,
            boolean eligible,
            String reason,
            CdpWarehousePrivacyAudienceBitmapRebuildService.AudienceBitmapRebuildResult rebuildResult) {
    }
}
