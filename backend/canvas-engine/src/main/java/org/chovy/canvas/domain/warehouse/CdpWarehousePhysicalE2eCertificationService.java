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

    public CdpWarehousePhysicalE2eCertificationService(
            CdpWarehouseProductionReadinessProofService productionReadinessProofService,
            @Qualifier("dorisJdbcTemplate") ObjectProvider<JdbcTemplate> dorisJdbcTemplate,
            CdpWarehouseTableGovernanceService tableGovernanceService) {
        this(productionReadinessProofService, dorisJdbcTemplate, tableGovernanceService, null, null);
    }

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

    public PhysicalE2eCertification certify(Long tenantId,
                                            LocalDateTime from,
                                            LocalDateTime to,
                                            String mode,
                                            List<String> contractKeys,
                                            boolean requirePhysical) {
        return certify(tenantId, from, to, mode, contractKeys, requirePhysical, false);
    }

    public PhysicalE2eCertification certify(Long tenantId,
                                            LocalDateTime from,
                                            LocalDateTime to,
                                            String mode,
                                            List<String> contractKeys,
                                            boolean requirePhysical,
                                            boolean requireRealtime) {
        return certify(tenantId, from, to, mode, contractKeys, requirePhysical, requireRealtime, false);
    }

    public PhysicalE2eCertification certify(Long tenantId,
                                            LocalDateTime from,
                                            LocalDateTime to,
                                            String mode,
                                            List<String> contractKeys,
                                            boolean requirePhysical,
                                            boolean requireRealtime,
                                            boolean requireDataPathProof) {
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
                dataPathProof(scopedTenantId, requireDataPathProof, evidence);

        LocalDateTime generatedAt = LocalDateTime.now();
        LocalDateTime windowEnd = productionReadiness == null
                ? (to == null ? generatedAt : to)
                : productionReadiness.windowEnd();
        LocalDateTime windowStart = productionReadiness == null
                ? (from == null ? windowEnd.minusHours(1) : from)
                : productionReadiness.windowStart();
        String proofMode = productionReadiness == null ? normalizedMode : productionReadiness.mode();

        return new PhysicalE2eCertification(
                scopedTenantId,
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

    private DorisProbeResult probeDoris(boolean requirePhysical, List<CertificationEvidence> evidence) {
        JdbcTemplate jdbcTemplate = dorisJdbcTemplate == null ? null : dorisJdbcTemplate.getIfAvailable();
        String missingStatus = missingPhysicalStatus(requirePhysical);
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
            return new DorisProbeResult(false);
        }
    }

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

    private CdpWarehouseTableGovernanceService.InspectionSummary skippedLiveTableInspection(
            boolean requirePhysical,
            List<CertificationEvidence> evidence) {
        evidence.add(new CertificationEvidence("live_table_contracts", missingPhysicalStatus(requirePhysical),
                "live table inspection skipped because Doris connectivity failed"));
        return null;
    }

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

    private CdpWarehouseSyntheticDataPathProbeService.ProbeRunView dataPathProof(
            Long tenantId,
            boolean requireDataPathProof,
            List<CertificationEvidence> evidence) {
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
                            "e2e-certification", null, true, 3, 100));
            String status = dataPathProofStatus(proof);
            evidence.add(new CertificationEvidence("synthetic_ods_data_path", status,
                    dataPathProofReason(proof, status)));
            return proof;
        } catch (RuntimeException e) {
            evidence.add(new CertificationEvidence("synthetic_ods_data_path", STATUS_FAIL,
                    "synthetic ODS data-path proof failed: " + message(e)));
            return null;
        }
    }

    private String liveInspectionStatus(CdpWarehouseTableGovernanceService.InspectionSummary summary,
                                        boolean requirePhysical) {
        if (summary == null || summary.total() <= 0) {
            return missingPhysicalStatus(requirePhysical);
        }
        if (summary.failed() > 0) {
            return STATUS_FAIL;
        }
        if (summary.warned() > 0) {
            return STATUS_WARN;
        }
        return STATUS_PASS;
    }

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

    private String missingPhysicalStatus(boolean requirePhysical) {
        return requirePhysical ? STATUS_FAIL : STATUS_WARN;
    }

    private String missingRealtimeStatus(boolean requireRealtime) {
        return requireRealtime ? STATUS_FAIL : STATUS_WARN;
    }

    private String realtimePipelineStatus(CdpWarehouseRealtimePipelineService.PipelineStatusSummary summary,
                                          boolean requireRealtime) {
        if (summary == null || summary.total() <= 0) {
            return missingRealtimeStatus(requireRealtime);
        }
        if (summary.failed() > 0) {
            return STATUS_FAIL;
        }
        if (summary.warned() > 0) {
            return STATUS_WARN;
        }
        return STATUS_PASS;
    }

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

    private String realtimeJobStatus(CdpWarehouseRealtimeJobControlService.JobStatusSummary summary,
                                     boolean requireRealtime) {
        if (summary == null || summary.total() <= 0) {
            return missingRealtimeStatus(requireRealtime);
        }
        if (summary.failed() > 0) {
            return STATUS_FAIL;
        }
        if (summary.warned() > 0) {
            return STATUS_WARN;
        }
        return STATUS_PASS;
    }

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

    private String dataPathProofStatus(CdpWarehouseSyntheticDataPathProbeService.ProbeRunView proof) {
        if (proof == null) {
            return STATUS_FAIL;
        }
        return STATUS_PASS.equals(normalizeStatus(proof.status())) ? STATUS_PASS : STATUS_FAIL;
    }

    private String dataPathProofReason(CdpWarehouseSyntheticDataPathProbeService.ProbeRunView proof,
                                       String status) {
        if (proof == null) {
            return "synthetic ODS data-path proof is missing";
        }
        return "synthetic ODS data-path proof " + status.toLowerCase(Locale.ROOT)
                + " proofStatus=" + proof.status()
                + " sinkStatus=" + proof.sinkStatus()
                + " odsStatus=" + proof.odsStatus()
                + " odsRows=" + proof.odsRowCount();
    }

    private String worstStatus(List<String> statuses) {
        if (statuses.stream().map(this::normalizeStatus).anyMatch(STATUS_FAIL::equals)) {
            return STATUS_FAIL;
        }
        if (statuses.stream().map(this::normalizeStatus).anyMatch(STATUS_WARN::equals)) {
            return STATUS_WARN;
        }
        return STATUS_PASS;
    }

    private String normalizeStatus(String status) {
        String value = status == null ? STATUS_FAIL : status.trim().toUpperCase(Locale.ROOT);
        if (STATUS_PASS.equals(value) || STATUS_WARN.equals(value) || STATUS_FAIL.equals(value)) {
            return value;
        }
        return STATUS_FAIL;
    }

    private String normalizeMode(String mode) {
        return mode == null || mode.isBlank() ? MODE_HYBRID : mode.trim().toUpperCase(Locale.ROOT);
    }

    private List<String> safeContractKeys(List<String> contractKeys) {
        if (contractKeys == null) {
            return List.of();
        }
        return contractKeys.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String statusReason(String subject, String status) {
        return subject + " " + status.toLowerCase(Locale.ROOT);
    }

    private String message(RuntimeException e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    private record DorisProbeResult(boolean connected) {
    }

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

    public record CertificationEvidence(
            String key,
            String status,
            String reason) {
    }
}
