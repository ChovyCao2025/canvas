package org.chovy.canvas.cdp.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class CdpWarehouseTableCatalog {

    private final Clock clock;
    private final Map<Long, TenantTables> tenants = new LinkedHashMap<>();

    public CdpWarehouseTableCatalog(Clock clock) {
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    public Map<String, Object> listContracts(Long tenantId, String layer, String lifecycleStatus) {
        List<Map<String, Object>> records = tables(tenantId).contracts.stream()
                .filter(item -> matches(item, "layer", upper(layer)))
                .filter(item -> matches(item, "lifecycleStatus", upper(lifecycleStatus)))
                .map(CdpWarehouseTableCatalog::copy)
                .toList();
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("tenantId", tenantId);
        page.put("total", (long) records.size());
        page.put("records", records);
        return page;
    }

    public Map<String, Object> upsertContract(Long tenantId, Map<String, Object> payload, String actor) {
        required(payload, "tableKey");
        TenantTables tenant = tables(tenantId);
        String tableKey = String.valueOf(payload.get("tableKey"));
        Map<String, Object> contract = findOptional(tenant.contracts, tableKey);
        if (contract == null) {
            contract = new LinkedHashMap<>();
            contract.put("id", (long) tenant.contracts.size() + 1);
            contract.put("tableKey", tableKey);
            tenant.contracts.add(contract);
        }
        contract.putAll(payload);
        contract.put("tenantId", tenantId);
        contract.put("tableKey", tableKey);
        contract.put("layer", upper(value(payload.get("layer"), "DWD")));
        contract.put("lifecycleStatus", upper(value(payload.get("lifecycleStatus"), "ACTIVE")));
        contract.put("updatedBy", actor);
        contract.put("updatedAt", now());
        return copy(contract);
    }

    public Map<String, Object> inspectContract(Long tenantId, String tableKey, String actor, boolean live) {
        TenantTables tenant = tables(tenantId);
        Map<String, Object> contract = findOptional(tenant.contracts, tableKey);
        if (contract == null && Objects.equals(tenantId, 0L)) {
            contract = defaultContract(tenantId, tableKey);
        } else if (contract == null) {
            throw new IllegalArgumentException("Warehouse table contract not found: " + tableKey);
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("tenantId", tenantId);
        report.put("tableKey", tableKey);
        report.put("live", live);
        report.put("status", "WARN");
        report.put("issueCount", issueCount(contract));
        report.put("inspectedBy", actor);
        report.put("inspectedAt", now());
        tenant.inspections.add(report);
        return copy(report);
    }

    public Map<String, Object> inspectAll(Long tenantId, String actor, boolean live) {
        TenantTables tenant = tables(tenantId);
        int issueCount = tenant.contracts.stream().mapToInt(CdpWarehouseTableCatalog::issueCount).sum();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("tenantId", tenantId);
        summary.put("live", live);
        summary.put("tableCount", tenant.contracts.size());
        summary.put("inspectedCount", tenant.contracts.size());
        summary.put("issueCount", issueCount);
        summary.put("failedCount", issueCount);
        summary.put("inspectedBy", actor);
        summary.put("inspectedAt", now());
        return summary;
    }

    public Map<String, Object> planRemediation(Long tenantId, String tableKey, boolean live, String actor) {
        Map<String, Object> contract = find(tables(tenantId).contracts, tableKey);
        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("tenantId", tenantId);
        plan.put("tableKey", tableKey);
        plan.put("live", live);
        plan.put("actionCount", issueCount(contract));
        plan.put("actions", remediationActions(contract));
        plan.put("plannedBy", actor);
        plan.put("plannedAt", now());
        return plan;
    }

    public Map<String, Object> planAllRemediation(Long tenantId, boolean live, String actor) {
        TenantTables tenant = tables(tenantId);
        int actionCount = tenant.contracts.stream().mapToInt(CdpWarehouseTableCatalog::issueCount).sum();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("tenantId", tenantId);
        summary.put("live", live);
        summary.put("tableCount", tenant.contracts.size());
        summary.put("plannedCount", tenant.contracts.size());
        summary.put("actionCount", actionCount);
        summary.put("plannedBy", actor);
        summary.put("plannedAt", now());
        return summary;
    }

    public Map<String, Object> scanIncidents(Long tenantId, boolean live, String actor) {
        TenantTables tenant = tables(tenantId);
        int incidentCount = tenant.contracts.stream().mapToInt(CdpWarehouseTableCatalog::issueCount).sum();
        Map<String, Object> scan = new LinkedHashMap<>();
        scan.put("tenantId", tenantId);
        scan.put("live", live);
        scan.put("scanKey", "warehouse-table-scan-" + (tenant.incidentScans.size() + 1));
        scan.put("scannedCount", tenant.contracts.size());
        scan.put("incidentCount", incidentCount);
        scan.put("scannedBy", actor);
        scan.put("scannedAt", now());
        tenant.incidentScans.add(scan);
        return copy(scan);
    }

    private TenantTables tables(Long tenantId) {
        return tenants.computeIfAbsent(tenantId, ignored -> new TenantTables());
    }

    private String now() {
        return Instant.now(clock).toString();
    }

    private static List<Map<String, Object>> remediationActions(Map<String, Object> contract) {
        if (issueCount(contract) == 0) {
            return List.of();
        }
        return List.of(Map.of(
                "type", "VERIFY_TABLE_PROPERTIES",
                "message", "expectedPropertiesJson is not configured"));
    }

    private static int issueCount(Map<String, Object> contract) {
        Object expectedProperties = contract.get("expectedPropertiesJson");
        return expectedProperties == null || String.valueOf(expectedProperties).isBlank() ? 1 : 0;
    }

    private static boolean matches(Map<String, Object> item, String field, Object expected) {
        return expected == null || String.valueOf(expected).isBlank() || Objects.equals(item.get(field), expected);
    }

    private static Map<String, Object> find(List<Map<String, Object>> contracts, String tableKey) {
        Map<String, Object> match = findOptional(contracts, tableKey);
        if (match == null) {
            throw new IllegalArgumentException("Warehouse table contract not found: " + tableKey);
        }
        return match;
    }

    private static Map<String, Object> defaultContract(Long tenantId, String tableKey) {
        Map<String, Object> contract = new LinkedHashMap<>();
        contract.put("tenantId", tenantId);
        contract.put("tableKey", tableKey);
        contract.put("layer", "DWD");
        contract.put("lifecycleStatus", "ACTIVE");
        return contract;
    }

    private static Map<String, Object> findOptional(List<Map<String, Object>> contracts, String tableKey) {
        return contracts.stream()
                .filter(contract -> Objects.equals(contract.get("tableKey"), tableKey))
                .findFirst()
                .orElse(null);
    }

    private static void required(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
    }

    private static String value(Object value, String fallback) {
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
    }

    private static String upper(String value) {
        return value == null || value.isBlank() ? value : value.trim().toUpperCase(Locale.ROOT);
    }

    private static Map<String, Object> copy(Map<String, Object> source) {
        return new LinkedHashMap<>(source);
    }

    private static final class TenantTables {
        private final List<Map<String, Object>> contracts = new ArrayList<>();
        private final List<Map<String, Object>> inspections = new ArrayList<>();
        private final List<Map<String, Object>> incidentScans = new ArrayList<>();
    }
}
