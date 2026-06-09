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
/**
 * CdpWarehouseReadinessService 承载对应领域的业务规则、流程编排和结果转换。
 */
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
    /**
     * 初始化 CdpWarehouseReadinessService 实例。
     *
     * @param operationsService 依赖组件，用于完成数据访问或外部能力调用。
     * @param realtimePipelineService 时间参数，用于计算窗口、过期或审计时间。
     * @param realtimeJobControlServiceProvider 时间参数，用于计算窗口、过期或审计时间。
     * @param incidentService 依赖组件，用于完成数据访问或外部能力调用。
     * @param audienceMaterializationOperationsService 依赖组件，用于完成数据访问或外部能力调用。
     * @param biDatasourceHealthProviderProvider bi datasource health provider provider 参数，用于 CdpWarehouseReadinessService 流程中的校验、计算或对象转换。
     * @param sloPolicyServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     */
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

    /**
     * 初始化 CdpWarehouseReadinessService 实例。
     *
     * @param operationsService 依赖组件，用于完成数据访问或外部能力调用。
     * @param realtimePipelineService 时间参数，用于计算窗口、过期或审计时间。
     * @param incidentService 依赖组件，用于完成数据访问或外部能力调用。
     * @param audienceMaterializationOperationsService 依赖组件，用于完成数据访问或外部能力调用。
     * @param biDatasourceHealthProvider bi datasource health provider 参数，用于 CdpWarehouseReadinessService 流程中的校验、计算或对象转换。
     */
    CdpWarehouseReadinessService(
            CdpWarehouseOperationsService operationsService,
            CdpWarehouseRealtimePipelineService realtimePipelineService,
            CdpWarehouseIncidentService incidentService,
            AudienceMaterializationOperationsService audienceMaterializationOperationsService,
            BiDatasourceHealthProvider biDatasourceHealthProvider) {
        this(operationsService, realtimePipelineService, null, incidentService,
                audienceMaterializationOperationsService, biDatasourceHealthProvider, null);
    }

    /**
     * 初始化 CdpWarehouseReadinessService 实例。
     *
     * @param operationsService 依赖组件，用于完成数据访问或外部能力调用。
     * @param realtimePipelineService 时间参数，用于计算窗口、过期或审计时间。
     * @param incidentService 依赖组件，用于完成数据访问或外部能力调用。
     * @param audienceMaterializationOperationsService 依赖组件，用于完成数据访问或外部能力调用。
     * @param biDatasourceHealthProvider bi datasource health provider 参数，用于 CdpWarehouseReadinessService 流程中的校验、计算或对象转换。
     * @param sloPolicyService 依赖组件，用于完成数据访问或外部能力调用。
     */
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

    /**
     * 初始化 CdpWarehouseReadinessService 实例。
     *
     * @param operationsService 依赖组件，用于完成数据访问或外部能力调用。
     * @param realtimePipelineService 时间参数，用于计算窗口、过期或审计时间。
     * @param realtimeJobControlService 时间参数，用于计算窗口、过期或审计时间。
     * @param incidentService 依赖组件，用于完成数据访问或外部能力调用。
     * @param audienceMaterializationOperationsService 依赖组件，用于完成数据访问或外部能力调用。
     * @param biDatasourceHealthProvider bi datasource health provider 参数，用于 CdpWarehouseReadinessService 流程中的校验、计算或对象转换。
     * @param sloPolicyService 依赖组件，用于完成数据访问或外部能力调用。
     */
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

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 readiness 流程生成的业务结果。
     */
    public ReadinessSummary readiness(Long tenantId) {
        // 准备本次处理所需的上下文和中间变量。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @param sloPolicy slo policy 参数，用于 offline 流程中的校验、计算或对象转换。
     * @return 返回 offline 流程生成的业务结果。
     */
    private OfflineReadiness offline(Long tenantId,
                                     LocalDateTime now,
                                     CdpWarehouseSloPolicyService.SloPolicyView sloPolicy) {
        try {
            CdpWarehouseOperationsService.WarehouseStatus status =
                    operationsService.status(tenantId, DEFAULT_RECENT_LIMIT);
            List<CdpWarehouseOperationsService.RunRow> runs =
                    status.recentRuns() == null ? List.of() : status.recentRuns();
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            long failed = runs.stream().filter(run -> isFailed(run.status())).count();
            long running = runs.stream().filter(run -> "RUNNING".equalsIgnoreCase(nullToEmpty(run.status()))).count();
            int watermarkCount = status.watermarks() == null ? 0 : status.watermarks().size();
            Long latestRunAgeMinutes = ageMinutes(latestRunAt(runs), now);
            Long latestWatermarkLagMinutes = ageMinutes(latestWatermarkAt(status.watermarks()), now);
            String sectionStatus;
            String reason;
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
            // 汇总前面计算出的状态和明细，返回给调用方。
            return new OfflineReadiness(FAIL, "offline sync status failed: " + e.getMessage(),
                    0, 0, 0, 0);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 realtime 流程生成的业务结果。
     */
    private RealtimeReadiness realtime(Long tenantId) {
        // 准备本次处理所需的上下文和中间变量。
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
            // 汇总前面计算出的状态和明细，返回给调用方。
            return new RealtimeReadiness(FAIL, "realtime status failed: " + e.getMessage(),
                    0, 0, 0, 0, 0);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 incidents 流程生成的业务结果。
     */
    private IncidentReadiness incidents(Long tenantId) {
        try {
            List<CdpWarehouseIncidentService.IncidentView> open =
                    incidentService.listIncidents(tenantId, "OPEN", 100);
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (open == null) {
                open = List.of();
            }
            // 遍历候选数据并按业务规则筛选、转换或聚合。
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
            // 汇总前面计算出的状态和明细，返回给调用方。
            return new IncidentReadiness(FAIL, "incident status failed: " + e.getMessage(),
                    0, 0, 0);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 bi 流程生成的业务结果。
     */
    private BiReadiness bi() {
        try {
            List<BiDatasourceHealth> rows = biDatasourceHealthProvider.health();
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (rows == null) {
                rows = List.of();
            }
            // 遍历候选数据并按业务规则筛选、转换或聚合。
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
            // 汇总前面计算出的状态和明细，返回给调用方。
            return new BiReadiness(FAIL, "BI datasource health failed: " + e.getMessage(), 0, 0, 0);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @param sloPolicy slo policy 参数，用于 audienceMaterialization 流程中的校验、计算或对象转换。
     * @return 返回 audienceMaterialization 流程生成的业务结果。
     */
    private AudienceMaterializationReadiness audienceMaterialization(
            Long tenantId,
            LocalDateTime now,
            CdpWarehouseSloPolicyService.SloPolicyView sloPolicy) {
        try {
            List<AudienceMaterializationOperationsService.RunView> runs =
                    audienceMaterializationOperationsService.recentRuns(tenantId, null, null, DEFAULT_RECENT_LIMIT);
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (runs == null) {
                runs = List.of();
            }
            // 遍历候选数据并按业务规则筛选、转换或聚合。
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
            // 汇总前面计算出的状态和明细，返回给调用方。
            return new AudienceMaterializationReadiness(FAIL,
                    "audience materialization status failed: " + e.getMessage(), 0, 0, 0);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 effectivePolicy 流程生成的业务结果。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param sections sections 参数，用于 overallStatus 流程中的校验、计算或对象转换。
     * @return 返回 overall status 生成的文本或业务键。
     */
    private String overallStatus(List<ReadinessSection> sections) {
        if (sections.stream().anyMatch(section -> FAIL.equals(section.status()))) {
            return FAIL;
        }
        if (sections.stream().anyMatch(section -> WARN.equals(section.status()))) {
            return WARN;
        }
        return PASS;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回布尔判断结果。
     */
    private boolean isFailed(String status) {
        String normalized = nullToEmpty(status).toUpperCase(Locale.ROOT);
        return FAIL.equals(normalized) || "FAILED".equals(normalized) || "ERROR".equals(normalized);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param runs runs 参数，用于 latestRunAt 流程中的校验、计算或对象转换。
     * @return 返回 latestRunAt 流程生成的业务结果。
     */
    private LocalDateTime latestRunAt(List<CdpWarehouseOperationsService.RunRow> runs) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return (runs == null ? List.<CdpWarehouseOperationsService.RunRow>of() : runs).stream()
                .map(run -> latest(run.finishedAt(), run.startedAt(), run.windowEnd(), run.windowStart()))
                .filter(value -> value != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param watermarks watermarks 参数，用于 latestWatermarkAt 流程中的校验、计算或对象转换。
     * @return 返回 latestWatermarkAt 流程生成的业务结果。
     */
    private LocalDateTime latestWatermarkAt(List<CdpWarehouseOperationsService.WatermarkRow> watermarks) {
        return (watermarks == null ? List.<CdpWarehouseOperationsService.WatermarkRow>of() : watermarks).stream()
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .map(row -> latest(row.watermarkTime(), row.updatedAt()))
                .filter(value -> value != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param runs runs 参数，用于 latestMaterializationRunAt 流程中的校验、计算或对象转换。
     * @return 返回 latestMaterializationRunAt 流程生成的业务结果。
     */
    private LocalDateTime latestMaterializationRunAt(List<AudienceMaterializationOperationsService.RunView> runs) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return (runs == null ? List.<AudienceMaterializationOperationsService.RunView>of() : runs).stream()
                .map(run -> latest(run.finishedAt(), run.startedAt()))
                .filter(value -> value != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param values values 参数，用于 latest 流程中的校验、计算或对象转换。
     * @return 返回 latest 流程生成的业务结果。
     */
    private LocalDateTime latest(LocalDateTime... values) {
        LocalDateTime latest = null;
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (values == null) {
            return null;
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (LocalDateTime value : values) {
            if (value != null && (latest == null || value.isAfter(latest))) {
                latest = value;
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return latest;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param observedAt 时间参数，用于计算窗口、过期或审计时间。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 age minutes 计算得到的数量、金额或指标值。
     */
    private Long ageMinutes(LocalDateTime observedAt, LocalDateTime now) {
        if (observedAt == null || now == null) {
            return null;
        }
        return Math.max(0L, Duration.between(observedAt, now).toMinutes());
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param threshold threshold 参数，用于 thresholdReached 流程中的校验、计算或对象转换。
     * @return 返回 threshold reached 的布尔判断结果。
     */
    private boolean thresholdReached(Long value, int threshold) {
        return value != null && value >= threshold;
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
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 null to empty 生成的文本或业务键。
     */
    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * ReadinessSummary 承载对应领域的业务规则、流程编排和结果转换。
     */
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

    /**
     * ReadinessSection 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record ReadinessSection(String key, String status, String reason) {
    }

    /**
     * OfflineReadiness 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record OfflineReadiness(
            String status,
            String reason,
            int recentRunCount,
            long failedRunCount,
            long runningRunCount,
            int watermarkCount) {
    }

    /**
     * RealtimeReadiness 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record RealtimeReadiness(
            String status,
            String reason,
            int pipelineCount,
            long passedCount,
            long warnedCount,
            long failedCount,
            int jobCount) {

        /**
         * 初始化 RealtimeReadiness 实例。
         *
         * @param status 业务状态，用于筛选或推进状态流转。
         * @param reason 原因说明，用于记录状态变化的业务依据。
         * @param pipelineCount pipeline count 参数，用于 RealtimeReadiness 流程中的校验、计算或对象转换。
         * @param passedCount passed count 参数，用于 RealtimeReadiness 流程中的校验、计算或对象转换。
         * @param warnedCount warned count 参数，用于 RealtimeReadiness 流程中的校验、计算或对象转换。
         * @param failedCount failed count 参数，用于 RealtimeReadiness 流程中的校验、计算或对象转换。
         */
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

    /**
     * IncidentReadiness 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record IncidentReadiness(
            String status,
            String reason,
            int openCount,
            long criticalCount,
            long warningCount) {
    }

    /**
     * BiReadiness 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record BiReadiness(
            String status,
            String reason,
            int datasourceCount,
            long availableCount,
            long unavailableCount) {
    }

    /**
     * AudienceMaterializationReadiness 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record AudienceMaterializationReadiness(
            String status,
            String reason,
            int recentRunCount,
            long successCount,
            long failedCount) {
    }
}
