package org.chovy.canvas.domain.warehouse;

import org.chovy.canvas.dal.dataobject.CdpWarehouseEnterpriseOlapEvidenceCollectionRunDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseEnterpriseOlapEvidenceCollectionRunMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class CdpWarehouseEnterpriseOlapEvidenceCollectionService {

    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_PASS = "PASS";
    private static final String STATUS_WARN = "WARN";
    private static final String STATUS_FAIL = "FAIL";
    private static final long PROOF_MAX_AGE_MINUTES = 15;

    private final CdpWarehouseEnterpriseOlapEvidenceService evidenceService;
    private final CdpWarehouseEnterpriseOlapEvidenceCollectionRunMapper mapper;
    private final Clock clock;

    @Autowired
    public CdpWarehouseEnterpriseOlapEvidenceCollectionService(
            CdpWarehouseEnterpriseOlapEvidenceService evidenceService,
            CdpWarehouseEnterpriseOlapEvidenceCollectionRunMapper mapper) {
        this(evidenceService, mapper, Clock.systemDefaultZone());
    }

    CdpWarehouseEnterpriseOlapEvidenceCollectionService(
            CdpWarehouseEnterpriseOlapEvidenceService evidenceService,
            CdpWarehouseEnterpriseOlapEvidenceCollectionRunMapper mapper,
            Clock clock) {
        this.evidenceService = evidenceService;
        this.mapper = mapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    public CollectionRunView run(Long tenantId, String triggerType, String actor) {
        Long scopedTenantId = tenantId == null ? 0L : tenantId;
        String trigger = defaultString(triggerType, "MANUAL").toUpperCase(Locale.ROOT);
        String createdBy = defaultString(actor, "system");
        LocalDateTime startedAt = now();
        CdpWarehouseEnterpriseOlapEvidenceCollectionRunDO row = new CdpWarehouseEnterpriseOlapEvidenceCollectionRunDO();
        row.setTenantId(scopedTenantId);
        row.setTriggerType(trigger);
        row.setStatus(STATUS_RUNNING);
        row.setStartedAt(startedAt);
        row.setEvidenceCount(0);
        row.setPassCount(0);
        row.setWarnCount(0);
        row.setFailCount(0);
        row.setReason("enterprise OLAP evidence collection running");
        row.setCreatedBy(createdBy);
        mapper.insert(row);
        try {
            CdpWarehouseEnterpriseOlapEvidenceService.EvidenceBundle bundle =
                    evidenceService.collectAutomatedEvidence(scopedTenantId, createdBy);
            Counts counts = counts(bundle.evidence());
            String status = normalizeStatus(bundle.status());
            String reason = "recorded " + counts.evidenceCount() + " enterprise OLAP evidence rows";
            LocalDateTime finishedAt = now();
            mapper.updateFinished(row.getId(), status, finishedAt, counts.evidenceCount(), counts.passCount(),
                    counts.warnCount(), counts.failCount(), reason);
            return new CollectionRunView(row.getId(), scopedTenantId, trigger, status, startedAt, finishedAt,
                    counts.evidenceCount(), counts.passCount(), counts.warnCount(), counts.failCount(), reason,
                    createdBy);
        } catch (RuntimeException e) {
            String reason = message(e);
            LocalDateTime finishedAt = now();
            mapper.updateFinished(row.getId(), STATUS_FAIL, finishedAt, 0, 0, 0, 0, reason);
            return new CollectionRunView(row.getId(), scopedTenantId, trigger, STATUS_FAIL, startedAt, finishedAt,
                    0, 0, 0, 0, reason, createdBy);
        }
    }

    public List<CollectionRunView> recentRuns(Long tenantId, int limit) {
        Long scopedTenantId = tenantId == null ? 0L : tenantId;
        int boundedLimit = Math.max(1, Math.min(limit <= 0 ? 20 : limit, 100));
        return safeList(mapper.listRecent(scopedTenantId, boundedLimit)).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .map(this::toView)
                .toList();
    }

    public CdpWarehouseProductionReadinessProofService.ProofEvidence proofEvidence(Long tenantId) {
        Long scopedTenantId = tenantId == null ? 0L : tenantId;
        List<CdpWarehouseEnterpriseOlapEvidenceCollectionRunDO> rows = safeList(mapper.listRecent(scopedTenantId, 1))
                .stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .toList();
        if (rows.isEmpty()) {
            return new CdpWarehouseProductionReadinessProofService.ProofEvidence(
                    "enterprise_olap:evidence_collection",
                    STATUS_FAIL,
                    "enterprise OLAP automated evidence collection run is missing");
        }
        CdpWarehouseEnterpriseOlapEvidenceCollectionRunDO latest = rows.getFirst();
        if (latest.getFinishedAt() == null || isExpired(latest.getFinishedAt())) {
            return new CdpWarehouseProductionReadinessProofService.ProofEvidence(
                    "enterprise_olap:evidence_collection",
                    STATUS_FAIL,
                    "enterprise OLAP automated evidence collection run expired");
        }
        String status = normalizeStatus(latest.getStatus());
        return new CdpWarehouseProductionReadinessProofService.ProofEvidence(
                "enterprise_olap:evidence_collection",
                status,
                defaultString(latest.getReason(), "enterprise OLAP automated evidence collection " + status));
    }

    private Counts counts(List<CdpWarehouseEnterpriseOlapEvidenceService.EvidenceView> evidence) {
        int pass = 0;
        int warn = 0;
        int fail = 0;
        for (CdpWarehouseEnterpriseOlapEvidenceService.EvidenceView row : safeList(evidence)) {
            String status = normalizeStatus(row.status());
            if (STATUS_PASS.equals(status)) {
                pass++;
            } else if (STATUS_WARN.equals(status)) {
                warn++;
            } else {
                fail++;
            }
        }
        return new Counts(safeList(evidence).size(), pass, warn, fail);
    }

    private CollectionRunView toView(CdpWarehouseEnterpriseOlapEvidenceCollectionRunDO row) {
        return new CollectionRunView(row.getId(), row.getTenantId(), row.getTriggerType(), row.getStatus(),
                row.getStartedAt(), row.getFinishedAt(), value(row.getEvidenceCount()), value(row.getPassCount()),
                value(row.getWarnCount()), value(row.getFailCount()), row.getReason(), row.getCreatedBy());
    }

    private String normalizeStatus(String status) {
        String value = status == null ? STATUS_FAIL : status.trim().toUpperCase(Locale.ROOT);
        if (STATUS_PASS.equals(value) || STATUS_WARN.equals(value) || STATUS_FAIL.equals(value)) {
            return value;
        }
        return STATUS_FAIL;
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String message(RuntimeException e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private boolean isExpired(LocalDateTime finishedAt) {
        return Duration.between(finishedAt, now()).toMinutes() > PROOF_MAX_AGE_MINUTES;
    }

    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    private record Counts(int evidenceCount, int passCount, int warnCount, int failCount) {
    }

    public record CollectionRunView(
            Long id,
            Long tenantId,
            String triggerType,
            String status,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            int evidenceCount,
            int passCount,
            int warnCount,
            int failCount,
            String reason,
            String createdBy) {
    }
}
