package org.chovy.canvas.domain.warehouse;

import org.chovy.canvas.dal.dataobject.CdpWarehouseEnterpriseOlapEvidenceDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseEnterpriseOlapEvidenceMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class CdpWarehouseEnterpriseOlapEvidenceService {

    private static final String STATUS_PASS = "PASS";
    private static final String STATUS_WARN = "WARN";
    private static final String STATUS_FAIL = "FAIL";
    private static final String SOURCE_DORIS = "doris";
    private static final String SOURCE_OPERATOR = "operator";
    private static final String SOURCE_WAREHOUSE = "warehouse";
    private static final int RECENT_LIMIT = 100;
    private static final long LIVE_METRIC_MAX_AGE_MINUTES = 5;
    private static final long QUERY_SLO_MAX_AGE_MINUTES = 15;
    private static final long REPLAY_MAX_AGE_MINUTES = 24 * 60;
    private static final long OPERATOR_DRILL_MAX_AGE_MINUTES = 7 * 24 * 60;
    private static final long MIN_QUERY_SLO_SAMPLES = 5;
    private static final double FAIL_QUERY_ERR_RATE = 0.05;
    private static final double FAIL_QUERY_LATENCY_MS = 5_000;
    private static final double FAIL_THREAD_QUEUE_SIZE = 100;
    private static final double FAIL_COMPACTION_SCORE = 100;
    private static final double WARN_COMPACTION_SCORE = 50;
    private static final double FAIL_DISK_IO_UTIL_PERCENT = 90;
    private static final double WARN_QUERY_SLO_P95_LATENCY_MS = 2_000;
    private static final double FAIL_QUERY_SLO_P95_LATENCY_MS = 3_000;
    private static final double FAIL_QUERY_SLO_P99_LATENCY_MS = 8_000;
    private static final double FAIL_QUERY_SLO_ERROR_RATE = 0.01;
    private static final double FAIL_QUERY_SLO_QUEUE_WAIT_MS = 3_000;
    private static final long FAIL_QUERY_SLO_MEMORY_BYTES = 4L * 1024L * 1024L * 1024L;
    private static final Set<String> OPERATOR_KEYS = Set.of(
            "backup_restore",
            "ingestion_replay",
            "runbook_drill");
    private static final List<String> PROOF_ORDER = List.of(
            "doris_metrics",
            "workload_isolation",
            "query_slo",
            "backup_restore",
            "compaction_health",
            "ingestion_replay",
            "runbook_drill");
    private static final List<String> AUTOMATED_COLLECTION_ORDER = List.of(
            "doris_metrics",
            "workload_isolation",
            "query_slo",
            "compaction_health",
            "ingestion_replay");
    private static final List<String> REQUIRED_WORKLOAD_GROUPS = List.of("bi", "ingestion", "audience");
    private static final List<String> REQUIRED_QUERY_SLO_PROFILES =
            List.of("bi_dashboard", "audience_materialization", "ad_hoc_segment");

    private final CdpWarehouseEnterpriseOlapEvidenceMapper mapper;
    private final ObjectProvider<CdpWarehouseEnterpriseOlapDorisEvidenceClient> dorisEvidenceClient;
    private final ObjectProvider<CdpWarehouseSyntheticDataPathProbeService> syntheticDataPathProbeService;
    private final Clock clock;

    @Autowired
    public CdpWarehouseEnterpriseOlapEvidenceService(
            CdpWarehouseEnterpriseOlapEvidenceMapper mapper,
            ObjectProvider<CdpWarehouseEnterpriseOlapDorisEvidenceClient> dorisEvidenceClient,
            ObjectProvider<CdpWarehouseSyntheticDataPathProbeService> syntheticDataPathProbeService) {
        this(mapper, dorisEvidenceClient, syntheticDataPathProbeService, Clock.systemDefaultZone());
    }

    CdpWarehouseEnterpriseOlapEvidenceService(
            CdpWarehouseEnterpriseOlapEvidenceMapper mapper,
            ObjectProvider<CdpWarehouseEnterpriseOlapDorisEvidenceClient> dorisEvidenceClient,
            ObjectProvider<CdpWarehouseSyntheticDataPathProbeService> syntheticDataPathProbeService,
            Clock clock) {
        this.mapper = mapper;
        this.dorisEvidenceClient = dorisEvidenceClient;
        this.syntheticDataPathProbeService = syntheticDataPathProbeService;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    public EvidenceView recordOperatorEvidence(Long tenantId, EvidenceCommand command, String actor) {
        if (command == null) {
            throw new IllegalArgumentException("enterprise OLAP evidence command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String key = normalizeKey(command.evidenceKey());
        if (!OPERATOR_KEYS.contains(key)) {
            throw new IllegalArgumentException("operator evidence key must be one of " + OPERATOR_KEYS);
        }
        LocalDateTime measuredAt = command.measuredAt() == null ? now() : command.measuredAt();
        LocalDateTime expiresAt = command.expiresAt() == null
                ? measuredAt.plusMinutes(defaultMaxAgeMinutes(key))
                : command.expiresAt();
        if (!expiresAt.isAfter(measuredAt)) {
            throw new IllegalArgumentException("expiresAt must be after measuredAt");
        }
        CdpWarehouseEnterpriseOlapEvidenceDO row = new CdpWarehouseEnterpriseOlapEvidenceDO();
        row.setTenantId(scopedTenantId);
        row.setEvidenceKey(key);
        row.setSource(SOURCE_OPERATOR);
        row.setStatus(normalizeStatus(command.status()));
        row.setReason(defaultReason(command.reason(), key, row.getStatus()));
        row.setMeasuredAt(measuredAt);
        row.setExpiresAt(expiresAt);
        row.setEvidenceJson(defaultString(command.evidenceJson(), "{}"));
        row.setCreatedBy(defaultString(actor, "system"));
        mapper.insert(row);
        return toView(row);
    }

    public EvidenceBundle latestEvidence(Long tenantId) {
        Long scopedTenantId = normalizeTenant(tenantId);
        LocalDateTime evaluatedAt = now();
        Map<String, EvidenceView> gates = new LinkedHashMap<>();
        Map<String, CdpWarehouseEnterpriseOlapEvidenceDO> latestRows = latestRows(scopedTenantId);
        addLiveDorisEvidence(scopedTenantId, evaluatedAt, gates);
        gates.put("backup_restore", operatorGate(scopedTenantId, "backup_restore", latestRows, evaluatedAt));
        gates.put("ingestion_replay", ingestionReplayGate(scopedTenantId, latestRows, evaluatedAt));
        gates.put("runbook_drill", operatorGate(scopedTenantId, "runbook_drill", latestRows, evaluatedAt));
        List<EvidenceView> ordered = PROOF_ORDER.stream()
                .map(gates::get)
                .filter(row -> row != null)
                .toList();
        return new EvidenceBundle(scopedTenantId, worstStatus(ordered), evaluatedAt, ordered);
    }

    public List<CdpWarehouseProductionReadinessProofService.ProofEvidence> proofEvidence(Long tenantId) {
        return latestEvidence(tenantId).evidence().stream()
                .map(row -> new CdpWarehouseProductionReadinessProofService.ProofEvidence(
                        "enterprise_olap:" + row.evidenceKey(),
                        row.status(),
                        row.reason()))
                .toList();
    }

    public EvidenceBundle collectAutomatedEvidence(Long tenantId, String actor) {
        Long scopedTenantId = normalizeTenant(tenantId);
        LocalDateTime evaluatedAt = now();
        Map<String, EvidenceView> gates = new LinkedHashMap<>();
        Map<String, CdpWarehouseEnterpriseOlapEvidenceDO> latestRows = latestRows(scopedTenantId);
        addLiveDorisEvidence(scopedTenantId, evaluatedAt, gates);
        EvidenceView replay = ingestionReplayGate(scopedTenantId, latestRows, evaluatedAt);
        if (!SOURCE_OPERATOR.equals(replay.source())) {
            gates.put("ingestion_replay", replay);
        }
        List<EvidenceView> automated = AUTOMATED_COLLECTION_ORDER.stream()
                .map(gates::get)
                .filter(row -> row != null)
                .toList();
        for (EvidenceView row : automated) {
            insertCollectedEvidence(row, actor);
        }
        return new EvidenceBundle(scopedTenantId, worstStatus(automated), evaluatedAt, automated);
    }

    private void addLiveDorisEvidence(Long tenantId,
                                      LocalDateTime evaluatedAt,
                                      Map<String, EvidenceView> gates) {
        CdpWarehouseEnterpriseOlapDorisEvidenceClient client =
                dorisEvidenceClient == null ? null : dorisEvidenceClient.getIfAvailable();
        if (client == null) {
            gates.put("doris_metrics", fail(tenantId, "doris_metrics", SOURCE_DORIS,
                    "Doris evidence client is not configured", evaluatedAt));
            gates.put("workload_isolation", fail(tenantId, "workload_isolation", SOURCE_DORIS,
                    "Doris evidence client is not configured", evaluatedAt));
            gates.put("query_slo", fail(tenantId, "query_slo", SOURCE_DORIS,
                    "Doris evidence client is not configured", evaluatedAt));
            gates.put("compaction_health", fail(tenantId, "compaction_health", SOURCE_DORIS,
                    "Doris evidence client is not configured", evaluatedAt));
            return;
        }
        try {
            CdpWarehouseEnterpriseOlapDorisEvidenceClient.DorisMetricsEvidence metrics = client.metrics();
            gates.put("doris_metrics", dorisMetricsGate(tenantId, metrics, evaluatedAt));
            gates.put("compaction_health", compactionGate(tenantId, metrics, evaluatedAt));
        } catch (RuntimeException e) {
            gates.put("doris_metrics", fail(tenantId, "doris_metrics", SOURCE_DORIS,
                    "Doris metrics collection failed: " + message(e), evaluatedAt));
            gates.put("compaction_health", fail(tenantId, "compaction_health", SOURCE_DORIS,
                    "Doris compaction metrics collection failed: " + message(e), evaluatedAt));
        }
        try {
            gates.put("workload_isolation", workloadIsolationGate(tenantId, client.workloadGroups(), evaluatedAt));
        } catch (RuntimeException e) {
            gates.put("workload_isolation", fail(tenantId, "workload_isolation", SOURCE_DORIS,
                    "Doris workload isolation collection failed: " + message(e), evaluatedAt));
        }
        try {
            gates.put("query_slo", querySloGate(tenantId, client.querySlo(), evaluatedAt));
        } catch (RuntimeException e) {
            gates.put("query_slo", fail(tenantId, "query_slo", SOURCE_DORIS,
                    "Doris query SLO collection failed: " + message(e), evaluatedAt));
        }
    }

    private EvidenceView dorisMetricsGate(
            Long tenantId,
            CdpWarehouseEnterpriseOlapDorisEvidenceClient.DorisMetricsEvidence metrics,
            LocalDateTime evaluatedAt) {
        List<CdpWarehouseEnterpriseOlapDorisEvidenceClient.MetricsEndpointEvidence> endpoints =
                metrics == null ? List.of() : metrics.endpoints();
        if (endpoints.isEmpty()) {
            return fail(tenantId, "doris_metrics", SOURCE_DORIS, "Doris metrics endpoints are missing", evaluatedAt);
        }
        if (!hasFreshRole(endpoints, "FE", evaluatedAt) || !hasFreshRole(endpoints, "BE", evaluatedAt)) {
            return fail(tenantId, "doris_metrics", SOURCE_DORIS,
                    "Doris FE and BE metrics must both be fresh", evaluatedAt);
        }
        double errRate = max(endpoints, "doris_fe_query_err_rate");
        double latency = max(endpoints, "doris_fe_query_latency_ms");
        double queue = max(endpoints,
                "fragment_thread_pool_queue_size",
                "doris_be_scanner_thread_pool_queue_size",
                "doris_be_send_batch_thread_pool_queue_size");
        if (known(errRate) && errRate >= FAIL_QUERY_ERR_RATE) {
            return fail(tenantId, "doris_metrics", SOURCE_DORIS,
                    "Doris query error rate " + errRate + " reaches fail threshold " + FAIL_QUERY_ERR_RATE,
                    evaluatedAt);
        }
        if (known(latency) && latency >= FAIL_QUERY_LATENCY_MS) {
            return fail(tenantId, "doris_metrics", SOURCE_DORIS,
                    "Doris query latency " + latency + "ms reaches fail threshold " + FAIL_QUERY_LATENCY_MS + "ms",
                    evaluatedAt);
        }
        if (known(queue) && queue >= FAIL_THREAD_QUEUE_SIZE) {
            return fail(tenantId, "doris_metrics", SOURCE_DORIS,
                    "Doris thread pool queue " + queue + " reaches fail threshold " + FAIL_THREAD_QUEUE_SIZE,
                    evaluatedAt);
        }
        return view(null, tenantId, "doris_metrics", SOURCE_DORIS, STATUS_PASS,
                "Doris FE/BE metrics are fresh and under policy", evaluatedAt, evaluatedAt.plusMinutes(5), "{}",
                "doris");
    }

    private EvidenceView compactionGate(
            Long tenantId,
            CdpWarehouseEnterpriseOlapDorisEvidenceClient.DorisMetricsEvidence metrics,
            LocalDateTime evaluatedAt) {
        List<CdpWarehouseEnterpriseOlapDorisEvidenceClient.MetricsEndpointEvidence> endpoints =
                metrics == null ? List.of() : metrics.endpoints();
        if (endpoints.isEmpty()) {
            return fail(tenantId, "compaction_health", SOURCE_DORIS,
                    "Doris compaction metrics are missing", evaluatedAt);
        }
        double compactionScore = max(endpoints,
                "doris_fe_max_tablet_compaction_score",
                "doris_fe_tablet_max_compaction_score",
                "doris_be_tablet_base_max_compaction_score",
                "doris_be_tablet_cumulative_max_compaction_score");
        double diskPressure = max(endpoints, "doris_be_max_disk_io_util_percent");
        if (known(compactionScore) && compactionScore >= FAIL_COMPACTION_SCORE) {
            return fail(tenantId, "compaction_health", SOURCE_DORIS,
                    "Doris compaction score " + compactionScore
                            + " reaches fail threshold " + FAIL_COMPACTION_SCORE,
                    evaluatedAt);
        }
        if (known(diskPressure) && diskPressure >= FAIL_DISK_IO_UTIL_PERCENT) {
            return fail(tenantId, "compaction_health", SOURCE_DORIS,
                    "Doris disk IO utilization " + diskPressure
                            + "% reaches fail threshold " + FAIL_DISK_IO_UTIL_PERCENT + "%",
                    evaluatedAt);
        }
        String status = known(compactionScore) && compactionScore >= WARN_COMPACTION_SCORE
                ? STATUS_WARN
                : STATUS_PASS;
        return view(null, tenantId, "compaction_health", SOURCE_DORIS, status,
                "Doris compaction and disk metrics " + status.toLowerCase(Locale.ROOT),
                evaluatedAt, evaluatedAt.plusMinutes(5), "{}", "doris");
    }

    private EvidenceView workloadIsolationGate(
            Long tenantId,
            List<CdpWarehouseEnterpriseOlapDorisEvidenceClient.WorkloadGroupEvidence> groups,
            LocalDateTime evaluatedAt) {
        Map<String, CdpWarehouseEnterpriseOlapDorisEvidenceClient.WorkloadGroupEvidence> byName =
                new LinkedHashMap<>();
        for (CdpWarehouseEnterpriseOlapDorisEvidenceClient.WorkloadGroupEvidence group : safeList(groups)) {
            if (group != null && hasText(group.name())) {
                byName.put(group.name().trim().toLowerCase(Locale.ROOT), group);
            }
        }
        List<String> missing = REQUIRED_WORKLOAD_GROUPS.stream()
                .filter(required -> !byName.containsKey(required))
                .toList();
        if (!missing.isEmpty()) {
            return fail(tenantId, "workload_isolation", SOURCE_DORIS,
                    "missing Doris workload groups " + String.join(", ", missing), evaluatedAt);
        }
        List<String> uncontrolled = REQUIRED_WORKLOAD_GROUPS.stream()
                .map(byName::get)
                .filter(group -> !hasExplicitControls(group))
                .map(CdpWarehouseEnterpriseOlapDorisEvidenceClient.WorkloadGroupEvidence::name)
                .toList();
        if (!uncontrolled.isEmpty()) {
            return fail(tenantId, "workload_isolation", SOURCE_DORIS,
                    "Doris workload groups have no explicit resource controls: " + String.join(", ", uncontrolled),
                    evaluatedAt);
        }
        return view(null, tenantId, "workload_isolation", SOURCE_DORIS, STATUS_PASS,
                "Doris workload groups isolate BI, ingestion, and audience workloads",
                evaluatedAt, evaluatedAt.plusMinutes(60), "{}", "doris");
    }

    private EvidenceView querySloGate(
            Long tenantId,
            List<CdpWarehouseEnterpriseOlapDorisEvidenceClient.QuerySloEvidence> querySloRows,
            LocalDateTime evaluatedAt) {
        Map<String, CdpWarehouseEnterpriseOlapDorisEvidenceClient.QuerySloEvidence> byProfile =
                new LinkedHashMap<>();
        for (CdpWarehouseEnterpriseOlapDorisEvidenceClient.QuerySloEvidence row : safeList(querySloRows)) {
            if (row != null && hasText(row.profileKey())) {
                byProfile.putIfAbsent(normalizeKey(row.profileKey()), row);
            }
        }
        List<String> missing = REQUIRED_QUERY_SLO_PROFILES.stream()
                .filter(profile -> !byProfile.containsKey(profile))
                .toList();
        List<String> stale = REQUIRED_QUERY_SLO_PROFILES.stream()
                .filter(byProfile::containsKey)
                .filter(profile -> byProfile.get(profile).measuredAt() == null
                        || ageMinutes(byProfile.get(profile).measuredAt(), evaluatedAt) > QUERY_SLO_MAX_AGE_MINUTES)
                .toList();
        if (!missing.isEmpty() || !stale.isEmpty()) {
            List<String> reasons = new ArrayList<>();
            if (!missing.isEmpty()) {
                reasons.add("missing query SLO profiles " + String.join(", ", missing));
            }
            if (!stale.isEmpty()) {
                reasons.add("stale query SLO profiles " + String.join(", ", stale));
            }
            return fail(tenantId, "query_slo", SOURCE_DORIS, String.join("; ", reasons), evaluatedAt);
        }
        for (String profile : REQUIRED_QUERY_SLO_PROFILES) {
            CdpWarehouseEnterpriseOlapDorisEvidenceClient.QuerySloEvidence row = byProfile.get(profile);
            long sampleCount = row.sampleCount() == null ? 0L : row.sampleCount();
            long errorCount = row.errorCount() == null ? 0L : row.errorCount();
            if (sampleCount < MIN_QUERY_SLO_SAMPLES) {
                return fail(tenantId, "query_slo", SOURCE_DORIS,
                        "query SLO profile " + profile + " sample count " + sampleCount
                                + " is below minimum " + MIN_QUERY_SLO_SAMPLES,
                        evaluatedAt);
            }
            double errorRate = sampleCount <= 0 ? (errorCount > 0 ? 1.0 : 0.0) : (double) errorCount / sampleCount;
            if (errorRate >= FAIL_QUERY_SLO_ERROR_RATE) {
                return fail(tenantId, "query_slo", SOURCE_DORIS,
                        "query SLO profile " + profile + " error rate " + errorRate
                                + " reaches fail threshold " + FAIL_QUERY_SLO_ERROR_RATE,
                        evaluatedAt);
            }
            if (known(row.p95LatencyMs()) && row.p95LatencyMs() >= FAIL_QUERY_SLO_P95_LATENCY_MS) {
                return fail(tenantId, "query_slo", SOURCE_DORIS,
                        "query SLO profile " + profile + " p95 latency " + row.p95LatencyMs()
                                + "ms reaches fail threshold " + FAIL_QUERY_SLO_P95_LATENCY_MS + "ms",
                        evaluatedAt);
            }
            if (known(row.p99LatencyMs()) && row.p99LatencyMs() >= FAIL_QUERY_SLO_P99_LATENCY_MS) {
                return fail(tenantId, "query_slo", SOURCE_DORIS,
                        "query SLO profile " + profile + " p99 latency " + row.p99LatencyMs()
                                + "ms reaches fail threshold " + FAIL_QUERY_SLO_P99_LATENCY_MS + "ms",
                        evaluatedAt);
            }
            if (known(row.maxQueueWaitMs()) && row.maxQueueWaitMs() >= FAIL_QUERY_SLO_QUEUE_WAIT_MS) {
                return fail(tenantId, "query_slo", SOURCE_DORIS,
                        "query SLO profile " + profile + " queue wait " + row.maxQueueWaitMs()
                                + "ms reaches fail threshold " + FAIL_QUERY_SLO_QUEUE_WAIT_MS + "ms",
                        evaluatedAt);
            }
            if (row.maxPeakMemoryBytes() != null && row.maxPeakMemoryBytes() >= FAIL_QUERY_SLO_MEMORY_BYTES) {
                return fail(tenantId, "query_slo", SOURCE_DORIS,
                        "query SLO profile " + profile + " peak memory " + row.maxPeakMemoryBytes()
                                + " bytes reaches fail threshold " + FAIL_QUERY_SLO_MEMORY_BYTES,
                        evaluatedAt);
            }
        }
        List<String> warnings = REQUIRED_QUERY_SLO_PROFILES.stream()
                .map(byProfile::get)
                .filter(row -> known(row.p95LatencyMs()) && row.p95LatencyMs() >= WARN_QUERY_SLO_P95_LATENCY_MS)
                .map(row -> row.profileKey() + " p95 latency " + row.p95LatencyMs() + "ms")
                .toList();
        if (!warnings.isEmpty()) {
            return view(null, tenantId, "query_slo", SOURCE_DORIS, STATUS_WARN,
                    "query SLO warning: " + String.join("; ", warnings),
                    evaluatedAt, evaluatedAt.plusMinutes(QUERY_SLO_MAX_AGE_MINUTES), "{}", "doris");
        }
        return view(null, tenantId, "query_slo", SOURCE_DORIS, STATUS_PASS,
                "query SLO representative profiles are fresh and under policy",
                evaluatedAt, evaluatedAt.plusMinutes(QUERY_SLO_MAX_AGE_MINUTES), "{}", "doris");
    }

    private EvidenceView operatorGate(Long tenantId,
                                      String key,
                                      Map<String, CdpWarehouseEnterpriseOlapEvidenceDO> latestRows,
                                      LocalDateTime evaluatedAt) {
        CdpWarehouseEnterpriseOlapEvidenceDO row = latestRows.get(key);
        if (row == null) {
            return fail(tenantId, key, SOURCE_OPERATOR, "missing operator evidence for " + key, evaluatedAt);
        }
        EvidenceView view = toView(row);
        if (isExpired(view, evaluatedAt)) {
            return fail(tenantId, key, view.source(),
                    key + " evidence expired at " + effectiveExpiresAt(view), evaluatedAt);
        }
        return view(view.id(), tenantId, key, view.source(), view.status(), view.reason(),
                view.measuredAt(), effectiveExpiresAt(view), view.evidenceJson(), view.createdBy());
    }

    private EvidenceView ingestionReplayGate(Long tenantId,
                                             Map<String, CdpWarehouseEnterpriseOlapEvidenceDO> latestRows,
                                             LocalDateTime evaluatedAt) {
        CdpWarehouseSyntheticDataPathProbeService service =
                syntheticDataPathProbeService == null ? null : syntheticDataPathProbeService.getIfAvailable();
        RuntimeException probeFailure = null;
        if (service != null) {
            try {
                List<CdpWarehouseSyntheticDataPathProbeService.ProbeRunView> probes =
                        safeList(service.recent(tenantId, 10)).stream()
                                .filter(row -> row != null && row.finishedAt() != null)
                                .sorted(Comparator.comparing(
                                        CdpWarehouseSyntheticDataPathProbeService.ProbeRunView::finishedAt,
                                        Comparator.reverseOrder()))
                                .toList();
                if (!probes.isEmpty()) {
                    CdpWarehouseSyntheticDataPathProbeService.ProbeRunView latest = probes.get(0);
                    if (ageMinutes(latest.finishedAt(), evaluatedAt) <= REPLAY_MAX_AGE_MINUTES) {
                        String status = normalizeStatus(latest.status());
                        return view(latest.id(), tenantId, "ingestion_replay", SOURCE_WAREHOUSE, status,
                                "synthetic ODS data-path probe " + status.toLowerCase(Locale.ROOT)
                                        + " rows=" + latest.odsRowCount(),
                                latest.finishedAt(),
                                latest.finishedAt().plusMinutes(REPLAY_MAX_AGE_MINUTES),
                                latest.evidenceJson(),
                                "synthetic-probe");
                    }
                }
            } catch (RuntimeException e) {
                probeFailure = e;
            }
        }
        if (probeFailure != null && !latestRows.containsKey("ingestion_replay")) {
            return fail(tenantId, "ingestion_replay", SOURCE_WAREHOUSE,
                    "synthetic ODS probe evidence failed: " + message(probeFailure), evaluatedAt);
        }
        return operatorGate(tenantId, "ingestion_replay", latestRows, evaluatedAt);
    }

    private Map<String, CdpWarehouseEnterpriseOlapEvidenceDO> latestRows(Long tenantId) {
        Map<String, CdpWarehouseEnterpriseOlapEvidenceDO> latest = new LinkedHashMap<>();
        for (CdpWarehouseEnterpriseOlapEvidenceDO row : safeList(mapper.listRecent(tenantId, RECENT_LIMIT))) {
            if (row == null || !hasText(row.getEvidenceKey())) {
                continue;
            }
            latest.putIfAbsent(normalizeKey(row.getEvidenceKey()), row);
        }
        return latest;
    }

    private boolean hasFreshRole(
            List<CdpWarehouseEnterpriseOlapDorisEvidenceClient.MetricsEndpointEvidence> endpoints,
            String role,
            LocalDateTime evaluatedAt) {
        return endpoints.stream()
                .filter(endpoint -> role.equalsIgnoreCase(defaultString(endpoint.role(), "")))
                .anyMatch(endpoint -> endpoint.measuredAt() != null
                        && ageMinutes(endpoint.measuredAt(), evaluatedAt) <= LIVE_METRIC_MAX_AGE_MINUTES
                        && !endpoint.metrics().isEmpty());
    }

    private boolean hasExplicitControls(
            CdpWarehouseEnterpriseOlapDorisEvidenceClient.WorkloadGroupEvidence group) {
        if (group == null) {
            return false;
        }
        return positive(group.minCpuPercent())
                || positive(group.maxCpuPercent())
                || positive(group.minMemoryPercent())
                || positive(group.maxMemoryPercent())
                || boundedConcurrency(group.maxConcurrency())
                || positive(group.maxQueueSize())
                || positive(group.queueTimeoutMs())
                || positive(group.readBytesPerSecond())
                || positive(group.remoteReadBytesPerSecond());
    }

    private double max(
            List<CdpWarehouseEnterpriseOlapDorisEvidenceClient.MetricsEndpointEvidence> endpoints,
            String... names) {
        double value = Double.NaN;
        for (CdpWarehouseEnterpriseOlapDorisEvidenceClient.MetricsEndpointEvidence endpoint : endpoints) {
            for (String name : names) {
                double candidate = endpoint.value(name);
                if (known(candidate)) {
                    value = known(value) ? Math.max(value, candidate) : candidate;
                }
            }
        }
        return value;
    }

    private boolean known(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    private boolean known(Double value) {
        return value != null && known(value.doubleValue());
    }

    private boolean positive(Number value) {
        return value != null && value.doubleValue() > 0;
    }

    private boolean boundedConcurrency(Integer value) {
        return value != null && value > 0 && value < Integer.MAX_VALUE;
    }

    private boolean isExpired(EvidenceView view, LocalDateTime evaluatedAt) {
        LocalDateTime expiresAt = effectiveExpiresAt(view);
        return expiresAt == null || !expiresAt.isAfter(evaluatedAt);
    }

    private LocalDateTime effectiveExpiresAt(EvidenceView view) {
        if (view.expiresAt() != null) {
            return view.expiresAt();
        }
        return view.measuredAt() == null
                ? null
                : view.measuredAt().plusMinutes(defaultMaxAgeMinutes(view.evidenceKey()));
    }

    private long defaultMaxAgeMinutes(String key) {
        if ("ingestion_replay".equals(normalizeKey(key))) {
            return REPLAY_MAX_AGE_MINUTES;
        }
        return OPERATOR_DRILL_MAX_AGE_MINUTES;
    }

    private long ageMinutes(LocalDateTime measuredAt, LocalDateTime evaluatedAt) {
        if (measuredAt == null || evaluatedAt == null) {
            return Long.MAX_VALUE;
        }
        return Math.max(0L, Duration.between(measuredAt, evaluatedAt).toMinutes());
    }

    private EvidenceView fail(Long tenantId, String key, String source, String reason, LocalDateTime evaluatedAt) {
        return view(null, tenantId, key, source, STATUS_FAIL, reason, evaluatedAt, evaluatedAt, "{}", "system");
    }

    private EvidenceView toView(CdpWarehouseEnterpriseOlapEvidenceDO row) {
        return view(row.getId(), row.getTenantId(), row.getEvidenceKey(), row.getSource(), row.getStatus(),
                row.getReason(), row.getMeasuredAt(), row.getExpiresAt(), row.getEvidenceJson(), row.getCreatedBy());
    }

    private void insertCollectedEvidence(EvidenceView view, String actor) {
        CdpWarehouseEnterpriseOlapEvidenceDO row = new CdpWarehouseEnterpriseOlapEvidenceDO();
        row.setTenantId(view.tenantId());
        row.setEvidenceKey(view.evidenceKey());
        row.setSource(view.source());
        row.setStatus(view.status());
        row.setReason(view.reason());
        row.setMeasuredAt(view.measuredAt());
        row.setExpiresAt(view.expiresAt());
        row.setEvidenceJson(view.evidenceJson());
        row.setCreatedBy(defaultString(actor, "enterprise-olap-evidence-collector"));
        mapper.insert(row);
    }

    private EvidenceView view(Long id,
                              Long tenantId,
                              String evidenceKey,
                              String source,
                              String status,
                              String reason,
                              LocalDateTime measuredAt,
                              LocalDateTime expiresAt,
                              String evidenceJson,
                              String createdBy) {
        return new EvidenceView(
                id,
                normalizeTenant(tenantId),
                normalizeKey(evidenceKey),
                defaultString(source, "unknown"),
                normalizeStatus(status),
                defaultReason(reason, evidenceKey, status),
                measuredAt == null ? now() : measuredAt,
                expiresAt,
                defaultString(evidenceJson, "{}"),
                defaultString(createdBy, "system"));
    }

    private String worstStatus(List<EvidenceView> rows) {
        if (rows.stream().map(EvidenceView::status).anyMatch(STATUS_FAIL::equals)) {
            return STATUS_FAIL;
        }
        if (rows.stream().map(EvidenceView::status).anyMatch(STATUS_WARN::equals)) {
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

    private String normalizeKey(String value) {
        return defaultString(value, "").toLowerCase(Locale.ROOT);
    }

    private String defaultReason(String reason, String key, String status) {
        if (hasText(reason)) {
            return reason.trim();
        }
        return normalizeKey(key) + " " + normalizeStatus(status).toLowerCase(Locale.ROOT);
    }

    private String defaultString(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private String message(RuntimeException e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    public record EvidenceCommand(
            String evidenceKey,
            String status,
            String reason,
            LocalDateTime measuredAt,
            LocalDateTime expiresAt,
            String evidenceJson) {
    }

    public record EvidenceBundle(
            Long tenantId,
            String status,
            LocalDateTime evaluatedAt,
            List<EvidenceView> evidence) {
        public EvidenceBundle {
            evidence = evidence == null ? List.of() : List.copyOf(evidence);
        }
    }

    public record EvidenceView(
            Long id,
            Long tenantId,
            String evidenceKey,
            String source,
            String status,
            String reason,
            LocalDateTime measuredAt,
            LocalDateTime expiresAt,
            String evidenceJson,
            String createdBy) {
    }
}
