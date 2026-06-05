package org.chovy.canvas.domain.analytics;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.AudienceBitmapRollbackDO;
import org.chovy.canvas.dal.dataobject.AudienceMaterializationRunDO;
import org.chovy.canvas.dal.mapper.AudienceBitmapRollbackMapper;
import org.chovy.canvas.dal.mapper.AudienceMaterializationRunMapper;
import org.chovy.canvas.domain.warehouse.CdpWarehouseAvailabilityService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseConsumerAvailabilityService;
import org.chovy.canvas.engine.audience.VersionedAudienceBitmapStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class AudienceMaterializationOperationsService {

    private static final int MAX_LIMIT = 100;
    private static final int MAX_REASON_LENGTH = 512;
    private static final String DEFAULT_OPERATOR = "operator";

    private final AudienceMaterializationService materializationService;
    private final AudienceMaterializationRunMapper runMapper;
    private final VersionedAudienceBitmapStore bitmapStore;
    private final AudienceBitmapRollbackMapper rollbackMapper;
    private final CdpWarehouseAvailabilityService availabilityService;
    private final CdpWarehouseConsumerAvailabilityService consumerAvailabilityService;

    public AudienceMaterializationOperationsService(AudienceMaterializationService materializationService,
                                                    AudienceMaterializationRunMapper runMapper) {
        this(materializationService, runMapper, null, null,
                (CdpWarehouseAvailabilityService) null,
                (CdpWarehouseConsumerAvailabilityService) null);
    }

    public AudienceMaterializationOperationsService(AudienceMaterializationService materializationService,
                                                    AudienceMaterializationRunMapper runMapper,
                                                    VersionedAudienceBitmapStore bitmapStore,
                                                    AudienceBitmapRollbackMapper rollbackMapper) {
        this(materializationService, runMapper, bitmapStore, rollbackMapper,
                (CdpWarehouseAvailabilityService) null,
                (CdpWarehouseConsumerAvailabilityService) null);
    }

    @Autowired
    public AudienceMaterializationOperationsService(AudienceMaterializationService materializationService,
                                                    AudienceMaterializationRunMapper runMapper,
                                                    VersionedAudienceBitmapStore bitmapStore,
                                                    AudienceBitmapRollbackMapper rollbackMapper,
                                                    ObjectProvider<CdpWarehouseAvailabilityService>
                                                            availabilityServiceProvider,
                                                    ObjectProvider<CdpWarehouseConsumerAvailabilityService>
                                                            consumerAvailabilityServiceProvider) {
        this(materializationService,
                runMapper,
                bitmapStore,
                rollbackMapper,
                availabilityServiceProvider == null ? null : availabilityServiceProvider.getIfAvailable(),
                consumerAvailabilityServiceProvider == null
                        ? null
                        : consumerAvailabilityServiceProvider.getIfAvailable());
    }

    AudienceMaterializationOperationsService(AudienceMaterializationService materializationService,
                                             AudienceMaterializationRunMapper runMapper,
                                             VersionedAudienceBitmapStore bitmapStore,
                                             AudienceBitmapRollbackMapper rollbackMapper,
                                             CdpWarehouseAvailabilityService availabilityService) {
        this(materializationService, runMapper, bitmapStore, rollbackMapper, availabilityService, null);
    }

    AudienceMaterializationOperationsService(AudienceMaterializationService materializationService,
                                             AudienceMaterializationRunMapper runMapper,
                                             VersionedAudienceBitmapStore bitmapStore,
                                             AudienceBitmapRollbackMapper rollbackMapper,
                                             CdpWarehouseAvailabilityService availabilityService,
                                             CdpWarehouseConsumerAvailabilityService consumerAvailabilityService) {
        this.materializationService = materializationService;
        this.runMapper = runMapper;
        this.bitmapStore = bitmapStore;
        this.rollbackMapper = rollbackMapper;
        this.availabilityService = availabilityService;
        this.consumerAvailabilityService = consumerAvailabilityService;
    }

    public AudienceMaterializationService.MaterializationResult materialize(Long tenantId,
                                                                           Long audienceId,
                                                                           String operator) {
        if (audienceId == null || audienceId <= 0) {
            throw new IllegalArgumentException("audienceId must be positive");
        }
        return materializationService.materialize(normalizeTenant(tenantId), audienceId, normalizeOperator(operator));
    }

    public GatedMaterializationResult materializeWithAvailabilityGate(Long tenantId,
                                                                      Long audienceId,
                                                                      LocalDateTime from,
                                                                      LocalDateTime to,
                                                                      String mode,
                                                                      boolean allowWarn,
                                                                      String operator) {
        if (audienceId == null || audienceId <= 0) {
            throw new IllegalArgumentException("audienceId must be positive");
        }
        if (availabilityService == null) {
            throw new IllegalStateException("warehouse availability service is not configured");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        CdpWarehouseAvailabilityService.AvailabilityDecision availability =
                availabilityService.evaluate(scopedTenantId, from, to, mode);
        String availabilityStatus = availability == null ? "FAIL" : availability.status();
        if ("FAIL".equalsIgnoreCase(availabilityStatus)) {
            return new GatedMaterializationResult(
                    scopedTenantId,
                    audienceId,
                    "BLOCKED",
                    "warehouse availability FAIL",
                    availability,
                    null);
        }
        if ("WARN".equalsIgnoreCase(availabilityStatus) && !allowWarn) {
            return new GatedMaterializationResult(
                    scopedTenantId,
                    audienceId,
                    "BLOCKED",
                    "warehouse availability WARN requires allowWarn=true",
                    availability,
                    null);
        }
        AudienceMaterializationService.MaterializationResult materialization =
                materializationService.materialize(scopedTenantId, audienceId, normalizeOperator(operator));
        String reason = "WARN".equalsIgnoreCase(availabilityStatus)
                ? "warehouse availability WARN accepted by operator"
                : "warehouse availability PASS";
        return new GatedMaterializationResult(
                scopedTenantId,
                audienceId,
                "TRIGGERED",
                reason,
                availability,
                materialization);
    }

    public ContractGatedMaterializationResult materializeWithConsumerAvailabilityContract(
            Long tenantId,
            Long audienceId,
            String contractKey,
            LocalDateTime from,
            LocalDateTime to,
            String operator) {
        if (audienceId == null || audienceId <= 0) {
            throw new IllegalArgumentException("audienceId must be positive");
        }
        if (!hasText(contractKey)) {
            throw new IllegalArgumentException("contractKey is required");
        }
        if (consumerAvailabilityService == null) {
            throw new IllegalStateException("warehouse consumer availability service is not configured");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityEvaluation evaluation =
                consumerAvailabilityService.evaluateContract(scopedTenantId, contractKey, from, to);
        if (evaluation == null || !evaluation.allowed()) {
            return new ContractGatedMaterializationResult(
                    scopedTenantId,
                    audienceId,
                    contractKey,
                    "BLOCKED",
                    evaluation == null
                            ? "consumer availability contract evaluation failed"
                            : evaluation.message(),
                    evaluation,
                    null);
        }
        AudienceMaterializationService.MaterializationResult materialization =
                materializationService.materialize(scopedTenantId, audienceId, normalizeOperator(operator));
        return new ContractGatedMaterializationResult(
                scopedTenantId,
                audienceId,
                contractKey,
                "TRIGGERED",
                evaluation.message(),
                evaluation,
                materialization);
    }

    public RollbackView rollback(Long tenantId,
                                 Long audienceId,
                                 Long targetVersion,
                                 String operator,
                                 String reason) {
        Long scopedTenantId = normalizeTenant(tenantId);
        if (audienceId == null || audienceId <= 0) {
            throw new IllegalArgumentException("audienceId must be positive");
        }
        if (targetVersion == null || targetVersion <= 0) {
            throw new IllegalArgumentException("targetVersion must be positive");
        }
        if (!hasText(reason)) {
            throw new IllegalArgumentException("reason is required");
        }
        if (bitmapStore == null || rollbackMapper == null) {
            throw new IllegalStateException("audience bitmap rollback is not configured");
        }

        VersionedAudienceBitmapStore.RollbackResult result =
                bitmapStore.rollbackToVersion(scopedTenantId, audienceId, targetVersion);
        String normalizedOperator = normalizeOperator(operator);
        String normalizedReason = limitReason(reason);
        LocalDateTime createdAt = LocalDateTime.now();

        AudienceBitmapRollbackDO row = new AudienceBitmapRollbackDO();
        row.setTenantId(scopedTenantId);
        row.setAudienceId(audienceId);
        row.setTargetVersion(targetVersion);
        row.setTargetBitmapKey(result.targetBitmapKey());
        row.setRolledBackVersions((long) result.rolledBackVersions());
        row.setStatus(result.status());
        row.setReason(normalizedReason);
        row.setOperator(normalizedOperator);
        row.setCreatedAt(createdAt);
        rollbackMapper.insert(row);

        return new RollbackView(
                row.getId(),
                scopedTenantId,
                audienceId,
                targetVersion,
                result.targetBitmapKey(),
                result.rolledBackVersions(),
                result.status(),
                normalizedReason,
                normalizedOperator,
                createdAt);
    }

    public List<RunView> recentRuns(Long tenantId, Long audienceId, String status, int limit) {
        LambdaQueryWrapper<AudienceMaterializationRunDO> query =
                new LambdaQueryWrapper<AudienceMaterializationRunDO>()
                        .eq(AudienceMaterializationRunDO::getTenantId, normalizeTenant(tenantId))
                        .orderByDesc(AudienceMaterializationRunDO::getStartedAt)
                        .orderByDesc(AudienceMaterializationRunDO::getId)
                        .last("LIMIT " + boundLimit(limit));
        if (audienceId != null) {
            query.eq(AudienceMaterializationRunDO::getAudienceId, audienceId);
        }
        if (hasText(status)) {
            query.eq(AudienceMaterializationRunDO::getStatus, status.trim().toUpperCase(Locale.ROOT));
        }
        List<AudienceMaterializationRunDO> rows = runMapper.selectList(query);
        return rows == null ? List.of() : rows.stream().map(this::toView).toList();
    }

    private RunView toView(AudienceMaterializationRunDO row) {
        return new RunView(
                row.getId(),
                row.getTenantId(),
                row.getAudienceId(),
                row.getVersion(),
                row.getStatus(),
                nullToZero(row.getMatchedUsers()),
                row.getBitmapKey(),
                row.getErrorMessage(),
                row.getStartedAt(),
                row.getFinishedAt(),
                row.getCreatedBy());
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String normalizeOperator(String operator) {
        return hasText(operator) ? operator.trim() : DEFAULT_OPERATOR;
    }

    private int boundLimit(int limit) {
        if (limit <= 0) {
            return 20;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String limitReason(String reason) {
        String value = reason.trim();
        if (value.length() <= MAX_REASON_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_REASON_LENGTH);
    }

    public record RollbackView(
            Long id,
            Long tenantId,
            Long audienceId,
            Long targetVersion,
            String targetBitmapKey,
            int rolledBackVersions,
            String status,
            String reason,
            String operator,
            LocalDateTime createdAt) {
    }

    public record RunView(
            Long id,
            Long tenantId,
            Long audienceId,
            Long version,
            String status,
            long matchedUsers,
            String bitmapKey,
            String errorMessage,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            String createdBy) {
    }

    public record GatedMaterializationResult(
            Long tenantId,
            Long audienceId,
            String status,
            String reason,
            CdpWarehouseAvailabilityService.AvailabilityDecision availability,
            AudienceMaterializationService.MaterializationResult materialization) {
    }

    public record ContractGatedMaterializationResult(
            Long tenantId,
            Long audienceId,
            String contractKey,
            String status,
            String reason,
            CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityEvaluation consumerAvailability,
            AudienceMaterializationService.MaterializationResult materialization) {
    }
}
