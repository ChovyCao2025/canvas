package org.chovy.canvas.domain.warehouse;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
/**
 * CdpWarehouseEnterpriseOlapReadinessService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class CdpWarehouseEnterpriseOlapReadinessService {

    private static final String STATUS_PASS = "PASS";
    private static final String STATUS_WARN = "WARN";
    private static final String STATUS_FAIL = "FAIL";
    private static final String ENTERPRISE_PREFIX = "enterprise_olap:";

    private static final List<GateDefinition> REQUIRED_GATES = List.of(
            new GateDefinition("warehouse_readiness", true, "canvas"),
            new GateDefinition("window_availability", true, "canvas"),
            new GateDefinition("consumer_contracts", true, "canvas"),
            new GateDefinition("privacy_erasure_backlog", true, "canvas"),
            new GateDefinition("doris_metrics", true, "doris"),
            new GateDefinition("evidence_collection", true, "warehouse"),
            new GateDefinition("workload_isolation", true, "doris"),
            new GateDefinition("query_slo", true, "doris"),
            new GateDefinition("backup_restore", true, "doris"),
            new GateDefinition("compaction_health", true, "doris"),
            new GateDefinition("ingestion_replay", true, "warehouse"),
            new GateDefinition("runbook_drill", true, "operator"));

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param evidence evidence 参数，用于 evaluate 流程中的校验、计算或对象转换。
     * @return 返回 evaluate 流程生成的业务结果。
     */
    public EnterpriseOlapReadiness evaluate(Long tenantId, List<EnterpriseOlapGate> evidence) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Map<String, EnterpriseOlapGate> suppliedByKey = suppliedByKey(evidence);
        List<EnterpriseOlapGate> gates = new ArrayList<>();
        List<String> missingCriticalGates = new ArrayList<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (GateDefinition definition : REQUIRED_GATES) {
            EnterpriseOlapGate supplied = suppliedByKey.get(definition.key());
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (supplied == null) {
                if (definition.critical()) {
                    missingCriticalGates.add(definition.key());
                }
                gates.add(new EnterpriseOlapGate(
                        definition.key(),
                        STATUS_FAIL,
                        "missing critical enterprise OLAP evidence: " + definition.key(),
                        definition.critical(),
                        definition.source()));
                continue;
            }
            gates.add(new EnterpriseOlapGate(
                    definition.key(),
                    normalizeStatus(supplied.status()),
                    defaultReason(supplied.reason(), supplied.status(), definition.key()),
                    definition.critical(),
                    defaultString(supplied.source(), definition.source())));
        }
        String status = worstStatus(gates);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new EnterpriseOlapReadiness(
                scopedTenantId,
                status,
                LocalDateTime.now(),
                summary(status, missingCriticalGates),
                List.copyOf(gates),
                List.copyOf(missingCriticalGates));
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param evidence evidence 参数，用于 evaluateFromProductionEvidence 流程中的校验、计算或对象转换。
     * @return 返回 evaluateFromProductionEvidence 流程生成的业务结果。
     */
    public EnterpriseOlapReadiness evaluateFromProductionEvidence(
            Long tenantId,
            List<CdpWarehouseProductionReadinessProofService.ProofEvidence> evidence) {
        List<EnterpriseOlapGate> gates = new ArrayList<>();
        List<CdpWarehouseProductionReadinessProofService.ProofEvidence> contractEvidence = new ArrayList<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (CdpWarehouseProductionReadinessProofService.ProofEvidence row : safeList(evidence)) {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (row == null || !hasText(row.key())) {
                continue;
            }
            String key = normalizeKey(row.key());
            if (key.startsWith("consumer_contract:")) {
                contractEvidence.add(row);
                continue;
            }
            if ("consumer_contracts".equals(key)) {
                gates.add(gate("consumer_contracts", row.status(), row.reason(), "canvas"));
                continue;
            }
            if (key.startsWith(ENTERPRISE_PREFIX)) {
                gates.add(gate(key.substring(ENTERPRISE_PREFIX.length()), row.status(), row.reason(),
                        "enterprise_olap"));
                continue;
            }
            if (definitionByKey(key) != null) {
                gates.add(gate(key, row.status(), row.reason(), "canvas"));
            }
        }
        if (!contractEvidence.isEmpty()) {
            gates.add(aggregateConsumerContracts(contractEvidence));
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return evaluate(tenantId, gates);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param evidence evidence 参数，用于 suppliedByKey 流程中的校验、计算或对象转换。
     * @return 返回 suppliedByKey 流程生成的业务结果。
     */
    private Map<String, EnterpriseOlapGate> suppliedByKey(List<EnterpriseOlapGate> evidence) {
        Map<String, EnterpriseOlapGate> suppliedByKey = new LinkedHashMap<>();
        for (EnterpriseOlapGate gate : safeList(evidence)) {
            if (gate == null || !hasText(gate.key())) {
                continue;
            }
            suppliedByKey.put(normalizeKey(gate.key()), gate);
        }
        return suppliedByKey;
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param contractEvidence contract evidence 参数，用于 aggregateConsumerContracts 流程中的校验、计算或对象转换。
     * @return 返回 aggregateConsumerContracts 流程生成的业务结果。
     */
    private EnterpriseOlapGate aggregateConsumerContracts(
            List<CdpWarehouseProductionReadinessProofService.ProofEvidence> contractEvidence) {
        String status = worstStatus(contractEvidence.stream()
                .map(CdpWarehouseProductionReadinessProofService.ProofEvidence::status)
                .toList());
        return gate(
                "consumer_contracts",
                status,
                contractEvidence.size() + " consumer contracts evaluated with " + status,
                "canvas");
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param key 业务键，用于在同一租户下定位资源。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param reason 原因说明，用于记录状态变化的业务依据。
     * @param source source 参数，用于 gate 流程中的校验、计算或对象转换。
     * @return 返回 gate 流程生成的业务结果。
     */
    private EnterpriseOlapGate gate(String key, String status, String reason, String source) {
        GateDefinition definition = definitionByKey(key);
        return new EnterpriseOlapGate(
                normalizeKey(key),
                normalizeStatus(status),
                defaultReason(reason, status, key),
                definition == null || definition.critical(),
                defaultString(source, definition == null ? "unknown" : definition.source()));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param key 业务键，用于在同一租户下定位资源。
     * @return 返回 definitionByKey 流程生成的业务结果。
     */
    private GateDefinition definitionByKey(String key) {
        String normalizedKey = normalizeKey(key);
        for (GateDefinition definition : REQUIRED_GATES) {
            if (definition.key().equals(normalizedKey)) {
                return definition;
            }
        }
        return null;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param values values 参数，用于 worstStatus 流程中的校验、计算或对象转换。
     * @return 返回 worst status 生成的文本或业务键。
     */
    private String worstStatus(List<?> values) {
        if (values.stream().map(this::statusValue).anyMatch(STATUS_FAIL::equals)) {
            return STATUS_FAIL;
        }
        if (values.stream().map(this::statusValue).anyMatch(STATUS_WARN::equals)) {
            return STATUS_WARN;
        }
        return STATUS_PASS;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 status value 生成的文本或业务键。
     */
    private String statusValue(Object value) {
        if (value instanceof EnterpriseOlapGate gate) {
            return normalizeStatus(gate.status());
        }
        if (value instanceof String status) {
            return normalizeStatus(status);
        }
        return STATUS_FAIL;
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
     * @param key 业务键，用于在同一租户下定位资源。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeKey(String key) {
        return key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @param reason 原因说明，用于记录状态变化的业务依据。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param key 业务键，用于在同一租户下定位资源。
     * @return 返回 default reason 生成的文本或业务键。
     */
    private String defaultReason(String reason, String status, String key) {
        if (hasText(reason)) {
            return reason.trim();
        }
        return normalizeKey(key) + " " + normalizeStatus(status).toLowerCase(Locale.ROOT);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param missingCriticalGates missing critical gates 参数，用于 summary 流程中的校验、计算或对象转换。
     * @return 返回 summary 生成的文本或业务键。
     */
    private String summary(String status, List<String> missingCriticalGates) {
        if (missingCriticalGates == null || missingCriticalGates.isEmpty()) {
            return "enterprise OLAP readiness " + status;
        }
        return "enterprise OLAP readiness " + status
                + ": missing critical gates " + String.join(", ", missingCriticalGates);
    }

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param defaultValue 待处理值，用于规则计算或转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private String defaultString(String value, String defaultValue) {
        return hasText(value) ? value.trim() : defaultValue;
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
     * GateDefinition 承载对应领域的业务规则、流程编排和结果转换。
     */
    private record GateDefinition(String key, boolean critical, String source) {
    }

    /**
     * EnterpriseOlapReadiness 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record EnterpriseOlapReadiness(
            Long tenantId,
            String status,
            LocalDateTime evaluatedAt,
            String summary,
            List<EnterpriseOlapGate> gates,
            List<String> missingCriticalGates) {
        public EnterpriseOlapReadiness {
            gates = gates == null ? List.of() : List.copyOf(gates);
            missingCriticalGates = missingCriticalGates == null
                    ? List.of()
                    : List.copyOf(missingCriticalGates);
        }
    }

    /**
     * EnterpriseOlapGate 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record EnterpriseOlapGate(
            String key,
            String status,
            String reason,
            boolean critical,
            String source) {
    }
}
