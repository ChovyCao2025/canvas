package org.chovy.canvas.domain.warehouse;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
/**
 * CdpWarehouseProductionReadinessProofService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class CdpWarehouseProductionReadinessProofService {

    private static final String MODE_HYBRID = "HYBRID";
    private static final String STATUS_PASS = "PASS";
    private static final String STATUS_WARN = "WARN";
    private static final String STATUS_FAIL = "FAIL";

    private final CdpWarehouseReadinessService readinessService;
    private final CdpWarehouseAvailabilityService availabilityService;
    private final CdpWarehouseConsumerAvailabilityService consumerAvailabilityService;
    private final ObjectProvider<CdpWarehousePrivacyErasureService> privacyErasureService;
    private final ObjectProvider<CdpWarehouseEnterpriseOlapReadinessService> enterpriseOlapReadinessService;
    private final ObjectProvider<CdpWarehouseEnterpriseOlapEvidenceService> enterpriseOlapEvidenceService;
    private final ObjectProvider<CdpWarehouseEnterpriseOlapEvidenceCollectionService> enterpriseOlapEvidenceCollectionService;

    /**
     * 初始化 CdpWarehouseProductionReadinessProofService 实例。
     *
     * @param readinessService 依赖组件，用于完成数据访问或外部能力调用。
     * @param availabilityService 依赖组件，用于完成数据访问或外部能力调用。
     * @param consumerAvailabilityService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseProductionReadinessProofService(
            CdpWarehouseReadinessService readinessService,
            CdpWarehouseAvailabilityService availabilityService,
            CdpWarehouseConsumerAvailabilityService consumerAvailabilityService) {
        this(readinessService, availabilityService, consumerAvailabilityService, null, null, null, null);
    }

    /**
     * 初始化 CdpWarehouseProductionReadinessProofService 实例。
     *
     * @param readinessService 依赖组件，用于完成数据访问或外部能力调用。
     * @param availabilityService 依赖组件，用于完成数据访问或外部能力调用。
     * @param consumerAvailabilityService 依赖组件，用于完成数据访问或外部能力调用。
     * @param privacyErasureService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseProductionReadinessProofService(
            CdpWarehouseReadinessService readinessService,
            CdpWarehouseAvailabilityService availabilityService,
            CdpWarehouseConsumerAvailabilityService consumerAvailabilityService,
            ObjectProvider<CdpWarehousePrivacyErasureService> privacyErasureService) {
        this(readinessService, availabilityService, consumerAvailabilityService, privacyErasureService, null, null,
                null);
    }

    /**
     * 初始化 CdpWarehouseProductionReadinessProofService 实例。
     *
     * @param readinessService 依赖组件，用于完成数据访问或外部能力调用。
     * @param availabilityService 依赖组件，用于完成数据访问或外部能力调用。
     * @param consumerAvailabilityService 依赖组件，用于完成数据访问或外部能力调用。
     * @param privacyErasureService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enterpriseOlapReadinessService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseProductionReadinessProofService(
            CdpWarehouseReadinessService readinessService,
            CdpWarehouseAvailabilityService availabilityService,
            CdpWarehouseConsumerAvailabilityService consumerAvailabilityService,
            ObjectProvider<CdpWarehousePrivacyErasureService> privacyErasureService,
            ObjectProvider<CdpWarehouseEnterpriseOlapReadinessService> enterpriseOlapReadinessService) {
        this(readinessService, availabilityService, consumerAvailabilityService, privacyErasureService,
                enterpriseOlapReadinessService, null, null);
    }

    /**
     * 初始化 CdpWarehouseProductionReadinessProofService 实例。
     *
     * @param readinessService 依赖组件，用于完成数据访问或外部能力调用。
     * @param availabilityService 依赖组件，用于完成数据访问或外部能力调用。
     * @param consumerAvailabilityService 依赖组件，用于完成数据访问或外部能力调用。
     * @param privacyErasureService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enterpriseOlapReadinessService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enterpriseOlapEvidenceService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseProductionReadinessProofService(
            CdpWarehouseReadinessService readinessService,
            CdpWarehouseAvailabilityService availabilityService,
            CdpWarehouseConsumerAvailabilityService consumerAvailabilityService,
            ObjectProvider<CdpWarehousePrivacyErasureService> privacyErasureService,
            ObjectProvider<CdpWarehouseEnterpriseOlapReadinessService> enterpriseOlapReadinessService,
            ObjectProvider<CdpWarehouseEnterpriseOlapEvidenceService> enterpriseOlapEvidenceService) {
        this(readinessService, availabilityService, consumerAvailabilityService, privacyErasureService,
                enterpriseOlapReadinessService, enterpriseOlapEvidenceService, null);
    }

    @Autowired
    /**
     * 初始化 CdpWarehouseProductionReadinessProofService 实例。
     *
     * @param readinessService 依赖组件，用于完成数据访问或外部能力调用。
     * @param availabilityService 依赖组件，用于完成数据访问或外部能力调用。
     * @param consumerAvailabilityService 依赖组件，用于完成数据访问或外部能力调用。
     * @param privacyErasureService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enterpriseOlapReadinessService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enterpriseOlapEvidenceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enterpriseOlapEvidenceCollectionService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseProductionReadinessProofService(
            CdpWarehouseReadinessService readinessService,
            CdpWarehouseAvailabilityService availabilityService,
            CdpWarehouseConsumerAvailabilityService consumerAvailabilityService,
            ObjectProvider<CdpWarehousePrivacyErasureService> privacyErasureService,
            ObjectProvider<CdpWarehouseEnterpriseOlapReadinessService> enterpriseOlapReadinessService,
            ObjectProvider<CdpWarehouseEnterpriseOlapEvidenceService> enterpriseOlapEvidenceService,
            ObjectProvider<CdpWarehouseEnterpriseOlapEvidenceCollectionService> enterpriseOlapEvidenceCollectionService) {
        this.readinessService = readinessService;
        this.availabilityService = availabilityService;
        this.consumerAvailabilityService = consumerAvailabilityService;
        this.privacyErasureService = privacyErasureService;
        this.enterpriseOlapReadinessService = enterpriseOlapReadinessService;
        this.enterpriseOlapEvidenceService = enterpriseOlapEvidenceService;
        this.enterpriseOlapEvidenceCollectionService = enterpriseOlapEvidenceCollectionService;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param from 时间或范围边界，用于限定统计窗口。
     * @param to 时间或范围边界，用于限定统计窗口。
     * @param mode mode 参数，用于 proof 流程中的校验、计算或对象转换。
     * @param contractKeys contract keys 参数，用于 proof 流程中的校验、计算或对象转换。
     * @return 返回 proof 流程生成的业务结果。
     */
    public ProductionReadinessProof proof(Long tenantId,
                                          LocalDateTime from,
                                          LocalDateTime to,
                                          String mode,
                                          List<String> contractKeys) {
        Long scopedTenantId = normalizeTenant(tenantId);
        LocalDateTime windowEnd = to == null ? LocalDateTime.now() : to;
        LocalDateTime windowStart = from == null ? windowEnd.minusHours(1) : from;
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (windowStart.isAfter(windowEnd)) {
            throw new IllegalArgumentException("from must be before or equal to to");
        }
        String normalizedMode = normalizeMode(mode);
        List<ProofEvidence> evidence = new ArrayList<>();
        List<ConsumerContractProof> contracts = new ArrayList<>();
        CdpWarehouseReadinessService.ReadinessSummary readiness = readiness(
                scopedTenantId, evidence);
        CdpWarehouseAvailabilityService.AvailabilityDecision availability = availability(
                scopedTenantId, windowStart, windowEnd, normalizedMode, evidence);
        addContractEvidence(scopedTenantId, windowStart, windowEnd, contractKeys, evidence, contracts);
        CdpWarehousePrivacyErasureService.BacklogSummary privacyErasureBacklog =
                privacyErasureBacklog(scopedTenantId, evidence);
        addEnterpriseOlapOperationalEvidence(scopedTenantId, evidence);
        addEnterpriseOlapCollectionRunEvidence(scopedTenantId, evidence);
        CdpWarehouseEnterpriseOlapReadinessService.EnterpriseOlapReadiness enterpriseOlapReadiness =
                enterpriseOlapReadiness(scopedTenantId, evidence);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new ProductionReadinessProof(
                scopedTenantId,
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                worstStatus(evidence.stream().map(ProofEvidence::status).toList()),
                LocalDateTime.now(),
                windowStart,
                windowEnd,
                normalizedMode,
                List.copyOf(evidence),
                readiness,
                availability,
                List.copyOf(contracts),
                privacyErasureBacklog,
                enterpriseOlapReadiness);
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param evidence evidence 参数，用于 readiness 流程中的校验、计算或对象转换。
     * @return 返回 readiness 流程生成的业务结果。
     */
    private CdpWarehouseReadinessService.ReadinessSummary readiness(
            Long tenantId,
            List<ProofEvidence> evidence) {
        try {
            CdpWarehouseReadinessService.ReadinessSummary summary = readinessService.readiness(tenantId);
            String status = normalizeStatus(summary == null ? null : summary.status());
            evidence.add(new ProofEvidence("warehouse_readiness", status,
                    summary == null ? "warehouse readiness summary is missing" : statusReason(status)));
            return summary;
        } catch (RuntimeException e) {
            evidence.add(new ProofEvidence("warehouse_readiness", STATUS_FAIL,
                    "warehouse readiness failed: " + message(e)));
            return null;
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param from 时间或范围边界，用于限定统计窗口。
     * @param to 时间或范围边界，用于限定统计窗口。
     * @param mode mode 参数，用于 availability 流程中的校验、计算或对象转换。
     * @param evidence evidence 参数，用于 availability 流程中的校验、计算或对象转换。
     * @return 返回 availability 流程生成的业务结果。
     */
    private CdpWarehouseAvailabilityService.AvailabilityDecision availability(
            Long tenantId,
            LocalDateTime from,
            LocalDateTime to,
            String mode,
            List<ProofEvidence> evidence) {
        try {
            CdpWarehouseAvailabilityService.AvailabilityDecision decision =
                    availabilityService.evaluate(tenantId, from, to, mode);
            String status = normalizeStatus(decision == null ? null : decision.status());
            evidence.add(new ProofEvidence("window_availability", status,
                    decision == null ? "warehouse availability decision is missing" : statusReason(status)));
            return decision;
        } catch (RuntimeException e) {
            evidence.add(new ProofEvidence("window_availability", STATUS_FAIL,
                    "window availability failed: " + message(e)));
            return null;
        }
    }

    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param from 时间或范围边界，用于限定统计窗口。
     * @param to 时间或范围边界，用于限定统计窗口。
     * @param contractKeys contract keys 参数，用于 addContractEvidence 流程中的校验、计算或对象转换。
     * @param evidence evidence 参数，用于 addContractEvidence 流程中的校验、计算或对象转换。
     * @param contracts contracts 参数，用于 addContractEvidence 流程中的校验、计算或对象转换。
     */
    private void addContractEvidence(Long tenantId,
                                     LocalDateTime from,
                                     LocalDateTime to,
                                     List<String> contractKeys,
                                     List<ProofEvidence> evidence,
                                     List<ConsumerContractProof> contracts) {
        List<String> keys = safeContractKeys(contractKeys);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (keys.isEmpty()) {
            evidence.add(new ProofEvidence("consumer_contracts", STATUS_WARN,
                    "no consumer contracts requested"));
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String contractKey : keys) {
            if (consumerAvailabilityService == null) {
                String reason = "consumer availability service is not configured";
                evidence.add(new ProofEvidence("consumer_contract:" + contractKey, STATUS_FAIL, reason));
                contracts.add(new ConsumerContractProof(contractKey, STATUS_FAIL, false, reason, null));
                continue;
            }
            try {
                CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityEvaluation evaluation =
                        consumerAvailabilityService.evaluateContract(tenantId, contractKey, from, to);
                String rawStatus = normalizeStatus(evaluation == null ? null : evaluation.status());
                boolean allowed = evaluation != null && evaluation.allowed();
                String status = allowed ? rawStatus : STATUS_FAIL;
                String reason = evaluation == null
                        ? "consumer availability evaluation is missing"
                        : defaultReason(evaluation.message(), status, allowed);
                evidence.add(new ProofEvidence("consumer_contract:" + contractKey, status, reason));
                contracts.add(new ConsumerContractProof(contractKey, status, allowed, reason, evaluation));
            } catch (RuntimeException e) {
                String reason = "consumer contract evaluation failed: " + message(e);
                evidence.add(new ProofEvidence("consumer_contract:" + contractKey, STATUS_FAIL, reason));
                contracts.add(new ConsumerContractProof(contractKey, STATUS_FAIL, false, reason, null));
            }
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param evidence evidence 参数，用于 privacyErasureBacklog 流程中的校验、计算或对象转换。
     * @return 返回 privacyErasureBacklog 流程生成的业务结果。
     */
    private CdpWarehousePrivacyErasureService.BacklogSummary privacyErasureBacklog(
            Long tenantId,
            List<ProofEvidence> evidence) {
        CdpWarehousePrivacyErasureService service =
                privacyErasureService == null ? null : privacyErasureService.getIfAvailable();
        if (service == null) {
            return null;
        }
        try {
            CdpWarehousePrivacyErasureService.BacklogSummary summary = service.summary(tenantId);
            String status = normalizeStatus(summary == null ? null : summary.status());
            evidence.add(new ProofEvidence("privacy_erasure_backlog", status,
                    summary == null ? "privacy erasure backlog summary is missing" : summary.reason()));
            return summary;
        } catch (RuntimeException e) {
            evidence.add(new ProofEvidence("privacy_erasure_backlog", STATUS_FAIL,
                    "privacy erasure backlog failed: " + message(e)));
            return null;
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param evidence evidence 参数，用于 enterpriseOlapReadiness 流程中的校验、计算或对象转换。
     * @return 返回 enterpriseOlapReadiness 流程生成的业务结果。
     */
    private CdpWarehouseEnterpriseOlapReadinessService.EnterpriseOlapReadiness enterpriseOlapReadiness(
            Long tenantId,
            List<ProofEvidence> evidence) {
        CdpWarehouseEnterpriseOlapReadinessService service =
                enterpriseOlapReadinessService == null ? null : enterpriseOlapReadinessService.getIfAvailable();
        if (service == null) {
            return null;
        }
        try {
            CdpWarehouseEnterpriseOlapReadinessService.EnterpriseOlapReadiness readiness =
                    service.evaluateFromProductionEvidence(tenantId, List.copyOf(evidence));
            String status = normalizeStatus(readiness == null ? null : readiness.status());
            evidence.add(new ProofEvidence("enterprise_olap_readiness", status,
                    readiness == null ? "enterprise OLAP readiness summary is missing" : readiness.summary()));
            return readiness;
        } catch (RuntimeException e) {
            evidence.add(new ProofEvidence("enterprise_olap_readiness", STATUS_FAIL,
                    "enterprise OLAP readiness failed: " + message(e)));
            return null;
        }
    }

    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param evidence evidence 参数，用于 addEnterpriseOlapOperationalEvidence 流程中的校验、计算或对象转换。
     */
    private void addEnterpriseOlapOperationalEvidence(Long tenantId, List<ProofEvidence> evidence) {
        CdpWarehouseEnterpriseOlapEvidenceService service =
                enterpriseOlapEvidenceService == null ? null : enterpriseOlapEvidenceService.getIfAvailable();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (service == null) {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        try {
            evidence.addAll(service.proofEvidence(tenantId));
        } catch (RuntimeException e) {
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            for (String key : List.of(
                    "doris_metrics",
                    "workload_isolation",
                    "backup_restore",
                    "compaction_health",
                    "ingestion_replay",
                    "runbook_drill")) {
                evidence.add(new ProofEvidence("enterprise_olap:" + key, STATUS_FAIL,
                        "enterprise OLAP evidence collection failed: " + message(e)));
            }
        }
    }

    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param evidence evidence 参数，用于 addEnterpriseOlapCollectionRunEvidence 流程中的校验、计算或对象转换。
     */
    private void addEnterpriseOlapCollectionRunEvidence(Long tenantId, List<ProofEvidence> evidence) {
        CdpWarehouseEnterpriseOlapEvidenceCollectionService service =
                enterpriseOlapEvidenceCollectionService == null
                        ? null
                        : enterpriseOlapEvidenceCollectionService.getIfAvailable();
        if (service == null) {
            return;
        }
        try {
            evidence.add(service.proofEvidence(tenantId));
        } catch (RuntimeException e) {
            evidence.add(new ProofEvidence("enterprise_olap:evidence_collection", STATUS_FAIL,
                    "enterprise OLAP evidence collection run check failed: " + message(e)));
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param contractKeys contract keys 参数，用于 safeContractKeys 流程中的校验、计算或对象转换。
     * @return 返回 safe contract keys 汇总后的集合、分页或映射视图。
     */
    private List<String> safeContractKeys(List<String> contractKeys) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (contractKeys == null) {
            return List.of();
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return contractKeys.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param allowed allowed 参数，用于 defaultReason 流程中的校验、计算或对象转换。
     * @return 返回 default reason 生成的文本或业务键。
     */
    private String defaultReason(String message, String status, boolean allowed) {
        if (message != null && !message.isBlank()) {
            return message;
        }
        return allowed
                ? "consumer availability " + status + " allowed"
                : "consumer availability " + status + " blocked";
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param statuses 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 worst status 生成的文本或业务键。
     */
    private String worstStatus(List<String> statuses) {
        if (statuses.stream().map(this::normalizeStatus).anyMatch(STATUS_FAIL::equals)) {
            return STATUS_FAIL;
        }
        if (statuses.stream().map(this::normalizeStatus).anyMatch(STATUS_WARN::equals)) {
            return STATUS_WARN;
        }
        return STATUS_PASS;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeStatus(String status) {
        String value = status == null ? STATUS_FAIL : status.trim().toUpperCase(Locale.ROOT);
        if (STATUS_PASS.equals(value) || STATUS_WARN.equals(value) || STATUS_FAIL.equals(value)) {
            return value;
        }
        return STATUS_FAIL;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param mode mode 参数，用于 normalizeMode 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeMode(String mode) {
        return mode == null || mode.isBlank() ? MODE_HYBRID : mode.trim().toUpperCase(Locale.ROOT);
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
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回 status reason 生成的文本或业务键。
     */
    private String statusReason(String status) {
        return "warehouse production evidence " + status.toLowerCase(Locale.ROOT);
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
     * ProductionReadinessProof 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record ProductionReadinessProof(
            Long tenantId,
            String status,
            LocalDateTime generatedAt,
            LocalDateTime windowStart,
            LocalDateTime windowEnd,
            String mode,
            List<ProofEvidence> evidence,
            CdpWarehouseReadinessService.ReadinessSummary readiness,
            CdpWarehouseAvailabilityService.AvailabilityDecision availability,
            List<ConsumerContractProof> contracts,
            CdpWarehousePrivacyErasureService.BacklogSummary privacyErasureBacklog,
            CdpWarehouseEnterpriseOlapReadinessService.EnterpriseOlapReadiness enterpriseOlapReadiness) {
        /**
         * 初始化 ProductionReadinessProof 实例。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @param status 业务状态，用于筛选或推进状态流转。
         * @param generatedAt 时间参数，用于计算窗口、过期或审计时间。
         * @param windowStart window start 参数，用于 ProductionReadinessProof 流程中的校验、计算或对象转换。
         * @param windowEnd window end 参数，用于 ProductionReadinessProof 流程中的校验、计算或对象转换。
         * @param mode mode 参数，用于 ProductionReadinessProof 流程中的校验、计算或对象转换。
         * @param evidence evidence 参数，用于 ProductionReadinessProof 流程中的校验、计算或对象转换。
         * @param readiness readiness 参数，用于 ProductionReadinessProof 流程中的校验、计算或对象转换。
         * @param availability availability 参数，用于 ProductionReadinessProof 流程中的校验、计算或对象转换。
         * @param contracts contracts 参数，用于 ProductionReadinessProof 流程中的校验、计算或对象转换。
         * @param privacyErasureBacklog privacy erasure backlog 参数，用于 ProductionReadinessProof 流程中的校验、计算或对象转换。
         */
        public ProductionReadinessProof(
                Long tenantId,
                String status,
                LocalDateTime generatedAt,
                LocalDateTime windowStart,
                LocalDateTime windowEnd,
                String mode,
                List<ProofEvidence> evidence,
                CdpWarehouseReadinessService.ReadinessSummary readiness,
                CdpWarehouseAvailabilityService.AvailabilityDecision availability,
                List<ConsumerContractProof> contracts,
                CdpWarehousePrivacyErasureService.BacklogSummary privacyErasureBacklog) {
            this(tenantId, status, generatedAt, windowStart, windowEnd, mode, evidence, readiness, availability,
                    contracts, privacyErasureBacklog, null);
        }

        public ProductionReadinessProof {
            evidence = evidence == null ? List.of() : List.copyOf(evidence);
            contracts = contracts == null ? List.of() : List.copyOf(contracts);
        }
    }

    /**
     * ProofEvidence 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record ProofEvidence(
            String key,
            String status,
            String reason) {
    }

    /**
     * ConsumerContractProof 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record ConsumerContractProof(
            String contractKey,
            String status,
            boolean allowed,
            String reason,
            CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityEvaluation evaluation) {
    }
}
