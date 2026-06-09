package org.chovy.canvas.domain.marketing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorAlertDO;
import org.chovy.canvas.dal.mapper.MarketingMonitorAlertMapper;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAlertFanoutService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * MarketingIntegrationContractProbeAlertService 编排 domain.marketing 场景的领域业务规则。
 */
@Slf4j
@Service
public class MarketingIntegrationContractProbeAlertService {

    static final String ALERT_TYPE = "INTEGRATION_CONTRACT_PROBE_FAILURE";

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final MarketingMonitorAlertMapper alertMapper;
    private final ObjectMapper objectMapper;
    private final MarketingMonitorAlertFanoutService fanoutService;
    private final Clock clock;

    /**
     * 创建 MarketingIntegrationContractProbeAlertService 实例并注入 domain.marketing 场景依赖。
     * @param alertMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param fanoutProvider fanout provider 参数，用于 MarketingIntegrationContractProbeAlertService 流程中的校验、计算或对象转换。
     */
    public MarketingIntegrationContractProbeAlertService(
            MarketingMonitorAlertMapper alertMapper,
            ObjectMapper objectMapper,
            ObjectProvider<MarketingMonitorAlertFanoutService> fanoutProvider) {
        this(alertMapper,
                objectMapper,
                fanoutProvider == null ? null : fanoutProvider.getIfAvailable(),
                Clock.systemDefaultZone());
    }

    /**
     * 执行 MarketingIntegrationContractProbeAlertService 流程，围绕 marketing integration contract probe alert service 完成校验、计算或结果组装。
     *
     * @param alertMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param fanoutService 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    MarketingIntegrationContractProbeAlertService(
            MarketingMonitorAlertMapper alertMapper,
            ObjectMapper objectMapper,
            MarketingMonitorAlertFanoutService fanoutService,
            Clock clock) {
        this.alertMapper = alertMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.fanoutService = fanoutService;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 执行业务操作 syncProbeResult，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param contract contract 参数，用于 syncProbeResult 流程中的校验、计算或对象转换。
     * @param probeRun probe run 参数，用于 syncProbeResult 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     */
    public void syncProbeResult(Long tenantId,
                                MarketingIntegrationContractDO contract,
                                MarketingIntegrationContractProbeRunView probeRun,
                                String actor) {
        if (contract == null) {
            return;
        }
        syncProbeResult(tenantId, snapshot(contract), probeRun, actor);
    }

    /**
     * 执行业务操作 syncProbeResult，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param contract contract 参数，用于 syncProbeResult 流程中的校验、计算或对象转换。
     * @param probeRun probe run 参数，用于 syncProbeResult 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     */
    public void syncProbeResult(Long tenantId,
                                MarketingIntegrationContractView contract,
                                MarketingIntegrationContractProbeRunView probeRun,
                                String actor) {
        if (contract == null) {
            return;
        }
        syncProbeResult(tenantId, snapshot(contract), probeRun, actor);
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param contract contract 参数，用于 syncProbeResult 流程中的校验、计算或对象转换。
     * @param probeRun probe run 参数，用于 syncProbeResult 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     */
    private void syncProbeResult(Long tenantId,
                                 ContractSnapshot contract,
                                 MarketingIntegrationContractProbeRunView probeRun,
                                 String actor) {
        if (probeRun == null || contract == null || !"PRODUCTION".equals(contract.environment())) {
            return;
        }
        String status = normalizeUpper(probeRun.status());
        if ("FAIL".equals(status)) {
            upsertOpenFailureAlert(safeTenantId(tenantId), contract, probeRun, actor(actor));
        // 根据前序判断结果进入后续条件分支。
        } else if ("PASS".equals(status)) {
            resolveOpenFailureAlert(safeTenantId(tenantId), contract, probeRun, actor(actor));
        }
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param contract contract 参数，用于 upsertOpenFailureAlert 流程中的校验、计算或对象转换。
     * @param probeRun probe run 参数，用于 upsertOpenFailureAlert 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     */
    private void upsertOpenFailureAlert(Long tenantId,
                                        ContractSnapshot contract,
                                        MarketingIntegrationContractProbeRunView probeRun,
                                        String actor) {
        LocalDateTime observedAt = observedAt(probeRun);
        MarketingMonitorAlertDO existing = openAlert(tenantId, contract.contractKey());
        if (existing == null) {
            MarketingMonitorAlertDO row = new MarketingMonitorAlertDO();
            row.setTenantId(tenantId);
            row.setAlertType(ALERT_TYPE);
            row.setSeverity(severity(contract.slaTier()));
            row.setStatus("OPEN");
            row.setScopeKey(contract.contractKey());
            row.setDedupeKey(dedupeKey(contract));
            row.setTitle("Marketing integration contract probe failed");
            row.setReason(failureReason(contract, probeRun));
            row.setItemCount(1);
            row.setWindowStart(observedAt);
            row.setWindowEnd(observedAt);
            row.setMetadataJson(json(failureMetadata(contract, probeRun)));
            row.setCreatedBy(actor);
            row.setCreatedAt(now());
            row.setUpdatedAt(now());
            try {
                alertMapper.insert(row);
                dispatch(row, actor);
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (DuplicateKeyException ex) {
                MarketingMonitorAlertDO concurrent = openAlert(tenantId, contract.contractKey());
                if (concurrent == null) {
                    throw ex;
                }
                updateOpenFailureAlert(concurrent, contract, probeRun, observedAt);
            }
            return;
        }
        updateOpenFailureAlert(existing, contract, probeRun, observedAt);
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param existing existing 参数，用于 updateOpenFailureAlert 流程中的校验、计算或对象转换。
     * @param contract contract 参数，用于 updateOpenFailureAlert 流程中的校验、计算或对象转换。
     * @param probeRun probe run 参数，用于 updateOpenFailureAlert 流程中的校验、计算或对象转换。
     * @param observedAt 时间参数，用于计算窗口、过期或审计时间。
     */
    private void updateOpenFailureAlert(MarketingMonitorAlertDO existing,
                                        ContractSnapshot contract,
                                        MarketingIntegrationContractProbeRunView probeRun,
                                        LocalDateTime observedAt) {
        // 准备本次处理所需的上下文和中间变量。
        existing.setSeverity(severity(contract.slaTier()));
        existing.setDedupeKey(dedupeKey(contract));
        existing.setReason(failureReason(contract, probeRun));
        existing.setItemCount(existing.getItemCount() == null ? 1 : existing.getItemCount() + 1);
        existing.setWindowEnd(observedAt);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put("lastProbeRunId", probeRun.id());
        updates.put("lastProbeStatus", probeRun.status());
        updates.put("lastHttpStatusCode", probeRun.httpStatusCode());
        updates.put("lastLatencyMs", probeRun.latencyMs());
        updates.put("lastObservedAt", observedAt.toString());
        updates.put("lastErrorMessage", defaultString(probeRun.errorMessage(), probeRun.summary()));
        existing.setMetadataJson(json(merge(existing.getMetadataJson(), updates)));
        existing.setUpdatedAt(now());
        alertMapper.updateById(existing);
    }

    /**
     * 解析业务依赖或上下文值。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param contract contract 参数，用于 resolveOpenFailureAlert 流程中的校验、计算或对象转换。
     * @param probeRun probe run 参数，用于 resolveOpenFailureAlert 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     */
    private void resolveOpenFailureAlert(Long tenantId,
                                         ContractSnapshot contract,
                                         MarketingIntegrationContractProbeRunView probeRun,
                                         String actor) {
        MarketingMonitorAlertDO existing = openAlert(tenantId, contract.contractKey());
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (existing == null) {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        LocalDateTime resolvedAt = now();
        existing.setStatus("RESOLVED");
        existing.setDedupeKey(null);
        existing.setResolvedBy(actor);
        existing.setResolvedAt(resolvedAt);
        existing.setWindowEnd(observedAt(probeRun));
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put("recoveredProbeRunId", probeRun.id());
        updates.put("recoveredHttpStatusCode", probeRun.httpStatusCode());
        updates.put("recoveredLatencyMs", probeRun.latencyMs());
        updates.put("recoveredAt", resolvedAt.toString());
        existing.setMetadataJson(json(merge(existing.getMetadataJson(), updates)));
        existing.setUpdatedAt(resolvedAt);
        alertMapper.updateById(existing);
    }

    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param scopeKey 业务键，用于在同一租户下定位资源。
     * @return 返回 openAlert 流程生成的业务结果。
     */
    private MarketingMonitorAlertDO openAlert(Long tenantId, String scopeKey) {
        return alertMapper.selectOne(new LambdaQueryWrapper<MarketingMonitorAlertDO>()
                .eq(MarketingMonitorAlertDO::getTenantId, tenantId)
                .eq(MarketingMonitorAlertDO::getAlertType, ALERT_TYPE)
                .eq(MarketingMonitorAlertDO::getStatus, "OPEN")
                .eq(MarketingMonitorAlertDO::getScopeKey, scopeKey)
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
            log.warn("[MARKETING-INTEGRATION] probe alert fanout skipped alert={} error={}",
                    row.getId(), ex.getMessage());
        }
    }

    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param contract contract 参数，用于 failureMetadata 流程中的校验、计算或对象转换。
     * @param probeRun probe run 参数，用于 failureMetadata 流程中的校验、计算或对象转换。
     * @return 返回 failureMetadata 流程生成的业务结果。
     */
    private Map<String, Object> failureMetadata(ContractSnapshot contract,
                                                MarketingIntegrationContractProbeRunView probeRun) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("contractId", contract.id());
        metadata.put("contractKey", contract.contractKey());
        metadata.put("displayName", contract.displayName());
        metadata.put("providerFamily", contract.providerFamily());
        metadata.put("ownerTeam", contract.ownerTeam());
        metadata.put("slaTier", contract.slaTier());
        metadata.put("apiRoot", contract.apiRoot());
        metadata.put("probeRunId", probeRun.id());
        metadata.put("probeKey", probeRun.probeKey());
        metadata.put("probeStatus", probeRun.status());
        metadata.put("httpStatusCode", probeRun.httpStatusCode());
        metadata.put("latencyMs", probeRun.latencyMs());
        metadata.put("problemTypeUri", probeRun.problemTypeUri());
        metadata.put("observedAt", observedAt(probeRun).toString());
        metadata.put("evidence", probeRun.evidence());
        return metadata;
    }

    /**
     * 处理集合、映射或字段拷贝逻辑。
     *
     * @param existingJson JSON 字符串，承载结构化配置或明细。
     * @param String string 参数，用于 merge 流程中的校验、计算或对象转换。
     * @param updates 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 merge 流程生成的业务结果。
     */
    private Map<String, Object> merge(String existingJson, Map<String, Object> updates) {
        Map<String, Object> merged = new LinkedHashMap<>(map(existingJson));
        updates.forEach((key, value) -> {
            if (value != null) {
                merged.put(key, value);
            }
        });
        return merged;
    }

    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param contract contract 参数，用于 failureReason 流程中的校验、计算或对象转换。
     * @param probeRun probe run 参数，用于 failureReason 流程中的校验、计算或对象转换。
     * @return 返回 failure reason 生成的文本或业务键。
     */
    private String failureReason(ContractSnapshot contract, MarketingIntegrationContractProbeRunView probeRun) {
        String message = defaultString(probeRun.errorMessage(), defaultString(probeRun.summary(), "probe failed"));
        return trimToLimit(contract.contractKey() + " failed " + probeRun.probeKey() + ": " + message, 1000);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 snapshot 流程生成的业务结果。
     */
    private ContractSnapshot snapshot(MarketingIntegrationContractDO row) {
        return new ContractSnapshot(
                row.getId(),
                safeTenantId(row.getTenantId()),
                trimToLimit(row.getContractKey(), 128),
                row.getDisplayName(),
                row.getProviderFamily(),
                normalizeUpper(row.getEnvironment()),
                row.getApiRoot(),
                row.getOwnerTeam(),
                row.getSlaTier());
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param view view 参数，用于 snapshot 流程中的校验、计算或对象转换。
     * @return 返回 snapshot 流程生成的业务结果。
     */
    private ContractSnapshot snapshot(MarketingIntegrationContractView view) {
        return new ContractSnapshot(
                view.id(),
                safeTenantId(view.tenantId()),
                trimToLimit(view.contractKey(), 128),
                view.displayName(),
                view.providerFamily(),
                normalizeUpper(view.environment()),
                view.apiRoot(),
                view.ownerTeam(),
                view.slaTier());
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
            throw new IllegalArgumentException("integration probe alert metadata must be JSON serializable", ex);
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
     * 执行 observedAt 流程，围绕 observed at 完成校验、计算或结果组装。
     *
     * @param probeRun probe run 参数，用于 observedAt 流程中的校验、计算或对象转换。
     * @return 返回 observedAt 流程生成的业务结果。
     */
    private LocalDateTime observedAt(MarketingIntegrationContractProbeRunView probeRun) {
        if (probeRun.createdAt() != null) {
            return probeRun.createdAt().withNano(0);
        }
        String observedAt = probeRun.observedAt();
        if (observedAt != null && !observedAt.isBlank()) {
            try {
                return LocalDateTime.parse(observedAt.trim()).withNano(0);
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (RuntimeException ignored) {
                return now();
            }
        }
        return now();
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
     * 执行 severity 流程，围绕 severity 完成校验、计算或结果组装。
     *
     * @param slaTier sla tier 参数，用于 severity 流程中的校验、计算或对象转换。
     * @return 返回 severity 生成的文本或业务键。
     */
    private String severity(String slaTier) {
        return "CRITICAL".equals(normalizeUpper(slaTier)) ? "CRITICAL" : "HIGH";
    }

    /**
     * 执行 dedupeKey 流程，围绕 dedupe key 完成校验、计算或结果组装。
     *
     * @param contract contract 参数，用于 dedupeKey 流程中的校验、计算或对象转换。
     * @return 返回 dedupe key 生成的文本或业务键。
     */
    private String dedupeKey(ContractSnapshot contract) {
        return trimToLimit(ALERT_TYPE.toLowerCase(Locale.ROOT) + ":" + contract.contractKey(), 256);
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
     * 解析并规范化租户 ID。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 safe tenant id 计算得到的数量、金额或指标值。
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    /**
     * 解析操作人标识。
     *
     * @param actor 操作人标识，用于审计和权限判断。
     * @return 返回 actor 生成的文本或业务键。
     */
    private static String actor(String actor) {
        String trimmed = actor == null ? "" : actor.trim();
        return trimmed.isBlank() ? "marketing-integration-probe-scheduler" : trimmed;
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
     * ContractSnapshot 数据记录。
     */
    private record ContractSnapshot(
            Long id,
            Long tenantId,
            String contractKey,
            String displayName,
            String providerFamily,
            String environment,
            String apiRoot,
            String ownerTeam,
            String slaTier) {
    }
}
