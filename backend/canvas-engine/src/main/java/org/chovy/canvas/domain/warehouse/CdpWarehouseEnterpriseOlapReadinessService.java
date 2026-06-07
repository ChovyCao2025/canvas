package org.chovy.canvas.domain.warehouse;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
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

    public EnterpriseOlapReadiness evaluate(Long tenantId, List<EnterpriseOlapGate> evidence) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Map<String, EnterpriseOlapGate> suppliedByKey = suppliedByKey(evidence);
        List<EnterpriseOlapGate> gates = new ArrayList<>();
        List<String> missingCriticalGates = new ArrayList<>();
        for (GateDefinition definition : REQUIRED_GATES) {
            EnterpriseOlapGate supplied = suppliedByKey.get(definition.key());
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
        return new EnterpriseOlapReadiness(
                scopedTenantId,
                status,
                LocalDateTime.now(),
                summary(status, missingCriticalGates),
                List.copyOf(gates),
                List.copyOf(missingCriticalGates));
    }

    public EnterpriseOlapReadiness evaluateFromProductionEvidence(
            Long tenantId,
            List<CdpWarehouseProductionReadinessProofService.ProofEvidence> evidence) {
        List<EnterpriseOlapGate> gates = new ArrayList<>();
        List<CdpWarehouseProductionReadinessProofService.ProofEvidence> contractEvidence = new ArrayList<>();
        for (CdpWarehouseProductionReadinessProofService.ProofEvidence row : safeList(evidence)) {
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
        return evaluate(tenantId, gates);
    }

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

    private EnterpriseOlapGate gate(String key, String status, String reason, String source) {
        GateDefinition definition = definitionByKey(key);
        return new EnterpriseOlapGate(
                normalizeKey(key),
                normalizeStatus(status),
                defaultReason(reason, status, key),
                definition == null || definition.critical(),
                defaultString(source, definition == null ? "unknown" : definition.source()));
    }

    private GateDefinition definitionByKey(String key) {
        String normalizedKey = normalizeKey(key);
        for (GateDefinition definition : REQUIRED_GATES) {
            if (definition.key().equals(normalizedKey)) {
                return definition;
            }
        }
        return null;
    }

    private String worstStatus(List<?> values) {
        if (values.stream().map(this::statusValue).anyMatch(STATUS_FAIL::equals)) {
            return STATUS_FAIL;
        }
        if (values.stream().map(this::statusValue).anyMatch(STATUS_WARN::equals)) {
            return STATUS_WARN;
        }
        return STATUS_PASS;
    }

    private String statusValue(Object value) {
        if (value instanceof EnterpriseOlapGate gate) {
            return normalizeStatus(gate.status());
        }
        if (value instanceof String status) {
            return normalizeStatus(status);
        }
        return STATUS_FAIL;
    }

    private String normalizeStatus(String status) {
        String value = status == null ? STATUS_FAIL : status.trim().toUpperCase(Locale.ROOT);
        if (STATUS_PASS.equals(value) || STATUS_WARN.equals(value) || STATUS_FAIL.equals(value)) {
            return value;
        }
        return STATUS_FAIL;
    }

    private String normalizeKey(String key) {
        return key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
    }

    private String defaultReason(String reason, String status, String key) {
        if (hasText(reason)) {
            return reason.trim();
        }
        return normalizeKey(key) + " " + normalizeStatus(status).toLowerCase(Locale.ROOT);
    }

    private String summary(String status, List<String> missingCriticalGates) {
        if (missingCriticalGates == null || missingCriticalGates.isEmpty()) {
            return "enterprise OLAP readiness " + status;
        }
        return "enterprise OLAP readiness " + status
                + ": missing critical gates " + String.join(", ", missingCriticalGates);
    }

    private String defaultString(String value, String defaultValue) {
        return hasText(value) ? value.trim() : defaultValue;
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private record GateDefinition(String key, boolean critical, String source) {
    }

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

    public record EnterpriseOlapGate(
            String key,
            String status,
            String reason,
            boolean critical,
            String source) {
    }
}
