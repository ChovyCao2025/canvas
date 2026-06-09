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
/**
 * CdpWarehouseEnterpriseOlapEvidenceService 承载对应领域的业务规则、流程编排和结果转换。
 */
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
    /**
     * 初始化 CdpWarehouseEnterpriseOlapEvidenceService 实例。
     *
     * @param mapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param dorisEvidenceClient 依赖组件，用于完成数据访问或外部能力调用。
     * @param syntheticDataPathProbeService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseEnterpriseOlapEvidenceService(
            CdpWarehouseEnterpriseOlapEvidenceMapper mapper,
            ObjectProvider<CdpWarehouseEnterpriseOlapDorisEvidenceClient> dorisEvidenceClient,
            ObjectProvider<CdpWarehouseSyntheticDataPathProbeService> syntheticDataPathProbeService) {
        this(mapper, dorisEvidenceClient, syntheticDataPathProbeService, Clock.systemDefaultZone());
    }

    /**
     * 初始化 CdpWarehouseEnterpriseOlapEvidenceService 实例。
     *
     * @param mapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param dorisEvidenceClient 依赖组件，用于完成数据访问或外部能力调用。
     * @param syntheticDataPathProbeService 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
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

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @param actor 操作人标识，用于审计和权限判断。
     * @return 返回流程执行后的业务结果。
     */
    public EvidenceView recordOperatorEvidence(Long tenantId, EvidenceCommand command, String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        mapper.insert(row);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 latestEvidence 流程生成的业务结果。
     */
    public EvidenceBundle latestEvidence(Long tenantId) {
        // 准备本次处理所需的上下文和中间变量。
        Long scopedTenantId = normalizeTenant(tenantId);
        LocalDateTime evaluatedAt = now();
        Map<String, EvidenceView> gates = new LinkedHashMap<>();
        Map<String, CdpWarehouseEnterpriseOlapEvidenceDO> latestRows = latestRows(scopedTenantId);
        addLiveDorisEvidence(scopedTenantId, evaluatedAt, gates);
        gates.put("backup_restore", operatorGate(scopedTenantId, "backup_restore", latestRows, evaluatedAt));
        gates.put("ingestion_replay", ingestionReplayGate(scopedTenantId, latestRows, evaluatedAt));
        gates.put("runbook_drill", operatorGate(scopedTenantId, "runbook_drill", latestRows, evaluatedAt));
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        List<EvidenceView> ordered = PROOF_ORDER.stream()
                .map(gates::get)
                .filter(row -> row != null)
                .toList();
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new EvidenceBundle(scopedTenantId, worstStatus(ordered), evaluatedAt, ordered);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 proof evidence 汇总后的集合、分页或映射视图。
     */
    public List<CdpWarehouseProductionReadinessProofService.ProofEvidence> proofEvidence(Long tenantId) {
        return latestEvidence(tenantId).evidence().stream()
                .map(row -> new CdpWarehouseProductionReadinessProofService.ProofEvidence(
                        "enterprise_olap:" + row.evidenceKey(),
                        row.status(),
                        row.reason()))
                .toList();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param actor 操作人标识，用于审计和权限判断。
     * @return 返回 collectAutomatedEvidence 流程生成的业务结果。
     */
    public EvidenceBundle collectAutomatedEvidence(Long tenantId, String actor) {
        Long scopedTenantId = normalizeTenant(tenantId);
        LocalDateTime evaluatedAt = now();
        Map<String, EvidenceView> gates = new LinkedHashMap<>();
        Map<String, CdpWarehouseEnterpriseOlapEvidenceDO> latestRows = latestRows(scopedTenantId);
        addLiveDorisEvidence(scopedTenantId, evaluatedAt, gates);
        EvidenceView replay = ingestionReplayGate(scopedTenantId, latestRows, evaluatedAt);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!SOURCE_OPERATOR.equals(replay.source())) {
            gates.put("ingestion_replay", replay);
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        List<EvidenceView> automated = AUTOMATED_COLLECTION_ORDER.stream()
                .map(gates::get)
                .filter(row -> row != null)
                .toList();
        for (EvidenceView row : automated) {
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            insertCollectedEvidence(row, actor);
        }
        return new EvidenceBundle(scopedTenantId, worstStatus(automated), evaluatedAt, automated);
    }

    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param evaluatedAt 时间参数，用于计算窗口、过期或审计时间。
     * @param gates gates 参数，用于 addLiveDorisEvidence 流程中的校验、计算或对象转换。
     */
    private void addLiveDorisEvidence(Long tenantId,
                                      LocalDateTime evaluatedAt,
                                      Map<String, EvidenceView> gates) {
        // 准备本次处理所需的上下文和中间变量。
        CdpWarehouseEnterpriseOlapDorisEvidenceClient client =
                dorisEvidenceClient == null ? null : dorisEvidenceClient.getIfAvailable();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (client == null) {
            gates.put("doris_metrics", fail(tenantId, "doris_metrics", SOURCE_DORIS,
                    "Doris evidence client is not configured", evaluatedAt));
            gates.put("workload_isolation", fail(tenantId, "workload_isolation", SOURCE_DORIS,
                    "Doris evidence client is not configured", evaluatedAt));
            gates.put("query_slo", fail(tenantId, "query_slo", SOURCE_DORIS,
                    "Doris evidence client is not configured", evaluatedAt));
            gates.put("compaction_health", fail(tenantId, "compaction_health", SOURCE_DORIS,
                    "Doris evidence client is not configured", evaluatedAt));
            // 汇总前面计算出的状态和明细，返回给调用方。
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param metrics metrics 参数，用于 dorisMetricsGate 流程中的校验、计算或对象转换。
     * @param evaluatedAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 dorisMetricsGate 流程生成的业务结果。
     */
    private EvidenceView dorisMetricsGate(
            Long tenantId,
            CdpWarehouseEnterpriseOlapDorisEvidenceClient.DorisMetricsEvidence metrics,
            LocalDateTime evaluatedAt) {
        // 准备本次处理所需的上下文和中间变量。
        List<CdpWarehouseEnterpriseOlapDorisEvidenceClient.MetricsEndpointEvidence> endpoints =
                metrics == null ? List.of() : metrics.endpoints();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return view(null, tenantId, "doris_metrics", SOURCE_DORIS, STATUS_PASS,
                "Doris FE/BE metrics are fresh and under policy", evaluatedAt, evaluatedAt.plusMinutes(5), "{}",
                "doris");
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param metrics metrics 参数，用于 compactionGate 流程中的校验、计算或对象转换。
     * @param evaluatedAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 compactionGate 流程生成的业务结果。
     */
    private EvidenceView compactionGate(
            Long tenantId,
            CdpWarehouseEnterpriseOlapDorisEvidenceClient.DorisMetricsEvidence metrics,
            LocalDateTime evaluatedAt) {
        // 准备本次处理所需的上下文和中间变量。
        List<CdpWarehouseEnterpriseOlapDorisEvidenceClient.MetricsEndpointEvidence> endpoints =
                metrics == null ? List.of() : metrics.endpoints();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return view(null, tenantId, "compaction_health", SOURCE_DORIS, status,
                "Doris compaction and disk metrics " + status.toLowerCase(Locale.ROOT),
                evaluatedAt, evaluatedAt.plusMinutes(5), "{}", "doris");
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param groups groups 参数，用于 workloadIsolationGate 流程中的校验、计算或对象转换。
     * @param evaluatedAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 workloadIsolationGate 流程生成的业务结果。
     */
    private EvidenceView workloadIsolationGate(
            Long tenantId,
            List<CdpWarehouseEnterpriseOlapDorisEvidenceClient.WorkloadGroupEvidence> groups,
            LocalDateTime evaluatedAt) {
        Map<String, CdpWarehouseEnterpriseOlapDorisEvidenceClient.WorkloadGroupEvidence> byName =
                new LinkedHashMap<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (CdpWarehouseEnterpriseOlapDorisEvidenceClient.WorkloadGroupEvidence group : safeList(groups)) {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return view(null, tenantId, "workload_isolation", SOURCE_DORIS, STATUS_PASS,
                "Doris workload groups isolate BI, ingestion, and audience workloads",
                evaluatedAt, evaluatedAt.plusMinutes(60), "{}", "doris");
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param querySloRows query slo rows 参数，用于 querySloGate 流程中的校验、计算或对象转换。
     * @param evaluatedAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回符合条件的数据列表或视图。
     */
    private EvidenceView querySloGate(
            Long tenantId,
            List<CdpWarehouseEnterpriseOlapDorisEvidenceClient.QuerySloEvidence> querySloRows,
            LocalDateTime evaluatedAt) {
        Map<String, CdpWarehouseEnterpriseOlapDorisEvidenceClient.QuerySloEvidence> byProfile =
                new LinkedHashMap<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (CdpWarehouseEnterpriseOlapDorisEvidenceClient.QuerySloEvidence row : safeList(querySloRows)) {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return view(null, tenantId, "query_slo", SOURCE_DORIS, STATUS_PASS,
                "query SLO representative profiles are fresh and under policy",
                evaluatedAt, evaluatedAt.plusMinutes(QUERY_SLO_MAX_AGE_MINUTES), "{}", "doris");
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param key 业务键，用于在同一租户下定位资源。
     * @param latestRows latest rows 参数，用于 operatorGate 流程中的校验、计算或对象转换。
     * @param evaluatedAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 operatorGate 流程生成的业务结果。
     */
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

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param latestRows latest rows 参数，用于 ingestionReplayGate 流程中的校验、计算或对象转换。
     * @param evaluatedAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 ingestionReplayGate 流程生成的业务结果。
     */
    private EvidenceView ingestionReplayGate(Long tenantId,
                                             Map<String, CdpWarehouseEnterpriseOlapEvidenceDO> latestRows,
                                             LocalDateTime evaluatedAt) {
        CdpWarehouseSyntheticDataPathProbeService service =
                syntheticDataPathProbeService == null ? null : syntheticDataPathProbeService.getIfAvailable();
        RuntimeException probeFailure = null;
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (service != null) {
            try {
                List<CdpWarehouseSyntheticDataPathProbeService.ProbeRunView> probes =
                        // 遍历候选数据并按业务规则筛选、转换或聚合。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return operatorGate(tenantId, "ingestion_replay", latestRows, evaluatedAt);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 latestRows 流程生成的业务结果。
     */
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

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param endpoints endpoints 参数，用于 hasFreshRole 流程中的校验、计算或对象转换。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param evaluatedAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回布尔判断结果。
     */
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

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param group group 参数，用于 hasExplicitControls 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param endpoints endpoints 参数，用于 max 流程中的校验、计算或对象转换。
     * @param names names 参数，用于 max 流程中的校验、计算或对象转换。
     * @return 返回 max 计算得到的数量、金额或指标值。
     */
    private double max(
            List<CdpWarehouseEnterpriseOlapDorisEvidenceClient.MetricsEndpointEvidence> endpoints,
            String... names) {
        double value = Double.NaN;
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (CdpWarehouseEnterpriseOlapDorisEvidenceClient.MetricsEndpointEvidence endpoint : endpoints) {
            for (String name : names) {
                double candidate = endpoint.value(name);
                // 校验关键输入和前置条件，避免无效状态继续进入主流程。
                if (known(candidate)) {
                    value = known(value) ? Math.max(value, candidate) : candidate;
                }
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return value;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 known 的布尔判断结果。
     */
    private boolean known(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 known 的布尔判断结果。
     */
    private boolean known(Double value) {
        return value != null && known(value.doubleValue());
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 positive 的布尔判断结果。
     */
    private boolean positive(Number value) {
        return value != null && value.doubleValue() > 0;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private boolean boundedConcurrency(Integer value) {
        return value != null && value > 0 && value < Integer.MAX_VALUE;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param view view 参数，用于 isExpired 流程中的校验、计算或对象转换。
     * @param evaluatedAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回布尔判断结果。
     */
    private boolean isExpired(EvidenceView view, LocalDateTime evaluatedAt) {
        LocalDateTime expiresAt = effectiveExpiresAt(view);
        return expiresAt == null || !expiresAt.isAfter(evaluatedAt);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param view view 参数，用于 effectiveExpiresAt 流程中的校验、计算或对象转换。
     * @return 返回 effectiveExpiresAt 流程生成的业务结果。
     */
    private LocalDateTime effectiveExpiresAt(EvidenceView view) {
        if (view.expiresAt() != null) {
            return view.expiresAt();
        }
        return view.measuredAt() == null
                ? null
                : view.measuredAt().plusMinutes(defaultMaxAgeMinutes(view.evidenceKey()));
    }

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @param key 业务键，用于在同一租户下定位资源。
     * @return 返回 default max age minutes 计算得到的数量、金额或指标值。
     */
    private long defaultMaxAgeMinutes(String key) {
        if ("ingestion_replay".equals(normalizeKey(key))) {
            return REPLAY_MAX_AGE_MINUTES;
        }
        return OPERATOR_DRILL_MAX_AGE_MINUTES;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param measuredAt 时间参数，用于计算窗口、过期或审计时间。
     * @param evaluatedAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 age minutes 计算得到的数量、金额或指标值。
     */
    private long ageMinutes(LocalDateTime measuredAt, LocalDateTime evaluatedAt) {
        if (measuredAt == null || evaluatedAt == null) {
            return Long.MAX_VALUE;
        }
        return Math.max(0L, Duration.between(measuredAt, evaluatedAt).toMinutes());
    }

    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param key 业务键，用于在同一租户下定位资源。
     * @param source source 参数，用于 fail 流程中的校验、计算或对象转换。
     * @param reason 原因说明，用于记录状态变化的业务依据。
     * @param evaluatedAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 fail 流程生成的业务结果。
     */
    private EvidenceView fail(Long tenantId, String key, String source, String reason, LocalDateTime evaluatedAt) {
        return view(null, tenantId, key, source, STATUS_FAIL, reason, evaluatedAt, evaluatedAt, "{}", "system");
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private EvidenceView toView(CdpWarehouseEnterpriseOlapEvidenceDO row) {
        return view(row.getId(), row.getTenantId(), row.getEvidenceKey(), row.getSource(), row.getStatus(),
                row.getReason(), row.getMeasuredAt(), row.getExpiresAt(), row.getEvidenceJson(), row.getCreatedBy());
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param view view 参数，用于 insertCollectedEvidence 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param id 业务对象 ID，用于定位具体记录。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param evidenceKey 业务键，用于在同一租户下定位资源。
     * @param source source 参数，用于 view 流程中的校验、计算或对象转换。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param reason 原因说明，用于记录状态变化的业务依据。
     * @param measuredAt 时间参数，用于计算窗口、过期或审计时间。
     * @param expiresAt 时间参数，用于计算窗口、过期或审计时间。
     * @param evidenceJson JSON 字符串，承载结构化配置或明细。
     * @param createdBy created by 参数，用于 view 流程中的校验、计算或对象转换。
     * @return 返回 view 流程生成的业务结果。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param rows rows 参数，用于 worstStatus 流程中的校验、计算或对象转换。
     * @return 返回 worst status 生成的文本或业务键。
     */
    private String worstStatus(List<EvidenceView> rows) {
        if (rows.stream().map(EvidenceView::status).anyMatch(STATUS_FAIL::equals)) {
            return STATUS_FAIL;
        }
        if (rows.stream().map(EvidenceView::status).anyMatch(STATUS_WARN::equals)) {
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
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeKey(String value) {
        return defaultString(value, "").toLowerCase(Locale.ROOT);
    }

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @param reason 原因说明，用于记录状态变化的业务依据。
     * @param key 业务键，用于在同一租户下定位资源。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回 default reason 生成的文本或业务键。
     */
    private String defaultReason(String reason, String key, String status) {
        if (hasText(reason)) {
            return reason.trim();
        }
        return normalizeKey(key) + " " + normalizeStatus(status).toLowerCase(Locale.ROOT);
    }

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultString 流程中的校验、计算或对象转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private String defaultString(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
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
     * @return 返回 now 流程生成的业务结果。
     */
    private LocalDateTime now() {
        return LocalDateTime.now(clock);
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
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param values values 参数，用于 safeList 流程中的校验、计算或对象转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    /**
     * EvidenceCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record EvidenceCommand(
            String evidenceKey,
            String status,
            String reason,
            LocalDateTime measuredAt,
            LocalDateTime expiresAt,
            String evidenceJson) {
    }

    /**
     * EvidenceBundle 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record EvidenceBundle(
            Long tenantId,
            String status,
            LocalDateTime evaluatedAt,
            List<EvidenceView> evidence) {
        public EvidenceBundle {
            evidence = evidence == null ? List.of() : List.copyOf(evidence);
        }
    }

    /**
     * EvidenceView 承载对应领域的业务规则、流程编排和结果转换。
     */
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
