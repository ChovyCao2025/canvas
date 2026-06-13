package org.chovy.canvas.cdp.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

public class CdpWarehouseReadinessPolicy {

    public static final String PASS = "PASS";
    public static final String WARN = "WARN";
    public static final String FAIL = "FAIL";

    public CdpWarehouseReadinessReport evaluate(CdpWarehouseReadinessEvidence evidence, LocalDateTime generatedAt) {
        CdpWarehouseReadinessEvidence safeEvidence = evidence == null
                ? new CdpWarehouseReadinessEvidence(0L, List.of(), List.of(),
                new WarehouseRealtimeStatus(0, 0, 0, 0), List.of(), List.of(), List.of())
                : evidence;
        List<CdpWarehouseReadinessSection> sections = List.of(
                offline(safeEvidence.offlineRuns(), safeEvidence.watermarks()),
                realtime(safeEvidence.realtimeStatus()),
                incidents(safeEvidence.incidents()),
                bi(safeEvidence.biDatasources()),
                materialization(safeEvidence.materializationRuns()));
        return new CdpWarehouseReadinessReport(
                safeEvidence.tenantId(),
                overallStatus(sections),
                generatedAt,
                sections);
    }

    private CdpWarehouseReadinessSection offline(List<WarehouseSyncRun> runs, List<WarehouseWatermark> watermarks) {
        List<WarehouseSyncRun> safeRuns = runs == null ? List.of() : runs;
        List<WarehouseWatermark> safeWatermarks = watermarks == null ? List.of() : watermarks;
        long failed = safeRuns.stream().filter(run -> isFailed(run.status())).count();
        if (failed > 0) {
            return new CdpWarehouseReadinessSection("offline_sync", FAIL,
                    failed + " recent offline sync run(s) failed");
        }
        if (safeRuns.isEmpty()) {
            return new CdpWarehouseReadinessSection("offline_sync", WARN, "no recent offline sync runs");
        }
        if (safeWatermarks.isEmpty()) {
            return new CdpWarehouseReadinessSection("offline_sync", WARN,
                    "offline sync watermarks are missing");
        }
        return new CdpWarehouseReadinessSection("offline_sync", PASS,
                "offline sync runs and watermarks are present");
    }

    private CdpWarehouseReadinessSection realtime(WarehouseRealtimeStatus status) {
        WarehouseRealtimeStatus safeStatus = status == null ? new WarehouseRealtimeStatus(0, 0, 0, 0) : status;
        if (safeStatus.failed() > 0) {
            return new CdpWarehouseReadinessSection("realtime_pipelines", FAIL,
                    safeStatus.failed() + " realtime pipeline/job(s) failed");
        }
        if (safeStatus.warned() > 0) {
            return new CdpWarehouseReadinessSection("realtime_pipelines", WARN,
                    safeStatus.warned() + " realtime pipeline/job(s) warning");
        }
        if (safeStatus.total() == 0) {
            return new CdpWarehouseReadinessSection("realtime_pipelines", WARN,
                    "no active realtime warehouse pipeline or job evidence");
        }
        return new CdpWarehouseReadinessSection("realtime_pipelines", PASS,
                "realtime pipelines and jobs are healthy");
    }

    private CdpWarehouseReadinessSection incidents(List<WarehouseIncident> incidents) {
        List<WarehouseIncident> open = (incidents == null ? List.<WarehouseIncident>of() : incidents).stream()
                .filter(row -> "OPEN".equalsIgnoreCase(nullToEmpty(row.status())))
                .toList();
        long critical = open.stream()
                .filter(row -> "CRITICAL".equalsIgnoreCase(nullToEmpty(row.severity())))
                .count();
        if (critical > 0) {
            return new CdpWarehouseReadinessSection("incidents", FAIL,
                    critical + " critical open warehouse incident(s)");
        }
        if (!open.isEmpty()) {
            return new CdpWarehouseReadinessSection("incidents", WARN,
                    open.size() + " open warehouse incident(s)");
        }
        return new CdpWarehouseReadinessSection("incidents", PASS, "no open warehouse incidents");
    }

    private CdpWarehouseReadinessSection bi(List<WarehouseBiDatasource> rows) {
        List<WarehouseBiDatasource> safeRows = rows == null ? List.of() : rows;
        long unavailable = safeRows.stream().filter(row -> row == null || !row.available()).count();
        if (unavailable > 0) {
            return new CdpWarehouseReadinessSection("bi_datasources", FAIL,
                    unavailable + " BI datasource(s) unavailable");
        }
        if (safeRows.isEmpty()) {
            return new CdpWarehouseReadinessSection("bi_datasources", WARN,
                    "no BI datasource health rows");
        }
        return new CdpWarehouseReadinessSection("bi_datasources", PASS, "BI datasources are available");
    }

    private CdpWarehouseReadinessSection materialization(List<WarehouseMaterializationRun> runs) {
        List<WarehouseMaterializationRun> safeRuns = runs == null ? List.of() : runs;
        long failed = safeRuns.stream().filter(run -> isFailed(run.status())).count();
        if (failed > 0) {
            return new CdpWarehouseReadinessSection("audience_materialization", FAIL,
                    failed + " recent audience materialization run(s) failed");
        }
        if (safeRuns.isEmpty()) {
            return new CdpWarehouseReadinessSection("audience_materialization", WARN,
                    "no recent audience materialization runs");
        }
        return new CdpWarehouseReadinessSection("audience_materialization", PASS,
                "audience materialization runs are healthy");
    }

    private String overallStatus(List<CdpWarehouseReadinessSection> sections) {
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

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
