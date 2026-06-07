package org.chovy.canvas.domain.warehouse;

import org.chovy.canvas.domain.analytics.AudienceMaterializationOperationsService;
import org.chovy.canvas.domain.bi.query.BiDatasourceHealth;
import org.chovy.canvas.domain.bi.query.BiDatasourceHealthProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class CdpWarehouseReadinessService {

    private static final String PASS = "PASS";
    private static final String WARN = "WARN";
    private static final String FAIL = "FAIL";
    private static final int DEFAULT_RECENT_LIMIT = 20;

    private final CdpWarehouseOperationsService operationsService;
    private final CdpWarehouseRealtimePipelineService realtimePipelineService;
    private final CdpWarehouseRealtimeJobControlService realtimeJobControlService;
    private final CdpWarehouseIncidentService incidentService;
    private final AudienceMaterializationOperationsService audienceMaterializationOperationsService;
    private final BiDatasourceHealthProvider biDatasourceHealthProvider;
    private final CdpWarehouseSloPolicyService sloPolicyService;

    @Autowired
    public CdpWarehouseReadinessService(
            CdpWarehouseOperationsService operationsService,
            CdpWarehouseRealtimePipelineService realtimePipelineService,
            ObjectProvider<CdpWarehouseRealtimeJobControlService> realtimeJobControlServiceProvider,
            CdpWarehouseIncidentService incidentService,
            AudienceMaterializationOperationsService audienceMaterializationOperationsService,
            ObjectProvider<BiDatasourceHealthProvider> biDatasourceHealthProviderProvider,
            ObjectProvider<CdpWarehouseSloPolicyService> sloPolicyServiceProvider) {
        this(operationsService,
                realtimePipelineService,
                realtimeJobControlServiceProvider == null
                        ? null
                        : realtimeJobControlServiceProvider.getIfAvailable(),
                incidentService,
                audienceMaterializationOperationsService,
                biDatasourceHealthProviderProvider == null
                        ? BiDatasourceHealthProvider.empty()
                        : biDatasourceHealthProviderProvider.getIfAvailable(BiDatasourceHealthProvider::empty),
                sloPolicyServiceProvider == null
                        ? null
                        : sloPolicyServiceProvider.getIfAvailable());
    }

    CdpWarehouseReadinessService(
            CdpWarehouseOperationsService operationsService,
            CdpWarehouseRealtimePipelineService realtimePipelineService,
            CdpWarehouseIncidentService incidentService,
            AudienceMaterializationOperationsService audienceMaterializationOperationsService,
            BiDatasourceHealthProvider biDatasourceHealthProvider) {
        this(operationsService, realtimePipelineService, null, incidentService,
                audienceMaterializationOperationsService, biDatasourceHealthProvider, null);
    }

    CdpWarehouseReadinessService(
            CdpWarehouseOperationsService operationsService,
            CdpWarehouseRealtimePipelineService realtimePipelineService,
            CdpWarehouseIncidentService incidentService,
            AudienceMaterializationOperationsService audienceMaterializationOperationsService,
            BiDatasourceHealthProvider biDatasourceHealthProvider,
            CdpWarehouseSloPolicyService sloPolicyService) {
        this(operationsService, realtimePipelineService, null, incidentService,
                audienceMaterializationOperationsService, biDatasourceHealthProvider, sloPolicyService);
    }

    CdpWarehouseReadinessService(
            CdpWarehouseOperationsService operationsService,
            CdpWarehouseRealtimePipelineService realtimePipelineService,
            CdpWarehouseRealtimeJobControlService realtimeJobControlService,
            CdpWarehouseIncidentService incidentService,
            AudienceMaterializationOperationsService audienceMaterializationOperationsService,
            BiDatasourceHealthProvider biDatasourceHealthProvider,
            CdpWarehouseSloPolicyService sloPolicyService) {
        this.operationsService = operationsService;
        this.realtimePipelineService = realtimePipelineService;
        this.realtimeJobControlService = realtimeJobControlService;
        this.incidentService = incidentService;
        this.audienceMaterializationOperationsService = audienceMaterializationOperationsService;
        this.biDatasourceHealthProvider = biDatasourceHealthProvider == null
                ? BiDatasourceHealthProvider.empty()
                : biDatasourceHealthProvider;
        this.sloPolicyService = sloPolicyService;
    }

    public ReadinessSummary readiness(Long tenantId) {
        Long scopedTenantId = normalizeTenant(tenantId);
        LocalDateTime generatedAt = LocalDateTime.now();
        CdpWarehouseSloPolicyService.SloPolicyView sloPolicy = effectivePolicy(scopedTenantId);
        OfflineReadiness offline = offline(scopedTenantId, generatedAt, sloPolicy);
        RealtimeReadiness realtime = realtime(scopedTenantId);
        IncidentReadiness incidents = incidents(scopedTenantId);
        BiReadiness bi = bi();
        AudienceMaterializationReadiness audienceMaterialization =
                audienceMaterialization(scopedTenantId, generatedAt, sloPolicy);

        List<ReadinessSection> sections = List.of(
                new ReadinessSection("offline_sync", offline.status(), offline.reason()),
                new ReadinessSection("realtime_pipelines", realtime.status(), realtime.reason()),
                new ReadinessSection("incidents", incidents.status(), incidents.reason()),
                new ReadinessSection("bi_datasources", bi.status(), bi.reason()),
                new ReadinessSection("audience_materialization", audienceMaterialization.status(),
                        audienceMaterialization.reason()));
        return new ReadinessSummary(
                scopedTenantId,
                overallStatus(sections),
                generatedAt,
                sections,
                offline,
                realtime,
                incidents,
                bi,
                audienceMaterialization);
    }

    private OfflineReadiness offline(Long tenantId,
                                     LocalDateTime now,
                                     CdpWarehouseSloPolicyService.SloPolicyView sloPolicy) {
        try {
            CdpWarehouseOperationsService.WarehouseStatus status =
                    operationsService.status(tenantId, DEFAULT_RECENT_LIMIT);
            List<CdpWarehouseOperationsService.RunRow> runs =
                    status.recentRuns() == null ? List.of() : status.recentRuns();
            long failed = runs.stream().filter(run -> isFailed(run.status())).count();
            long running = runs.stream().filter(run -> "RUNNING".equalsIgnoreCase(nullToEmpty(run.status()))).count();
            int watermarkCount = status.watermarks() == null ? 0 : status.watermarks().size();
            Long latestRunAgeMinutes = ageMinutes(latestRunAt(runs), now);
            Long latestWatermarkLagMinutes = ageMinutes(latestWatermarkAt(status.watermarks()), now);
            String sectionStatus;
            String reason;
            if (failed > 0) {
                sectionStatus = FAIL;
                reason = failed + " recent offline sync run(s) failed";
            } else if (runs.isEmpty()) {
                sectionStatus = WARN;
                reason = "no recent offline sync runs";
            } else if (watermarkCount == 0) {
                sectionStatus = WARN;
                reason = "offline sync watermarks are missing";
            } else if (thresholdReached(latestRunAgeMinutes, sloPolicy.offlineFailRunGapMinutes())) {
                sectionStatus = FAIL;
                reason = "latest offline sync run age " + latestRunAgeMinutes
                        + "m reaches fail threshold " + sloPolicy.offlineFailRunGapMinutes() + "m";
            } else if (thresholdReached(latestWatermarkLagMinutes, sloPolicy.offlineFailWatermarkLagMinutes())) {
                sectionStatus = FAIL;
                reason = "latest offline watermark lag " + latestWatermarkLagMinutes
                        + "m reaches fail threshold " + sloPolicy.offlineFailWatermarkLagMinutes() + "m";
            } else if (thresholdReached(latestRunAgeMinutes, sloPolicy.offlineWarnRunGapMinutes())) {
                sectionStatus = WARN;
                reason = "latest offline sync run age " + latestRunAgeMinutes
                        + "m reaches warn threshold " + sloPolicy.offlineWarnRunGapMinutes() + "m";
            } else if (thresholdReached(latestWatermarkLagMinutes, sloPolicy.offlineWarnWatermarkLagMinutes())) {
                sectionStatus = WARN;
                reason = "latest offline watermark lag " + latestWatermarkLagMinutes
                        + "m reaches warn threshold " + sloPolicy.offlineWarnWatermarkLagMinutes() + "m";
            } else {
                sectionStatus = PASS;
                reason = "offline sync runs and watermarks are present";
            }
            return new OfflineReadiness(sectionStatus, reason, runs.size(), failed, running, watermarkCount);
        } catch (RuntimeException e) {
            return new OfflineReadiness(FAIL, "offline sync status failed: " + e.getMessage(),
                    0, 0, 0, 0);
        }
    }

    private RealtimeReadiness realtime(Long tenantId) {
        try {
            CdpWarehouseRealtimePipelineService.PipelineStatusSummary status =
                    realtimePipelineService.status(tenantId, 3);
            CdpWarehouseRealtimeJobControlService.JobStatusSummary jobStatus =
                    realtimeJobControlService == null
                            ? new CdpWarehouseRealtimeJobControlService.JobStatusSummary(
                            tenantId, 0, 0, 0, 0, List.of())
                            : realtimeJobControlService.status(tenantId, null, 300, 50);
            long failed = status.failed() + jobStatus.failed();
            long warned = status.warned() + jobStatus.warned();
            int total = status.total() + jobStatus.total();
            String sectionStatus;
            String reason;
            if (failed > 0) {
                sectionStatus = FAIL;
                reason = failed + " realtime pipeline/job(s) failed";
            } else if (warned > 0) {
                sectionStatus = WARN;
                reason = warned + " realtime pipeline/job(s) warning";
            } else if (total == 0) {
                sectionStatus = WARN;
                reason = "no active realtime warehouse pipeline or job evidence";
            } else {
                sectionStatus = PASS;
                reason = "realtime pipelines and jobs are healthy";
            }
            return new RealtimeReadiness(sectionStatus, reason,
                    status.total(), status.passed() + jobStatus.passed(), warned, failed, jobStatus.total());
        } catch (RuntimeException e) {
            return new RealtimeReadiness(FAIL, "realtime status failed: " + e.getMessage(),
                    0, 0, 0, 0, 0);
        }
    }

    private IncidentReadiness incidents(Long tenantId) {
        try {
            List<CdpWarehouseIncidentService.IncidentView> open =
                    incidentService.listIncidents(tenantId, "OPEN", 100);
            if (open == null) {
                open = List.of();
            }
            long critical = open.stream().filter(row -> "CRITICAL".equalsIgnoreCase(nullToEmpty(row.severity()))).count();
            long warning = open.size() - critical;
            String sectionStatus;
            String reason;
            if (critical > 0) {
                sectionStatus = FAIL;
                reason = critical + " critical open warehouse incident(s)";
            } else if (!open.isEmpty()) {
                sectionStatus = WARN;
                reason = open.size() + " open warehouse incident(s)";
            } else {
                sectionStatus = PASS;
                reason = "no open warehouse incidents";
            }
            return new IncidentReadiness(sectionStatus, reason, open.size(), critical, warning);
        } catch (RuntimeException e) {
            return new IncidentReadiness(FAIL, "incident status failed: " + e.getMessage(),
                    0, 0, 0);
        }
    }

    private BiReadiness bi() {
        try {
            List<BiDatasourceHealth> rows = biDatasourceHealthProvider.health();
            if (rows == null) {
                rows = List.of();
            }
            long unavailable = rows.stream().filter(row -> row == null || !row.available()).count();
            String sectionStatus;
            String reason;
            if (unavailable > 0) {
                sectionStatus = FAIL;
                reason = unavailable + " BI datasource(s) unavailable";
            } else if (rows.isEmpty()) {
                sectionStatus = WARN;
                reason = "no BI datasource health rows";
            } else {
                sectionStatus = PASS;
                reason = "BI datasources are available";
            }
            return new BiReadiness(sectionStatus, reason, rows.size(), rows.size() - unavailable, unavailable);
        } catch (RuntimeException e) {
            return new BiReadiness(FAIL, "BI datasource health failed: " + e.getMessage(), 0, 0, 0);
        }
    }

    private AudienceMaterializationReadiness audienceMaterialization(
            Long tenantId,
            LocalDateTime now,
            CdpWarehouseSloPolicyService.SloPolicyView sloPolicy) {
        try {
            List<AudienceMaterializationOperationsService.RunView> runs =
                    audienceMaterializationOperationsService.recentRuns(tenantId, null, null, DEFAULT_RECENT_LIMIT);
            if (runs == null) {
                runs = List.of();
            }
            long failed = runs.stream().filter(run -> isFailed(run.status())).count();
            long success = runs.stream().filter(run -> "SUCCESS".equalsIgnoreCase(nullToEmpty(run.status()))).count();
            Long latestRunAgeMinutes = ageMinutes(latestMaterializationRunAt(runs), now);
            String sectionStatus;
            String reason;
            if (failed > 0) {
                sectionStatus = FAIL;
                reason = failed + " recent audience materialization run(s) failed";
            } else if (runs.isEmpty()) {
                sectionStatus = WARN;
                reason = "no recent audience materialization runs";
            } else if (thresholdReached(latestRunAgeMinutes, sloPolicy.audienceFailRunGapMinutes())) {
                sectionStatus = FAIL;
                reason = "latest audience materialization run age " + latestRunAgeMinutes
                        + "m reaches fail threshold " + sloPolicy.audienceFailRunGapMinutes() + "m";
            } else if (thresholdReached(latestRunAgeMinutes, sloPolicy.audienceWarnRunGapMinutes())) {
                sectionStatus = WARN;
                reason = "latest audience materialization run age " + latestRunAgeMinutes
                        + "m reaches warn threshold " + sloPolicy.audienceWarnRunGapMinutes() + "m";
            } else {
                sectionStatus = PASS;
                reason = "audience materialization runs are healthy";
            }
            return new AudienceMaterializationReadiness(sectionStatus, reason, runs.size(), success, failed);
        } catch (RuntimeException e) {
            return new AudienceMaterializationReadiness(FAIL,
                    "audience materialization status failed: " + e.getMessage(), 0, 0, 0);
        }
    }

    private CdpWarehouseSloPolicyService.SloPolicyView effectivePolicy(Long tenantId) {
        if (sloPolicyService == null) {
            return CdpWarehouseSloPolicyService.defaultPolicy(tenantId);
        }
        try {
            return sloPolicyService.effectivePolicy(tenantId);
        } catch (RuntimeException e) {
            return CdpWarehouseSloPolicyService.defaultPolicy(tenantId);
        }
    }

    private String overallStatus(List<ReadinessSection> sections) {
        if (sections.stream().anyMatch(section -> FAIL.equals(section.status()))) {
            return FAIL;
        }
        if (sections.stream().anyMatch(section -> WARN.equals(section.status()))) {
            return WARN;
        }
        return PASS;
    }

    private boolean isFailed(String status) {
        String normalized = nullToEmpty(status).toUpperCase(Locale.ROOT);
        return FAIL.equals(normalized) || "FAILED".equals(normalized) || "ERROR".equals(normalized);
    }

    private LocalDateTime latestRunAt(List<CdpWarehouseOperationsService.RunRow> runs) {
        return (runs == null ? List.<CdpWarehouseOperationsService.RunRow>of() : runs).stream()
                .map(run -> latest(run.finishedAt(), run.startedAt(), run.windowEnd(), run.windowStart()))
                .filter(value -> value != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    private LocalDateTime latestWatermarkAt(List<CdpWarehouseOperationsService.WatermarkRow> watermarks) {
        return (watermarks == null ? List.<CdpWarehouseOperationsService.WatermarkRow>of() : watermarks).stream()
                .map(row -> latest(row.watermarkTime(), row.updatedAt()))
                .filter(value -> value != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    private LocalDateTime latestMaterializationRunAt(List<AudienceMaterializationOperationsService.RunView> runs) {
        return (runs == null ? List.<AudienceMaterializationOperationsService.RunView>of() : runs).stream()
                .map(run -> latest(run.finishedAt(), run.startedAt()))
                .filter(value -> value != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    private LocalDateTime latest(LocalDateTime... values) {
        LocalDateTime latest = null;
        if (values == null) {
            return null;
        }
        for (LocalDateTime value : values) {
            if (value != null && (latest == null || value.isAfter(latest))) {
                latest = value;
            }
        }
        return latest;
    }

    private Long ageMinutes(LocalDateTime observedAt, LocalDateTime now) {
        if (observedAt == null || now == null) {
            return null;
        }
        return Math.max(0L, Duration.between(observedAt, now).toMinutes());
    }

    private boolean thresholdReached(Long value, int threshold) {
        return value != null && value >= threshold;
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public record ReadinessSummary(
            Long tenantId,
            String status,
            LocalDateTime generatedAt,
            List<ReadinessSection> sections,
            OfflineReadiness offline,
            RealtimeReadiness realtime,
            IncidentReadiness incidents,
            BiReadiness bi,
            AudienceMaterializationReadiness audienceMaterialization) {
    }

    public record ReadinessSection(String key, String status, String reason) {
    }

    public record OfflineReadiness(
            String status,
            String reason,
            int recentRunCount,
            long failedRunCount,
            long runningRunCount,
            int watermarkCount) {
    }

    public record RealtimeReadiness(
            String status,
            String reason,
            int pipelineCount,
            long passedCount,
            long warnedCount,
            long failedCount,
            int jobCount) {

        public RealtimeReadiness(
                String status,
                String reason,
                int pipelineCount,
                long passedCount,
                long warnedCount,
                long failedCount) {
            this(status, reason, pipelineCount, passedCount, warnedCount, failedCount, 0);
        }
    }

    public record IncidentReadiness(
            String status,
            String reason,
            int openCount,
            long criticalCount,
            long warningCount) {
    }

    public record BiReadiness(
            String status,
            String reason,
            int datasourceCount,
            long availableCount,
            long unavailableCount) {
    }

    public record AudienceMaterializationReadiness(
            String status,
            String reason,
            int recentRunCount,
            long successCount,
            long failedCount) {
    }
}
