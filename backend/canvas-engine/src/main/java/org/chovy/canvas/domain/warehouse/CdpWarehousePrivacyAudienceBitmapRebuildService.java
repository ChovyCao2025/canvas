package org.chovy.canvas.domain.warehouse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;
import org.chovy.canvas.dal.mapper.AudienceDefinitionMapper;
import org.chovy.canvas.domain.analytics.AudienceMaterializationOperationsService;
import org.chovy.canvas.domain.analytics.AudienceMaterializationService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class CdpWarehousePrivacyAudienceBitmapRebuildService {

    private static final String ASSET_KEY = "AUDIENCE_BITMAP_VERSION";
    private static final String PASS = "PASS";
    private static final String WARN = "WARN";
    private static final String FAIL = "FAIL";
    private static final String SKIPPED = "SKIPPED";
    private static final String SUCCESS = "SUCCESS";
    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 1000;
    private static final int MAX_MESSAGE_LENGTH = 1000;
    private static final String DEFAULT_ACTOR = "privacy-audience-rebuild";

    private final CdpWarehousePrivacyErasureService erasureService;
    private final AudienceDefinitionMapper definitionMapper;
    private final AudienceMaterializationOperationsService operationsService;

    public CdpWarehousePrivacyAudienceBitmapRebuildService(
            CdpWarehousePrivacyErasureService erasureService,
            AudienceDefinitionMapper definitionMapper,
            AudienceMaterializationOperationsService operationsService) {
        this.erasureService = erasureService;
        this.definitionMapper = definitionMapper;
        this.operationsService = operationsService;
    }

    public AudienceBitmapRebuildResult rebuild(Long tenantId,
                                               Long requestId,
                                               AudienceBitmapRebuildCommand command) {
        Long scopedTenantId = normalizeTenant(tenantId);
        AudienceBitmapRebuildCommand scopedCommand =
                command == null ? new AudienceBitmapRebuildCommand(null, null, null) : command;
        String actor = actor(scopedCommand.actor());
        CdpWarehousePrivacyErasureService.ErasureRequestView request =
                erasureService.get(scopedTenantId, requestId);

        List<CdpWarehousePrivacyErasureService.AssetProofView> blockers =
                upstreamBlockers(request.assetProofs());
        if (!blockers.isEmpty()) {
            String message = limit("audience bitmap rebuild blocked until upstream erasure proofs pass: "
                    + blockerSummary(blockers));
            recordProof(scopedTenantId, requestId, WARN, 0, 0, message, null, actor);
            return new AudienceBitmapRebuildResult(
                    scopedTenantId, requestId, WARN, true, false, 0, 0, 0, 0, List.of());
        }

        int limit = boundLimit(scopedCommand.limit());
        List<AudienceDefinitionDO> selected = selectCandidates(scopedTenantId, scopedCommand.audienceIds(), limit);
        boolean truncated = selected.size() > limit;
        if (truncated) {
            selected = selected.subList(0, limit);
        }
        if (selected.isEmpty()) {
            String message = "no enabled offline or hybrid audience materializations require rebuild";
            recordProof(scopedTenantId, requestId, SKIPPED, 0, 0, message, null, actor);
            return new AudienceBitmapRebuildResult(
                    scopedTenantId, requestId, SKIPPED, false, false, 0, 0, 0, 0, List.of());
        }

        List<AudienceRebuildItem> results = selected.stream()
                .map(definition -> rebuildAudience(scopedTenantId, definition.getId(), actor))
                .toList();
        int rebuilt = (int) results.stream().filter(item -> SUCCESS.equalsIgnoreCase(item.status())).count();
        int failed = results.size() - rebuilt;
        long matchedUsers = results.stream().mapToLong(AudienceRebuildItem::matchedUsers).sum();
        String status = aggregateStatus(failed, truncated);
        String message = proofMessage(status, results.size(), rebuilt, failed, truncated, limit);
        String errorMessage = failed > 0 ? failedSummary(results) : null;
        recordProof(scopedTenantId, requestId, status, results.size(), rebuilt, message, errorMessage, actor);

        return new AudienceBitmapRebuildResult(
                scopedTenantId,
                requestId,
                status,
                false,
                truncated,
                results.size(),
                rebuilt,
                failed,
                matchedUsers,
                results);
    }

    private List<CdpWarehousePrivacyErasureService.AssetProofView> upstreamBlockers(
            List<CdpWarehousePrivacyErasureService.AssetProofView> proofs) {
        if (proofs == null || proofs.isEmpty()) {
            return List.of();
        }
        return proofs.stream()
                .filter(proof -> proof != null && !ASSET_KEY.equalsIgnoreCase(proof.assetKey()))
                .filter(proof -> !isTerminalPass(proof.status()))
                .toList();
    }

    private boolean isTerminalPass(String status) {
        String normalized = normalizeStatus(status);
        return PASS.equals(normalized) || SKIPPED.equals(normalized);
    }

    private List<AudienceDefinitionDO> selectCandidates(Long tenantId, List<Long> audienceIds, int limit) {
        LambdaQueryWrapper<AudienceDefinitionDO> query = new LambdaQueryWrapper<AudienceDefinitionDO>()
                .eq(AudienceDefinitionDO::getTenantId, tenantId)
                .eq(AudienceDefinitionDO::getEnabled, 1)
                .in(AudienceDefinitionDO::getEvaluationStrategy, List.of("OFFLINE_BATCH", "HYBRID"))
                .orderByAsc(AudienceDefinitionDO::getUpdatedAt)
                .orderByAsc(AudienceDefinitionDO::getId)
                .last("LIMIT " + (limit + 1));
        List<Long> scopedIds = audienceIds(audienceIds);
        if (!scopedIds.isEmpty()) {
            query.in(AudienceDefinitionDO::getId, scopedIds);
        }
        List<AudienceDefinitionDO> rows = definitionMapper.selectList(query);
        return rows == null ? List.of() : rows;
    }

    private AudienceRebuildItem rebuildAudience(Long tenantId, Long audienceId, String actor) {
        try {
            AudienceMaterializationService.MaterializationResult result =
                    operationsService.materialize(tenantId, audienceId, actor);
            String status = result == null ? FAIL : normalizeStatus(result.status());
            long matchedUsers = result == null ? 0 : Math.max(result.matchedUsers(), 0);
            String message = SUCCESS.equals(status)
                    ? "rebuilt audience " + audienceId
                    : "audience " + audienceId + " materialization returned " + status;
            return new AudienceRebuildItem(audienceId, status, matchedUsers, message);
        } catch (RuntimeException e) {
            return new AudienceRebuildItem(
                    audienceId, FAIL, 0, "audience " + audienceId + " rebuild failed: " + e.getMessage());
        }
    }

    private void recordProof(Long tenantId,
                             Long requestId,
                             String status,
                             long matchedCount,
                             long affectedCount,
                             String proofMessage,
                             String errorMessage,
                             String actor) {
        erasureService.recordAssetProof(tenantId, requestId,
                new CdpWarehousePrivacyErasureService.AssetProofCommand(
                        ASSET_KEY,
                        "ADS",
                        "REBUILD",
                        status,
                        matchedCount,
                        affectedCount,
                        limit(proofMessage),
                        limit(errorMessage),
                        actor,
                        LocalDateTime.now()));
    }

    private String aggregateStatus(int failed, boolean truncated) {
        if (failed > 0) {
            return FAIL;
        }
        return truncated ? WARN : PASS;
    }

    private String proofMessage(String status, int selected, int rebuilt, int failed, boolean truncated, int limit) {
        if (FAIL.equals(status)) {
            return "rebuilt " + rebuilt + " of " + selected
                    + " audience bitmap version(s); " + failed + " failed";
        }
        if (truncated) {
            return "rebuilt " + rebuilt
                    + " audience bitmap version(s); candidate selection truncated by limit " + limit;
        }
        return "rebuilt " + rebuilt + " audience bitmap version(s) after privacy erasure request";
    }

    private String failedSummary(List<AudienceRebuildItem> results) {
        return limit(results.stream()
                .filter(item -> !SUCCESS.equalsIgnoreCase(item.status()))
                .map(AudienceRebuildItem::message)
                .reduce((left, right) -> left + "; " + right)
                .orElse(null));
    }

    private String blockerSummary(List<CdpWarehousePrivacyErasureService.AssetProofView> blockers) {
        return blockers.stream()
                .map(proof -> proof.assetKey() + "=" + normalizeStatus(proof.status()))
                .reduce((left, right) -> left + ", " + right)
                .orElse("unknown upstream asset");
    }

    private List<Long> audienceIds(List<Long> input) {
        if (input == null || input.isEmpty()) {
            return List.of();
        }
        Set<Long> ids = new LinkedHashSet<>();
        for (Long id : input) {
            if (id != null && id > 0) {
                ids.add(id);
            }
        }
        return List.copyOf(ids);
    }

    private int boundLimit(Integer input) {
        int value = input == null || input <= 0 ? DEFAULT_LIMIT : input;
        return Math.min(value, MAX_LIMIT);
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String actor(String actor) {
        return actor != null && !actor.isBlank() ? actor.trim() : DEFAULT_ACTOR;
    }

    private String normalizeStatus(String status) {
        return status == null || status.isBlank() ? FAIL : status.trim().toUpperCase(Locale.ROOT);
    }

    private String limit(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= MAX_MESSAGE_LENGTH ? value : value.substring(0, MAX_MESSAGE_LENGTH);
    }

    public record AudienceBitmapRebuildCommand(
            String actor,
            Integer limit,
            List<Long> audienceIds) {
        public AudienceBitmapRebuildCommand {
            audienceIds = audienceIds == null ? List.of() : List.copyOf(audienceIds);
        }
    }

    public record AudienceBitmapRebuildResult(
            Long tenantId,
            Long requestId,
            String status,
            boolean blocked,
            boolean truncated,
            int selectedAudiences,
            int rebuiltAudiences,
            int failedAudiences,
            long matchedUsers,
            List<AudienceRebuildItem> audienceResults) {
        public AudienceBitmapRebuildResult {
            audienceResults = audienceResults == null ? List.of() : List.copyOf(audienceResults);
        }
    }

    public record AudienceRebuildItem(
            Long audienceId,
            String status,
            long matchedUsers,
            String message) {
    }
}
