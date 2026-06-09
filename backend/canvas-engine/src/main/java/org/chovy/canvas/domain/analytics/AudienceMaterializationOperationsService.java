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
/**
 * AudienceMaterializationOperationsService 承载对应领域的业务规则、流程编排和结果转换。
 */
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

    /**
     * 初始化 AudienceMaterializationOperationsService 实例。
     *
     * @param materializationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param runMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public AudienceMaterializationOperationsService(AudienceMaterializationService materializationService,
                                                    AudienceMaterializationRunMapper runMapper) {
        this(materializationService, runMapper, null, null,
                (CdpWarehouseAvailabilityService) null,
                (CdpWarehouseConsumerAvailabilityService) null);
    }

    /**
     * 初始化 AudienceMaterializationOperationsService 实例。
     *
     * @param materializationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param runMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param bitmapStore bitmap store 参数，用于 AudienceMaterializationOperationsService 流程中的校验、计算或对象转换。
     * @param rollbackMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public AudienceMaterializationOperationsService(AudienceMaterializationService materializationService,
                                                    AudienceMaterializationRunMapper runMapper,
                                                    VersionedAudienceBitmapStore bitmapStore,
                                                    AudienceBitmapRollbackMapper rollbackMapper) {
        this(materializationService, runMapper, bitmapStore, rollbackMapper,
                (CdpWarehouseAvailabilityService) null,
                (CdpWarehouseConsumerAvailabilityService) null);
    }

    @Autowired
    /**
     * 初始化 AudienceMaterializationOperationsService 实例。
     *
     * @param materializationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param runMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param bitmapStore bitmap store 参数，用于 AudienceMaterializationOperationsService 流程中的校验、计算或对象转换。
     * @param rollbackMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param availabilityServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param consumerAvailabilityServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     */
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

    /**
     * 初始化 AudienceMaterializationOperationsService 实例。
     *
     * @param materializationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param runMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param bitmapStore bitmap store 参数，用于 AudienceMaterializationOperationsService 流程中的校验、计算或对象转换。
     * @param rollbackMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param availabilityService 依赖组件，用于完成数据访问或外部能力调用。
     */
    AudienceMaterializationOperationsService(AudienceMaterializationService materializationService,
                                             AudienceMaterializationRunMapper runMapper,
                                             VersionedAudienceBitmapStore bitmapStore,
                                             AudienceBitmapRollbackMapper rollbackMapper,
                                             CdpWarehouseAvailabilityService availabilityService) {
        this(materializationService, runMapper, bitmapStore, rollbackMapper, availabilityService, null);
    }

    /**
     * 初始化 AudienceMaterializationOperationsService 实例。
     *
     * @param materializationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param runMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param bitmapStore bitmap store 参数，用于 AudienceMaterializationOperationsService 流程中的校验、计算或对象转换。
     * @param rollbackMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param availabilityService 依赖组件，用于完成数据访问或外部能力调用。
     * @param consumerAvailabilityService 依赖组件，用于完成数据访问或外部能力调用。
     */
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

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param audienceId 业务对象 ID，用于定位具体记录。
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回 materialize 流程生成的业务结果。
     */
    public AudienceMaterializationService.MaterializationResult materialize(Long tenantId,
                                                                           Long audienceId,
                                                                           String operator) {
        if (audienceId == null || audienceId <= 0) {
            throw new IllegalArgumentException("audienceId must be positive");
        }
        return materializationService.materialize(normalizeTenant(tenantId), audienceId, normalizeOperator(operator));
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param audienceId 业务对象 ID，用于定位具体记录。
     * @param from 时间或范围边界，用于限定统计窗口。
     * @param to 时间或范围边界，用于限定统计窗口。
     * @param mode mode 参数，用于 materializeWithAvailabilityGate 流程中的校验、计算或对象转换。
     * @param allowWarn allow warn 参数，用于 materializeWithAvailabilityGate 流程中的校验、计算或对象转换。
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回 materializeWithAvailabilityGate 流程生成的业务结果。
     */
    public GatedMaterializationResult materializeWithAvailabilityGate(Long tenantId,
                                                                      Long audienceId,
                                                                      LocalDateTime from,
                                                                      LocalDateTime to,
                                                                      String mode,
                                                                      boolean allowWarn,
                                                                      String operator) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new GatedMaterializationResult(
                scopedTenantId,
                audienceId,
                "TRIGGERED",
                reason,
                availability,
                materialization);
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param audienceId 业务对象 ID，用于定位具体记录。
     * @param contractKey 业务键，用于在同一租户下定位资源。
     * @param from 时间或范围边界，用于限定统计窗口。
     * @param to 时间或范围边界，用于限定统计窗口。
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回 materializeWithConsumerAvailabilityContract 流程生成的业务结果。
     */
    public ContractGatedMaterializationResult materializeWithConsumerAvailabilityContract(
            Long tenantId,
            Long audienceId,
            String contractKey,
            LocalDateTime from,
            LocalDateTime to,
            String operator) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new ContractGatedMaterializationResult(
                scopedTenantId,
                audienceId,
                contractKey,
                "TRIGGERED",
                evaluation.message(),
                evaluation,
                materialization);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param audienceId 业务对象 ID，用于定位具体记录。
     * @param targetVersion target version 参数，用于 rollback 流程中的校验、计算或对象转换。
     * @param operator 操作人标识，用于审计和权限判断。
     * @param reason 原因说明，用于记录状态变化的业务依据。
     * @return 返回 rollback 流程生成的业务结果。
     */
    public RollbackView rollback(Long tenantId,
                                 Long audienceId,
                                 Long targetVersion,
                                 String operator,
                                 String reason) {
        Long scopedTenantId = normalizeTenant(tenantId);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        rollbackMapper.insert(row);

        // 汇总前面计算出的状态和明细，返回给调用方。
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

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param audienceId 业务对象 ID，用于定位具体记录。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<RunView> recentRuns(Long tenantId, Long audienceId, String status, int limit) {
        LambdaQueryWrapper<AudienceMaterializationRunDO> query =
                new LambdaQueryWrapper<AudienceMaterializationRunDO>()
                        .eq(AudienceMaterializationRunDO::getTenantId, normalizeTenant(tenantId))
                        .orderByDesc(AudienceMaterializationRunDO::getStartedAt)
                        .orderByDesc(AudienceMaterializationRunDO::getId)
                        .last("LIMIT " + boundLimit(limit));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (audienceId != null) {
            query.eq(AudienceMaterializationRunDO::getAudienceId, audienceId);
        }
        if (hasText(status)) {
            query.eq(AudienceMaterializationRunDO::getStatus, status.trim().toUpperCase(Locale.ROOT));
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        List<AudienceMaterializationRunDO> rows = runMapper.selectList(query);
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return rows == null ? List.of() : rows.stream().map(this::toView).toList();
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeOperator(String operator) {
        return hasText(operator) ? operator.trim() : DEFAULT_OPERATOR;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int boundLimit(int limit) {
        if (limit <= 0) {
            return 20;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 null to zero 计算得到的数量、金额或指标值。
     */
    private long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param reason 原因说明，用于记录状态变化的业务依据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String limitReason(String reason) {
        String value = reason.trim();
        if (value.length() <= MAX_REASON_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_REASON_LENGTH);
    }

    /**
     * RollbackView 承载对应领域的业务规则、流程编排和结果转换。
     */
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

    /**
     * RunView 承载对应领域的业务规则、流程编排和结果转换。
     */
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

    /**
     * GatedMaterializationResult 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record GatedMaterializationResult(
            Long tenantId,
            Long audienceId,
            String status,
            String reason,
            CdpWarehouseAvailabilityService.AvailabilityDecision availability,
            AudienceMaterializationService.MaterializationResult materialization) {
    }

    /**
     * ContractGatedMaterializationResult 承载对应领域的业务规则、流程编排和结果转换。
     */
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
