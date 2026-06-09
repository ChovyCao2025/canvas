package org.chovy.canvas.domain.warehouse;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
/**
 * CdpWarehousePhysicalE2eCertificationService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class CdpWarehousePhysicalE2eCertificationService {

    private static final String MODE_HYBRID = "HYBRID";
    private static final String STATUS_PASS = "PASS";
    private static final String STATUS_WARN = "WARN";
    private static final String STATUS_FAIL = "FAIL";
    private static final String INSPECTED_BY = "warehouse-e2e-certification";

    private final CdpWarehouseProductionReadinessProofService productionReadinessProofService;
    private final ObjectProvider<JdbcTemplate> dorisJdbcTemplate;
    private final CdpWarehouseTableGovernanceService tableGovernanceService;
    private final ObjectProvider<CdpWarehouseRealtimePipelineService> realtimePipelineService;
    private final ObjectProvider<CdpWarehouseRealtimeJobControlService> realtimeJobControlService;
    private final ObjectProvider<CdpWarehouseSyntheticDataPathProbeService> dataPathProbeService;

    /**
     * 初始化 CdpWarehousePhysicalE2eCertificationService 实例。
     *
     * @param productionReadinessProofService 依赖组件，用于完成数据访问或外部能力调用。
     * @param dorisJdbcTemplate doris jdbc template 参数，用于 CdpWarehousePhysicalE2eCertificationService 流程中的校验、计算或对象转换。
     * @param tableGovernanceService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehousePhysicalE2eCertificationService(
            CdpWarehouseProductionReadinessProofService productionReadinessProofService,
            @Qualifier("dorisJdbcTemplate") ObjectProvider<JdbcTemplate> dorisJdbcTemplate,
            CdpWarehouseTableGovernanceService tableGovernanceService) {
        this(productionReadinessProofService, dorisJdbcTemplate, tableGovernanceService, null, null);
    }

    /**
     * 初始化 CdpWarehousePhysicalE2eCertificationService 实例。
     *
     * @param productionReadinessProofService 依赖组件，用于完成数据访问或外部能力调用。
     * @param dorisJdbcTemplate doris jdbc template 参数，用于 CdpWarehousePhysicalE2eCertificationService 流程中的校验、计算或对象转换。
     * @param tableGovernanceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param realtimePipelineService 时间参数，用于计算窗口、过期或审计时间。
     * @param realtimeJobControlService 时间参数，用于计算窗口、过期或审计时间。
     */
    public CdpWarehousePhysicalE2eCertificationService(
            CdpWarehouseProductionReadinessProofService productionReadinessProofService,
            @Qualifier("dorisJdbcTemplate") ObjectProvider<JdbcTemplate> dorisJdbcTemplate,
            CdpWarehouseTableGovernanceService tableGovernanceService,
            ObjectProvider<CdpWarehouseRealtimePipelineService> realtimePipelineService,
            ObjectProvider<CdpWarehouseRealtimeJobControlService> realtimeJobControlService) {
        this(productionReadinessProofService, dorisJdbcTemplate, tableGovernanceService,
                realtimePipelineService, realtimeJobControlService, null);
    }

    @Autowired
    /**
     * 初始化 CdpWarehousePhysicalE2eCertificationService 实例。
     *
     * @param productionReadinessProofService 依赖组件，用于完成数据访问或外部能力调用。
     * @param dorisJdbcTemplate doris jdbc template 参数，用于 CdpWarehousePhysicalE2eCertificationService 流程中的校验、计算或对象转换。
     * @param tableGovernanceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param realtimePipelineService 时间参数，用于计算窗口、过期或审计时间。
     * @param realtimeJobControlService 时间参数，用于计算窗口、过期或审计时间。
     * @param dataPathProbeService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehousePhysicalE2eCertificationService(
            CdpWarehouseProductionReadinessProofService productionReadinessProofService,
            @Qualifier("dorisJdbcTemplate") ObjectProvider<JdbcTemplate> dorisJdbcTemplate,
            CdpWarehouseTableGovernanceService tableGovernanceService,
            ObjectProvider<CdpWarehouseRealtimePipelineService> realtimePipelineService,
            ObjectProvider<CdpWarehouseRealtimeJobControlService> realtimeJobControlService,
            ObjectProvider<CdpWarehouseSyntheticDataPathProbeService> dataPathProbeService) {
        this.productionReadinessProofService = productionReadinessProofService;
        this.dorisJdbcTemplate = dorisJdbcTemplate;
        this.tableGovernanceService = tableGovernanceService;
        this.realtimePipelineService = realtimePipelineService;
        this.realtimeJobControlService = realtimeJobControlService;
        this.dataPathProbeService = dataPathProbeService;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param from 时间或范围边界，用于限定统计窗口。
     * @param to 时间或范围边界，用于限定统计窗口。
     * @param mode mode 参数，用于 certify 流程中的校验、计算或对象转换。
     * @param contractKeys contract keys 参数，用于 certify 流程中的校验、计算或对象转换。
     * @param requirePhysical require physical 参数，用于 certify 流程中的校验、计算或对象转换。
     * @return 返回 certify 流程生成的业务结果。
     */
    public PhysicalE2eCertification certify(Long tenantId,
                                            LocalDateTime from,
                                            LocalDateTime to,
                                            String mode,
                                            List<String> contractKeys,
                                            boolean requirePhysical) {
        return certify(tenantId, from, to, mode, contractKeys, requirePhysical, false);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param from 时间或范围边界，用于限定统计窗口。
     * @param to 时间或范围边界，用于限定统计窗口。
     * @param mode mode 参数，用于 certify 流程中的校验、计算或对象转换。
     * @param contractKeys contract keys 参数，用于 certify 流程中的校验、计算或对象转换。
     * @param requirePhysical require physical 参数，用于 certify 流程中的校验、计算或对象转换。
     * @param requireRealtime 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 certify 流程生成的业务结果。
     */
    public PhysicalE2eCertification certify(Long tenantId,
                                            LocalDateTime from,
                                            LocalDateTime to,
                                            String mode,
                                            List<String> contractKeys,
                                            boolean requirePhysical,
                                            boolean requireRealtime) {
        return certify(tenantId, from, to, mode, contractKeys, requirePhysical, requireRealtime, false);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param from 时间或范围边界，用于限定统计窗口。
     * @param to 时间或范围边界，用于限定统计窗口。
     * @param mode mode 参数，用于 certify 流程中的校验、计算或对象转换。
     * @param contractKeys contract keys 参数，用于 certify 流程中的校验、计算或对象转换。
     * @param requirePhysical require physical 参数，用于 certify 流程中的校验、计算或对象转换。
     * @param requireRealtime 时间参数，用于计算窗口、过期或审计时间。
     * @param requireDataPathProof require data path proof 参数，用于 certify 流程中的校验、计算或对象转换。
     * @return 返回 certify 流程生成的业务结果。
     */
    public PhysicalE2eCertification certify(Long tenantId,
                                            LocalDateTime from,
                                            LocalDateTime to,
                                            String mode,
                                            List<String> contractKeys,
                                            boolean requirePhysical,
                                            boolean requireRealtime,
                                            boolean requireDataPathProof) {
        // 准备本次处理所需的上下文和中间变量。
        Long scopedTenantId = normalizeTenant(tenantId);
        String normalizedMode = normalizeMode(mode);
        List<String> safeContractKeys = safeContractKeys(contractKeys);
        List<CertificationEvidence> evidence = new ArrayList<>();
        CdpWarehouseProductionReadinessProofService.ProductionReadinessProof productionReadiness =
                productionReadiness(scopedTenantId, from, to, normalizedMode, safeContractKeys, evidence);

        DorisProbeResult dorisProbe = probeDoris(requirePhysical, evidence);
        CdpWarehouseTableGovernanceService.InspectionSummary liveTableInspection = dorisProbe.connected()
                ? liveTableInspection(scopedTenantId, requirePhysical, evidence)
                : skippedLiveTableInspection(requirePhysical, evidence);
        CdpWarehouseRealtimePipelineService.PipelineStatusSummary realtimePipelineStatus =
                realtimePipelineStatus(scopedTenantId, requireRealtime, evidence);
        CdpWarehouseRealtimeJobControlService.JobStatusSummary realtimeJobStatus =
                realtimeJobStatus(scopedTenantId, requireRealtime, evidence);
        CdpWarehouseSyntheticDataPathProbeService.ProbeRunView dataPathProof =
                dataPathProof(scopedTenantId, requireRealtime, requireDataPathProof, evidence);

        LocalDateTime generatedAt = LocalDateTime.now();
        LocalDateTime windowEnd = productionReadiness == null
                ? (to == null ? generatedAt : to)
                : productionReadiness.windowEnd();
        LocalDateTime windowStart = productionReadiness == null
                ? (from == null ? windowEnd.minusHours(1) : from)
                : productionReadiness.windowStart();
        String proofMode = productionReadiness == null ? normalizedMode : productionReadiness.mode();

        // 汇总前面计算出的状态和明细，返回给调用方。
        return new PhysicalE2eCertification(
                scopedTenantId,
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                worstStatus(evidence.stream().map(CertificationEvidence::status).toList()),
                generatedAt,
                windowStart,
                windowEnd,
                proofMode,
                requirePhysical,
                requireRealtime,
                requireDataPathProof,
                List.copyOf(evidence),
                productionReadiness,
                liveTableInspection,
                realtimePipelineStatus,
                realtimeJobStatus,
                dataPathProof);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param from 时间或范围边界，用于限定统计窗口。
     * @param to 时间或范围边界，用于限定统计窗口。
     * @param mode mode 参数，用于 productionReadiness 流程中的校验、计算或对象转换。
     * @param contractKeys contract keys 参数，用于 productionReadiness 流程中的校验、计算或对象转换。
     * @param evidence evidence 参数，用于 productionReadiness 流程中的校验、计算或对象转换。
     * @return 返回 productionReadiness 流程生成的业务结果。
     */
    private CdpWarehouseProductionReadinessProofService.ProductionReadinessProof productionReadiness(
            Long tenantId,
            LocalDateTime from,
            LocalDateTime to,
            String mode,
            List<String> contractKeys,
            List<CertificationEvidence> evidence) {
        try {
            CdpWarehouseProductionReadinessProofService.ProductionReadinessProof proof =
                    productionReadinessProofService.proof(tenantId, from, to, mode, contractKeys);
            String status = normalizeStatus(proof == null ? null : proof.status());
            evidence.add(new CertificationEvidence("production_readiness", status,
                    proof == null ? "production readiness proof is missing" : statusReason("production readiness", status)));
            return proof;
        } catch (RuntimeException e) {
            evidence.add(new CertificationEvidence("production_readiness", STATUS_FAIL,
                    "production readiness proof failed: " + message(e)));
            return null;
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param requirePhysical require physical 参数，用于 probeDoris 流程中的校验、计算或对象转换。
     * @param evidence evidence 参数，用于 probeDoris 流程中的校验、计算或对象转换。
     * @return 返回 probeDoris 流程生成的业务结果。
     */
    private DorisProbeResult probeDoris(boolean requirePhysical, List<CertificationEvidence> evidence) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        JdbcTemplate jdbcTemplate = dorisJdbcTemplate == null ? null : dorisJdbcTemplate.getIfAvailable();
        String missingStatus = missingPhysicalStatus(requirePhysical);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (jdbcTemplate == null) {
            evidence.add(new CertificationEvidence("doris_jdbc_connectivity", missingStatus,
                    "Doris JDBC is not configured"));
            return new DorisProbeResult(false);
        }
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (Integer.valueOf(1).equals(result)) {
                evidence.add(new CertificationEvidence("doris_jdbc_connectivity", STATUS_PASS,
                        "Doris JDBC SELECT 1 passed"));
                return new DorisProbeResult(true);
            }
            evidence.add(new CertificationEvidence("doris_jdbc_connectivity", STATUS_FAIL,
                    "Doris JDBC SELECT 1 returned " + result));
            return new DorisProbeResult(false);
        } catch (RuntimeException e) {
            evidence.add(new CertificationEvidence("doris_jdbc_connectivity", STATUS_FAIL,
                    "Doris JDBC SELECT 1 failed: " + message(e)));
            // 汇总前面计算出的状态和明细，返回给调用方。
            return new DorisProbeResult(false);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param requirePhysical require physical 参数，用于 liveTableInspection 流程中的校验、计算或对象转换。
     * @param evidence evidence 参数，用于 liveTableInspection 流程中的校验、计算或对象转换。
     * @return 返回 liveTableInspection 流程生成的业务结果。
     */
    private CdpWarehouseTableGovernanceService.InspectionSummary liveTableInspection(
            Long tenantId,
            boolean requirePhysical,
            List<CertificationEvidence> evidence) {
        if (tableGovernanceService == null) {
            evidence.add(new CertificationEvidence("live_table_contracts", missingPhysicalStatus(requirePhysical),
                    "table governance service is not configured"));
            return null;
        }
        try {
            CdpWarehouseTableGovernanceService.InspectionSummary summary =
                    tableGovernanceService.inspectLiveAll(tenantId, INSPECTED_BY);
            String status = liveInspectionStatus(summary, requirePhysical);
            evidence.add(new CertificationEvidence("live_table_contracts", status, liveInspectionReason(summary, status)));
            return summary;
        } catch (RuntimeException e) {
            evidence.add(new CertificationEvidence("live_table_contracts", missingPhysicalStatus(requirePhysical),
                    "live table inspection failed: " + message(e)));
            return null;
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param requirePhysical require physical 参数，用于 skippedLiveTableInspection 流程中的校验、计算或对象转换。
     * @param evidence evidence 参数，用于 skippedLiveTableInspection 流程中的校验、计算或对象转换。
     * @return 返回 skippedLiveTableInspection 流程生成的业务结果。
     */
    private CdpWarehouseTableGovernanceService.InspectionSummary skippedLiveTableInspection(
            boolean requirePhysical,
            List<CertificationEvidence> evidence) {
        evidence.add(new CertificationEvidence("live_table_contracts", missingPhysicalStatus(requirePhysical),
                "live table inspection skipped because Doris connectivity failed"));
        return null;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param requireRealtime 时间参数，用于计算窗口、过期或审计时间。
     * @param evidence evidence 参数，用于 realtimePipelineStatus 流程中的校验、计算或对象转换。
     * @return 返回 realtimePipelineStatus 流程生成的业务结果。
     */
    private CdpWarehouseRealtimePipelineService.PipelineStatusSummary realtimePipelineStatus(
            Long tenantId,
            boolean requireRealtime,
            List<CertificationEvidence> evidence) {
        CdpWarehouseRealtimePipelineService service =
                realtimePipelineService == null ? null : realtimePipelineService.getIfAvailable();
        if (service == null) {
            evidence.add(new CertificationEvidence("realtime_pipeline_status", missingRealtimeStatus(requireRealtime),
                    "realtime pipeline service is not configured"));
            return null;
        }
        try {
            CdpWarehouseRealtimePipelineService.PipelineStatusSummary summary = service.status(tenantId, 5);
            String status = realtimePipelineStatus(summary, requireRealtime);
            evidence.add(new CertificationEvidence("realtime_pipeline_status", status,
                    realtimePipelineReason(summary, status)));
            return summary;
        } catch (RuntimeException e) {
            evidence.add(new CertificationEvidence("realtime_pipeline_status", missingRealtimeStatus(requireRealtime),
                    "realtime pipeline status failed: " + message(e)));
            return null;
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param requireRealtime 时间参数，用于计算窗口、过期或审计时间。
     * @param evidence evidence 参数，用于 realtimeJobStatus 流程中的校验、计算或对象转换。
     * @return 返回 realtimeJobStatus 流程生成的业务结果。
     */
    private CdpWarehouseRealtimeJobControlService.JobStatusSummary realtimeJobStatus(
            Long tenantId,
            boolean requireRealtime,
            List<CertificationEvidence> evidence) {
        CdpWarehouseRealtimeJobControlService service =
                realtimeJobControlService == null ? null : realtimeJobControlService.getIfAvailable();
        if (service == null) {
            evidence.add(new CertificationEvidence("realtime_job_status", missingRealtimeStatus(requireRealtime),
                    "realtime job control service is not configured"));
            return null;
        }
        try {
            CdpWarehouseRealtimeJobControlService.JobStatusSummary summary = service.status(tenantId, null, 300, 100);
            String status = realtimeJobStatus(summary, requireRealtime);
            evidence.add(new CertificationEvidence("realtime_job_status", status,
                    realtimeJobReason(summary, status)));
            return summary;
        } catch (RuntimeException e) {
            evidence.add(new CertificationEvidence("realtime_job_status", missingRealtimeStatus(requireRealtime),
                    "realtime job status failed: " + message(e)));
            return null;
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param requireRealtime 时间参数，用于计算窗口、过期或审计时间。
     * @param requireDataPathProof require data path proof 参数，用于 dataPathProof 流程中的校验、计算或对象转换。
     * @param evidence evidence 参数，用于 dataPathProof 流程中的校验、计算或对象转换。
     * @return 返回 dataPathProof 流程生成的业务结果。
     */
    private CdpWarehouseSyntheticDataPathProbeService.ProbeRunView dataPathProof(
            Long tenantId,
            boolean requireRealtime,
            boolean requireDataPathProof,
            List<CertificationEvidence> evidence) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!requireDataPathProof) {
            return null;
        }
        CdpWarehouseSyntheticDataPathProbeService service =
                dataPathProbeService == null ? null : dataPathProbeService.getIfAvailable();
        if (service == null) {
            evidence.add(new CertificationEvidence("synthetic_ods_data_path", STATUS_FAIL,
                    "synthetic ODS data-path proof service is not configured"));
            return null;
        }
        try {
            CdpWarehouseSyntheticDataPathProbeService.ProbeRunView proof = service.run(tenantId,
                    new CdpWarehouseSyntheticDataPathProbeService.RunCommand(
                            "e2e-certification", null, true, 3, 100,
                            requireRealtime ? "MYSQL_CDC" : "DIRECT_SINK"));
            String status = dataPathProofStatus(proof);
            evidence.add(new CertificationEvidence("synthetic_ods_data_path", status,
                    dataPathProofReason(proof, status)));
            return proof;
        } catch (RuntimeException e) {
            evidence.add(new CertificationEvidence("synthetic_ods_data_path", STATUS_FAIL,
                    "synthetic ODS data-path proof failed: " + message(e)));
            // 汇总前面计算出的状态和明细，返回给调用方。
            return null;
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param summary summary 参数，用于 liveInspectionStatus 流程中的校验、计算或对象转换。
     * @param requirePhysical require physical 参数，用于 liveInspectionStatus 流程中的校验、计算或对象转换。
     * @return 返回 live inspection status 生成的文本或业务键。
     */
    private String liveInspectionStatus(CdpWarehouseTableGovernanceService.InspectionSummary summary,
                                        boolean requirePhysical) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (summary == null || summary.total() <= 0) {
            return missingPhysicalStatus(requirePhysical);
        }
        if (summary.failed() > 0) {
            return STATUS_FAIL;
        }
        if (summary.warned() > 0) {
            return STATUS_WARN;
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return STATUS_PASS;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param summary summary 参数，用于 liveInspectionReason 流程中的校验、计算或对象转换。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回 live inspection reason 生成的文本或业务键。
     */
    private String liveInspectionReason(CdpWarehouseTableGovernanceService.InspectionSummary summary,
                                        String status) {
        if (summary == null) {
            return "live table inspection summary is missing";
        }
        if (summary.total() <= 0) {
            return "no active live table contracts inspected";
        }
        return "live table inspection " + status.toLowerCase(Locale.ROOT)
                + " total=" + summary.total()
                + " passed=" + summary.passed()
                + " warned=" + summary.warned()
                + " failed=" + summary.failed();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param requirePhysical require physical 参数，用于 missingPhysicalStatus 流程中的校验、计算或对象转换。
     * @return 返回 missing physical status 生成的文本或业务键。
     */
    private String missingPhysicalStatus(boolean requirePhysical) {
        return requirePhysical ? STATUS_FAIL : STATUS_WARN;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param requireRealtime 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 missing realtime status 生成的文本或业务键。
     */
    private String missingRealtimeStatus(boolean requireRealtime) {
        return requireRealtime ? STATUS_FAIL : STATUS_WARN;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param summary summary 参数，用于 realtimePipelineStatus 流程中的校验、计算或对象转换。
     * @param requireRealtime 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 realtime pipeline status 生成的文本或业务键。
     */
    private String realtimePipelineStatus(CdpWarehouseRealtimePipelineService.PipelineStatusSummary summary,
                                          boolean requireRealtime) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (summary == null || summary.total() <= 0) {
            return missingRealtimeStatus(requireRealtime);
        }
        if (summary.failed() > 0) {
            return STATUS_FAIL;
        }
        if (summary.warned() > 0) {
            return STATUS_WARN;
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return STATUS_PASS;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param summary summary 参数，用于 realtimePipelineReason 流程中的校验、计算或对象转换。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回 realtime pipeline reason 生成的文本或业务键。
     */
    private String realtimePipelineReason(CdpWarehouseRealtimePipelineService.PipelineStatusSummary summary,
                                          String status) {
        if (summary == null || summary.total() <= 0) {
            return "no active realtime pipelines reported";
        }
        return "realtime pipeline status " + status.toLowerCase(Locale.ROOT)
                + " total=" + summary.total()
                + " passed=" + summary.passed()
                + " warned=" + summary.warned()
                + " failed=" + summary.failed();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param summary summary 参数，用于 realtimeJobStatus 流程中的校验、计算或对象转换。
     * @param requireRealtime 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 realtime job status 生成的文本或业务键。
     */
    private String realtimeJobStatus(CdpWarehouseRealtimeJobControlService.JobStatusSummary summary,
                                     boolean requireRealtime) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (summary == null || summary.total() <= 0) {
            return missingRealtimeStatus(requireRealtime);
        }
        if (summary.failed() > 0) {
            return STATUS_FAIL;
        }
        if (summary.warned() > 0) {
            return STATUS_WARN;
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return STATUS_PASS;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param summary summary 参数，用于 realtimeJobReason 流程中的校验、计算或对象转换。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回 realtime job reason 生成的文本或业务键。
     */
    private String realtimeJobReason(CdpWarehouseRealtimeJobControlService.JobStatusSummary summary,
                                     String status) {
        if (summary == null || summary.total() <= 0) {
            return "no realtime jobs reported";
        }
        return "realtime job status " + status.toLowerCase(Locale.ROOT)
                + " total=" + summary.total()
                + " passed=" + summary.passed()
                + " warned=" + summary.warned()
                + " failed=" + summary.failed();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param proof proof 参数，用于 dataPathProofStatus 流程中的校验、计算或对象转换。
     * @return 返回 data path proof status 生成的文本或业务键。
     */
    private String dataPathProofStatus(CdpWarehouseSyntheticDataPathProbeService.ProbeRunView proof) {
        if (proof == null) {
            return STATUS_FAIL;
        }
        return STATUS_PASS.equals(normalizeStatus(proof.status())) ? STATUS_PASS : STATUS_FAIL;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param proof proof 参数，用于 dataPathProofReason 流程中的校验、计算或对象转换。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回 data path proof reason 生成的文本或业务键。
     */
    private String dataPathProofReason(CdpWarehouseSyntheticDataPathProbeService.ProbeRunView proof,
                                       String status) {
        if (proof == null) {
            return "synthetic ODS data-path proof is missing";
        }
        return "synthetic ODS data-path proof " + status.toLowerCase(Locale.ROOT)
                + " sourceMode=" + proof.sourceMode()
                + " sourceStatus=" + proof.sourceStatus()
                + " proofStatus=" + proof.status()
                + " sinkStatus=" + proof.sinkStatus()
                + " odsStatus=" + proof.odsStatus()
                + " odsRows=" + proof.odsRowCount();
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
     * @param subject 待处理业务值，用于规则计算、转换或外部调用。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回 status reason 生成的文本或业务键。
     */
    private String statusReason(String subject, String status) {
        return subject + " " + status.toLowerCase(Locale.ROOT);
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
     * DorisProbeResult 承载对应领域的业务规则、流程编排和结果转换。
     */
    private record DorisProbeResult(boolean connected) {
    }

    /**
     * PhysicalE2eCertification 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record PhysicalE2eCertification(
            Long tenantId,
            String status,
            LocalDateTime generatedAt,
            LocalDateTime windowStart,
            LocalDateTime windowEnd,
            String mode,
            boolean requirePhysical,
            boolean requireRealtime,
            boolean requireDataPathProof,
            List<CertificationEvidence> evidence,
            CdpWarehouseProductionReadinessProofService.ProductionReadinessProof productionReadiness,
            CdpWarehouseTableGovernanceService.InspectionSummary liveTableInspection,
            CdpWarehouseRealtimePipelineService.PipelineStatusSummary realtimePipelineStatus,
            CdpWarehouseRealtimeJobControlService.JobStatusSummary realtimeJobStatus,
            CdpWarehouseSyntheticDataPathProbeService.ProbeRunView dataPathProof) {
        public PhysicalE2eCertification {
            evidence = evidence == null ? List.of() : List.copyOf(evidence);
        }
    }

    /**
     * CertificationEvidence 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record CertificationEvidence(
            String key,
            String status,
            String reason) {
    }
}
