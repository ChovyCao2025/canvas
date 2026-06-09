package org.chovy.canvas.domain.marketing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractDO;
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractProbeWindowStatsDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorAlertDO;
import org.chovy.canvas.dal.mapper.MarketingIntegrationContractMapper;
import org.chovy.canvas.dal.mapper.MarketingIntegrationContractProbeObservationMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorAlertMapper;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAlertFanoutService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * MarketingIntegrationContractSloService 编排 domain.marketing 场景的领域业务规则。
 */
@Slf4j
@Service
public class MarketingIntegrationContractSloService {

    static final String ALERT_TYPE = "INTEGRATION_CONTRACT_SLO_BURN_RATE";

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final List<SloRule> RULES = List.of(
            new SloRule("PAGE_FAST_BURN", "PAGE", "CRITICAL", 60, 5, 14.4),
            new SloRule("PAGE_SLOW_BURN", "PAGE", "HIGH", 360, 30, 6.0),
            new SloRule("TICKET_BURN", "TICKET", "HIGH", 4320, 360, 1.0));

    private final MarketingIntegrationContractMapper contractMapper;
    private final MarketingIntegrationContractProbeObservationMapper observationMapper;
    private final MarketingMonitorAlertMapper alertMapper;
    private final ObjectMapper objectMapper;
    private final MarketingMonitorAlertFanoutService fanoutService;
    private final Clock clock;

    /**
     * 创建 MarketingIntegrationContractSloService 实例并注入 domain.marketing 场景依赖。
     * @param contractMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param observationMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param alertMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param fanoutProvider fanout provider 参数，用于 MarketingIntegrationContractSloService 流程中的校验、计算或对象转换。
     */
    public MarketingIntegrationContractSloService(
            MarketingIntegrationContractMapper contractMapper,
            MarketingIntegrationContractProbeObservationMapper observationMapper,
            MarketingMonitorAlertMapper alertMapper,
            ObjectMapper objectMapper,
            ObjectProvider<MarketingMonitorAlertFanoutService> fanoutProvider) {
        this(contractMapper,
                observationMapper,
                alertMapper,
                objectMapper,
                fanoutProvider == null ? null : fanoutProvider.getIfAvailable(),
                Clock.systemDefaultZone());
    }

    /**
     * 执行 MarketingIntegrationContractSloService 流程，围绕 marketing integration contract slo service 完成校验、计算或结果组装。
     *
     * @param contractMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param observationMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param alertMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param fanoutService 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    MarketingIntegrationContractSloService(
            MarketingIntegrationContractMapper contractMapper,
            MarketingIntegrationContractProbeObservationMapper observationMapper,
            MarketingMonitorAlertMapper alertMapper,
            ObjectMapper objectMapper,
            MarketingMonitorAlertFanoutService fanoutService,
            Clock clock) {
        this.contractMapper = contractMapper;
        this.observationMapper = observationMapper;
        this.alertMapper = alertMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.fanoutService = fanoutService;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 查询业务列表，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 不直接修改业务状态，主要读取数据或执行本地规则计算。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param limit 返回或处理数量上限，方法内部会按业务最大值收敛
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<MarketingIntegrationContractSloEvaluationView> listProductionSloEvaluations(Long tenantId,
                                                                                            Integer limit) {
        Long scopedTenantId = safeTenantId(tenantId);
        int boundedLimit = boundedLimit(limit);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        List<MarketingIntegrationContractDO> rows = contractMapper.selectList(
                new LambdaQueryWrapper<MarketingIntegrationContractDO>()
                        .eq(MarketingIntegrationContractDO::getTenantId, scopedTenantId)
                        .eq(MarketingIntegrationContractDO::getEnvironment, "PRODUCTION")
                        .eq(MarketingIntegrationContractDO::getStatus, "ACTIVE")
                        .orderByDesc(MarketingIntegrationContractDO::getUpdatedAt)
                        .last("LIMIT " + boundedLimit));
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return (rows == null ? List.<MarketingIntegrationContractDO>of() : rows).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> "PRODUCTION".equalsIgnoreCase(defaultString(row.getEnvironment(), "")))
                .filter(row -> "ACTIVE".equalsIgnoreCase(defaultString(row.getStatus(), "")))
                .limit(boundedLimit)
                .map(row -> evaluateContract(scopedTenantId, row, sloProbeKey(row)))
                .toList();
    }

    /**
     * 执行业务操作 evaluateAndSyncContract，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param contract contract 参数，用于 evaluateAndSyncContract 流程中的校验、计算或对象转换。
     * @param probeKey 业务键，用于定位租户内的配置、资产或治理对象
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回本次处理的状态、计数、命中明细或治理结论，供控制器和调度任务判断后续动作
     */
    public MarketingIntegrationContractSloEvaluationView evaluateAndSyncContract(Long tenantId,
                                                                                 MarketingIntegrationContractDO contract,
                                                                                 String probeKey,
                                                                                 String actor) {
        MarketingIntegrationContractSloEvaluationView view =
                evaluateContract(safeTenantId(tenantId), contract, defaultString(probeKey, sloProbeKey(contract)));
        syncAlert(view, actor(actor));
        return view;
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param contract contract 参数，用于 evaluateContract 流程中的校验、计算或对象转换。
     * @param probeKey 业务键，用于在同一租户下定位资源。
     * @return 返回 evaluateContract 流程生成的业务结果。
     */
    private MarketingIntegrationContractSloEvaluationView evaluateContract(Long tenantId,
                                                                          MarketingIntegrationContractDO contract,
                                                                          String probeKey) {
        if (contract == null) {
            throw new IllegalArgumentException("integration contract is required");
        }
        Map<String, Object> metadata = map(contract.getMetadataJson());
        LocalDateTime generatedAt = now();
        double targetPercent = targetPercent(contract, metadata);
        double errorBudget = round4(Math.max(0.0001, (100.0 - targetPercent) / 100.0));
        int minLongSamples = intMetadata(metadata, "sloMinLongWindowSamples", 1);
        int minShortSamples = intMetadata(metadata, "sloMinShortWindowSamples", 1);
        List<MarketingIntegrationContractSloWindowView> windows = new ArrayList<>();
        for (SloRule rule : RULES) {
            MarketingIntegrationContractSloWindowView longWindow = window(
                    tenantId,
                    contract,
                    probeKey,
                    rule,
                    "long",
                    rule.longWindowMinutes(),
                    minLongSamples,
                    targetPercent,
                    errorBudget,
                    generatedAt);
            MarketingIntegrationContractSloWindowView shortWindow = window(
                    tenantId,
                    contract,
                    probeKey,
                    rule,
                    "short",
                    rule.shortWindowMinutes(),
                    minShortSamples,
                    targetPercent,
                    errorBudget,
                    generatedAt);
            windows.add(longWindow);
            windows.add(shortWindow);
            if (Boolean.TRUE.equals(longWindow.breached()) && Boolean.TRUE.equals(shortWindow.breached())) {
                return view(tenantId, contract, probeKey, rule.status(), rule.severity(), rule.key(),
                        /**
                         * 执行 reason 流程，围绕 reason 完成校验、计算或结果组装。
                         *
                         * @param contract contract 参数，用于 reason 流程中的校验、计算或对象转换。
                         * @param rule rule 参数，用于 reason 流程中的校验、计算或对象转换。
                         * @param longWindow long window 参数，用于 reason 流程中的校验、计算或对象转换。
                         * @param generatedAt 时间参数，用于计算窗口、过期或审计时间。
                         * @return 返回 reason 流程生成的业务结果。
                         */
                        targetPercent, errorBudget, reason(contract, rule, longWindow, shortWindow),
                        generatedAt, List.of(longWindow, shortWindow));
            }
        }
        boolean insufficient = windows.stream().noneMatch(MarketingIntegrationContractSloWindowView::sufficient);
        return view(tenantId, contract, probeKey, insufficient ? "INSUFFICIENT_DATA" : "OK", "INFO", null,
                targetPercent, errorBudget,
                insufficient ? "not enough production probe observations for SLO evaluation" : "SLO burn-rate is within policy",
                generatedAt, windows);
    }

    /**
     * 执行 window 流程，围绕 window 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param contract contract 参数，用于 window 流程中的校验、计算或对象转换。
     * @param probeKey 业务键，用于在同一租户下定位资源。
     * @param rule rule 参数，用于 window 流程中的校验、计算或对象转换。
     * @param windowKey 业务键，用于在同一租户下定位资源。
     * @param windowMinutes window minutes 参数，用于 window 流程中的校验、计算或对象转换。
     * @param minSamples min samples 参数，用于 window 流程中的校验、计算或对象转换。
     * @param targetPercent target percent 参数，用于 window 流程中的校验、计算或对象转换。
     * @param errorBudget error budget 参数，用于 window 流程中的校验、计算或对象转换。
     * @param generatedAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 window 流程生成的业务结果。
     */
    private MarketingIntegrationContractSloWindowView window(Long tenantId,
                                                            MarketingIntegrationContractDO contract,
                                                            String probeKey,
                                                            SloRule rule,
                                                            String windowKey,
                                                            int windowMinutes,
                                                            int minSamples,
                                                            double targetPercent,
                                                            double errorBudget,
                                                            LocalDateTime generatedAt) {
        // 准备本次处理所需的上下文和中间变量。
        LocalDateTime windowStart = generatedAt.minusMinutes(windowMinutes);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        MarketingIntegrationContractProbeWindowStatsDO stats = observationMapper.selectWindowStats(
                tenantId,
                contract.getId(),
                probeKey,
                windowStart);
        long total = stats == null || stats.getTotalCount() == null ? 0L : stats.getTotalCount();
        long bad = stats == null || stats.getBadCount() == null ? 0L : stats.getBadCount();
        double badRatio = total == 0 ? 0.0 : round4((double) bad / total);
        double burnRate = round2(badRatio / errorBudget);
        boolean sufficient = total >= minSamples;
        boolean breached = sufficient && burnRate >= rule.thresholdBurnRate();
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new MarketingIntegrationContractSloWindowView(
                rule.key(),
                windowKey,
                windowMinutes,
                total,
                bad,
                badRatio,
                burnRate,
                rule.thresholdBurnRate(),
                sufficient,
                breached,
                windowStart.toString(),
                generatedAt.toString());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param contract contract 参数，用于 view 流程中的校验、计算或对象转换。
     * @param probeKey 业务键，用于在同一租户下定位资源。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param severity severity 参数，用于 view 流程中的校验、计算或对象转换。
     * @param triggeredRuleKey 业务键，用于在同一租户下定位资源。
     * @param targetPercent target percent 参数，用于 view 流程中的校验、计算或对象转换。
     * @param errorBudget error budget 参数，用于 view 流程中的校验、计算或对象转换。
     * @param reason 原因说明，用于记录状态变化的业务依据。
     * @param generatedAt 时间参数，用于计算窗口、过期或审计时间。
     * @param windows windows 参数，用于 view 流程中的校验、计算或对象转换。
     * @return 返回 view 流程生成的业务结果。
     */
    private MarketingIntegrationContractSloEvaluationView view(Long tenantId,
                                                              MarketingIntegrationContractDO contract,
                                                              String probeKey,
                                                              String status,
                                                              String severity,
                                                              String triggeredRuleKey,
                                                              double targetPercent,
                                                              double errorBudget,
                                                              String reason,
                                                              LocalDateTime generatedAt,
                                                              List<MarketingIntegrationContractSloWindowView> windows) {
        return new MarketingIntegrationContractSloEvaluationView(
                tenantId,
                contract.getId(),
                contract.getContractKey(),
                contract.getDisplayName(),
                contract.getProviderFamily(),
                probeKey,
                status,
                severity,
                triggeredRuleKey,
                round2(targetPercent),
                errorBudget,
                reason,
                generatedAt.toString(),
                windows);
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param view view 参数，用于 syncAlert 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     */
    private void syncAlert(MarketingIntegrationContractSloEvaluationView view, String actor) {
        if (view == null || alertMapper == null) {
            return;
        }
        if ("PAGE".equals(view.status()) || "TICKET".equals(view.status())) {
            upsertAlert(view, actor);
        // 根据前序判断结果进入后续条件分支。
        } else if ("OK".equals(view.status())) {
            resolveAlert(view, actor);
        }
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param view view 参数，用于 upsertAlert 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     */
    private void upsertAlert(MarketingIntegrationContractSloEvaluationView view, String actor) {
        MarketingMonitorAlertDO existing = openAlert(view.tenantId(), view.contractKey());
        if (existing == null) {
            MarketingMonitorAlertDO row = new MarketingMonitorAlertDO();
            row.setTenantId(view.tenantId());
            row.setAlertType(ALERT_TYPE);
            row.setSeverity(view.severity());
            row.setStatus("OPEN");
            row.setScopeKey(view.contractKey());
            row.setDedupeKey(dedupeKey(view.contractKey()));
            row.setTitle("Marketing integration contract SLO burn-rate breached");
            row.setReason(view.reason());
            row.setItemCount(1);
            row.setWindowStart(triggerWindowStart(view));
            row.setWindowEnd(now());
            row.setMetadataJson(json(metadata(view)));
            row.setCreatedBy(actor);
            row.setCreatedAt(now());
            row.setUpdatedAt(now());
            try {
                alertMapper.insert(row);
                dispatch(row, actor);
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (DuplicateKeyException ex) {
                MarketingMonitorAlertDO concurrent = openAlert(view.tenantId(), view.contractKey());
                if (concurrent == null) {
                    throw ex;
                }
                updateAlert(concurrent, view);
            }
            return;
        }
        updateAlert(existing, view);
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param existing existing 参数，用于 updateAlert 流程中的校验、计算或对象转换。
     * @param view view 参数，用于 updateAlert 流程中的校验、计算或对象转换。
     */
    private void updateAlert(MarketingMonitorAlertDO existing, MarketingIntegrationContractSloEvaluationView view) {
        existing.setSeverity(view.severity());
        existing.setDedupeKey(dedupeKey(view.contractKey()));
        existing.setReason(view.reason());
        existing.setItemCount(existing.getItemCount() == null ? 1 : existing.getItemCount() + 1);
        existing.setWindowStart(triggerWindowStart(view));
        existing.setWindowEnd(now());
        existing.setMetadataJson(json(metadata(view)));
        existing.setUpdatedAt(now());
        alertMapper.updateById(existing);
    }

    /**
     * 解析业务依赖或上下文值。
     *
     * @param view view 参数，用于 resolveAlert 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     */
    private void resolveAlert(MarketingIntegrationContractSloEvaluationView view, String actor) {
        MarketingMonitorAlertDO existing = openAlert(view.tenantId(), view.contractKey());
        if (existing == null) {
            return;
        }
        LocalDateTime resolvedAt = now();
        existing.setStatus("RESOLVED");
        existing.setDedupeKey(null);
        existing.setResolvedBy(actor);
        existing.setResolvedAt(resolvedAt);
        existing.setWindowEnd(resolvedAt);
        Map<String, Object> metadata = new LinkedHashMap<>(map(existing.getMetadataJson()));
        metadata.put("recoveredAt", resolvedAt.toString());
        metadata.put("recoveredStatus", view.status());
        existing.setMetadataJson(json(metadata));
        existing.setUpdatedAt(resolvedAt);
        alertMapper.updateById(existing);
    }

    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param contractKey 业务键，用于在同一租户下定位资源。
     * @return 返回 openAlert 流程生成的业务结果。
     */
    private MarketingMonitorAlertDO openAlert(Long tenantId, String contractKey) {
        return alertMapper.selectOne(new LambdaQueryWrapper<MarketingMonitorAlertDO>()
                .eq(MarketingMonitorAlertDO::getTenantId, tenantId)
                .eq(MarketingMonitorAlertDO::getAlertType, ALERT_TYPE)
                .eq(MarketingMonitorAlertDO::getStatus, "OPEN")
                .eq(MarketingMonitorAlertDO::getDedupeKey, dedupeKey(contractKey))
                .last("LIMIT 1"));
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param actor 操作人标识，用于审计和权限判断。
     */
    private void dispatch(MarketingMonitorAlertDO row, String actor) {
        if (fanoutService == null) {
            return;
        }
        try {
            fanoutService.dispatchAlert(row.getTenantId(), row, actor);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException ex) {
            log.warn("[MARKETING-INTEGRATION] SLO burn-rate fanout skipped alert={} error={}",
                    row.getId(), ex.getMessage());
        }
    }

    /**
     * 执行 metadata 流程，围绕 metadata 完成校验、计算或结果组装。
     *
     * @param view view 参数，用于 metadata 流程中的校验、计算或对象转换。
     * @return 返回 metadata 流程生成的业务结果。
     */
    private Map<String, Object> metadata(MarketingIntegrationContractSloEvaluationView view) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("contractId", view.contractId());
        metadata.put("contractKey", view.contractKey());
        metadata.put("displayName", view.displayName());
        metadata.put("providerFamily", view.providerFamily());
        metadata.put("probeKey", view.probeKey());
        metadata.put("status", view.status());
        metadata.put("severity", view.severity());
        metadata.put("triggeredRuleKey", view.triggeredRuleKey());
        metadata.put("targetPercent", view.targetPercent());
        metadata.put("errorBudget", view.errorBudget());
        metadata.put("windows", view.windows());
        metadata.put("generatedAt", view.generatedAt());
        return metadata;
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param view view 参数，用于 triggerWindowStart 流程中的校验、计算或对象转换。
     * @return 返回 triggerWindowStart 流程生成的业务结果。
     */
    private LocalDateTime triggerWindowStart(MarketingIntegrationContractSloEvaluationView view) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return view.windows().stream()
                .map(MarketingIntegrationContractSloWindowView::windowStart)
                .map(LocalDateTime::parse)
                .min(LocalDateTime::compareTo)
                .orElse(now());
    }

    /**
     * 执行 reason 流程，围绕 reason 完成校验、计算或结果组装。
     *
     * @param contract contract 参数，用于 reason 流程中的校验、计算或对象转换。
     * @param rule rule 参数，用于 reason 流程中的校验、计算或对象转换。
     * @param longWindow long window 参数，用于 reason 流程中的校验、计算或对象转换。
     * @param shortWindow short window 参数，用于 reason 流程中的校验、计算或对象转换。
     * @return 返回 reason 生成的文本或业务键。
     */
    private String reason(MarketingIntegrationContractDO contract,
                          SloRule rule,
                          MarketingIntegrationContractSloWindowView longWindow,
                          MarketingIntegrationContractSloWindowView shortWindow) {
        return trimToLimit(contract.getContractKey() + " breached " + rule.key()
                /**
                 * 执行 formatRate 流程，围绕 format rate 完成校验、计算或结果组装。
                 *
                 * @return 返回 formatRate 流程生成的业务结果。
                 */
                + ": " + formatRate(longWindow.burnRate()) + " burn over " + longWindow.windowMinutes() + "m"
                /**
                 * 执行 formatRate 流程，围绕 format rate 完成校验、计算或结果组装。
                 *
                 * @return 返回 formatRate 流程生成的业务结果。
                 */
                + " and " + formatRate(shortWindow.burnRate()) + " over " + shortWindow.windowMinutes() + "m", 1000);
    }

    /**
     * 执行 formatRate 流程，围绕 format rate 完成校验、计算或结果组装。
     *
     * @param burnRate burn rate 参数，用于 formatRate 流程中的校验、计算或对象转换。
     * @return 返回 format rate 生成的文本或业务键。
     */
    private static String formatRate(Double burnRate) {
        return String.format(Locale.ROOT, "%.2fx", burnRate == null ? 0.0 : burnRate);
    }

    /**
     * 执行 sloProbeKey 流程，围绕 slo probe key 完成校验、计算或结果组装。
     *
     * @param contract contract 参数，用于 sloProbeKey 流程中的校验、计算或对象转换。
     * @return 返回 slo probe key 生成的文本或业务键。
     */
    private String sloProbeKey(MarketingIntegrationContractDO contract) {
        return defaultString(stringMetadata(map(contract == null ? null : contract.getMetadataJson()), "sloProbeKey"),
                MarketingIntegrationContractProbeAutomationService.PROBE_KEY);
    }

    /**
     * 执行 targetPercent 流程，围绕 target percent 完成校验、计算或结果组装。
     *
     * @param contract contract 参数，用于 targetPercent 流程中的校验、计算或对象转换。
     * @param String string 参数，用于 targetPercent 流程中的校验、计算或对象转换。
     * @param metadata metadata 参数，用于 targetPercent 流程中的校验、计算或对象转换。
     * @return 返回 target percent 计算得到的数量、金额或指标值。
     */
    private double targetPercent(MarketingIntegrationContractDO contract, Map<String, Object> metadata) {
        Double configured = doubleMetadata(metadata, "sloTargetPercent");
        if (configured != null) {
            return Math.max(50.0, Math.min(99.99, configured));
        }
        String tier = normalizeUpper(contract.getSlaTier());
        return switch (tier) {
            case "CRITICAL", "BUSINESS_CRITICAL" -> 99.9;
            case "BEST_EFFORT" -> 95.0;
            default -> 99.0;
        };
    }

    /**
     * 执行 doubleMetadata 流程，围绕 double metadata 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 doubleMetadata 流程中的校验、计算或对象转换。
     * @param metadata metadata 参数，用于 doubleMetadata 流程中的校验、计算或对象转换。
     * @param key 业务键，用于在同一租户下定位资源。
     * @return 返回 double metadata 计算得到的数量、金额或指标值。
     */
    private Double doubleMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Double.parseDouble(text.trim());
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    /**
     * 执行 intMetadata 流程，围绕 int metadata 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 intMetadata 流程中的校验、计算或对象转换。
     * @param metadata metadata 参数，用于 intMetadata 流程中的校验、计算或对象转换。
     * @param key 业务键，用于在同一租户下定位资源。
     * @param fallback fallback 参数，用于 intMetadata 流程中的校验、计算或对象转换。
     * @return 返回 int metadata 计算得到的数量、金额或指标值。
     */
    private int intMetadata(Map<String, Object> metadata, String key, int fallback) {
        Double value = doubleMetadata(metadata, key);
        if (value == null || value < 1) {
            return fallback;
        }
        return Math.min(10_000, value.intValue());
    }

    /**
     * 执行 stringMetadata 流程，围绕 string metadata 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 stringMetadata 流程中的校验、计算或对象转换。
     * @param metadata metadata 参数，用于 stringMetadata 流程中的校验、计算或对象转换。
     * @param key 业务键，用于在同一租户下定位资源。
     * @return 返回 string metadata 生成的文本或业务键。
     */
    private String stringMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param String string 参数，用于 json 流程中的校验、计算或对象转换。
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 json 生成的文本或业务键。
     */
    private String json(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("integration SLO alert metadata must be JSON serializable", ex);
        }
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回组装或转换后的结果对象。
     */
    private Map<String, Object> map(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    /**
     * 执行 now 流程，围绕 now 完成校验、计算或结果组装。
     *
     * @return 返回 now 流程生成的业务结果。
     */
    private LocalDateTime now() {
        return LocalDateTime.now(clock).withNano(0);
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 safe tenant id 计算得到的数量、金额或指标值。
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static int boundedLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    /**
     * 解析操作人标识。
     *
     * @param actor 操作人标识，用于审计和权限判断。
     * @return 返回 actor 生成的文本或业务键。
     */
    private static String actor(String actor) {
        String trimmed = actor == null ? "" : actor.trim();
        return trimmed.isBlank() ? "marketing-integration-slo-evaluator" : trimmed;
    }

    /**
     * 执行 dedupeKey 流程，围绕 dedupe key 完成校验、计算或结果组装。
     *
     * @param contractKey 业务键，用于在同一租户下定位资源。
     * @return 返回 dedupe key 生成的文本或业务键。
     */
    private static String dedupeKey(String contractKey) {
        return trimToLimit(ALERT_TYPE.toLowerCase(Locale.ROOT) + ":" + contractKey, 256);
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeUpper(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? "" : trimmed.toUpperCase(Locale.ROOT);
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultString 流程中的校验、计算或对象转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private static String defaultString(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed;
    }

    /**
     * 执行 round2 流程，围绕 round2 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 round2 计算得到的数量、金额或指标值。
     */
    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /**
     * 执行 round4 流程，围绕 round4 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 round4 计算得到的数量、金额或指标值。
     */
    private static double round4(double value) {
        return Math.round(value * 10_000.0) / 10_000.0;
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String trimToLimit(String value, int limit) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            return null;
        }
        return trimmed.length() <= limit ? trimmed : trimmed.substring(0, limit);
    }

    /**
     * SloRule 数据记录。
     */
    private record SloRule(
            String key,
            String status,
            String severity,
            Integer longWindowMinutes,
            Integer shortWindowMinutes,
            Double thresholdBurnRate) {
    }
}
