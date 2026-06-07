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

@Slf4j
@Service
public class MarketingIntegrationContractProbeAlertService {

    static final String ALERT_TYPE = "INTEGRATION_CONTRACT_PROBE_FAILURE";

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final MarketingMonitorAlertMapper alertMapper;
    private final ObjectMapper objectMapper;
    private final MarketingMonitorAlertFanoutService fanoutService;
    private final Clock clock;

    public MarketingIntegrationContractProbeAlertService(
            MarketingMonitorAlertMapper alertMapper,
            ObjectMapper objectMapper,
            ObjectProvider<MarketingMonitorAlertFanoutService> fanoutProvider) {
        this(alertMapper,
                objectMapper,
                fanoutProvider == null ? null : fanoutProvider.getIfAvailable(),
                Clock.systemDefaultZone());
    }

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

    public void syncProbeResult(Long tenantId,
                                MarketingIntegrationContractDO contract,
                                MarketingIntegrationContractProbeRunView probeRun,
                                String actor) {
        if (contract == null) {
            return;
        }
        syncProbeResult(tenantId, snapshot(contract), probeRun, actor);
    }

    public void syncProbeResult(Long tenantId,
                                MarketingIntegrationContractView contract,
                                MarketingIntegrationContractProbeRunView probeRun,
                                String actor) {
        if (contract == null) {
            return;
        }
        syncProbeResult(tenantId, snapshot(contract), probeRun, actor);
    }

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
        } else if ("PASS".equals(status)) {
            resolveOpenFailureAlert(safeTenantId(tenantId), contract, probeRun, actor(actor));
        }
    }

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

    private void updateOpenFailureAlert(MarketingMonitorAlertDO existing,
                                        ContractSnapshot contract,
                                        MarketingIntegrationContractProbeRunView probeRun,
                                        LocalDateTime observedAt) {
        existing.setSeverity(severity(contract.slaTier()));
        existing.setDedupeKey(dedupeKey(contract));
        existing.setReason(failureReason(contract, probeRun));
        existing.setItemCount(existing.getItemCount() == null ? 1 : existing.getItemCount() + 1);
        existing.setWindowEnd(observedAt);
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

    private void resolveOpenFailureAlert(Long tenantId,
                                         ContractSnapshot contract,
                                         MarketingIntegrationContractProbeRunView probeRun,
                                         String actor) {
        MarketingMonitorAlertDO existing = openAlert(tenantId, contract.contractKey());
        if (existing == null) {
            return;
        }
        LocalDateTime resolvedAt = now();
        existing.setStatus("RESOLVED");
        existing.setDedupeKey(null);
        existing.setResolvedBy(actor);
        existing.setResolvedAt(resolvedAt);
        existing.setWindowEnd(observedAt(probeRun));
        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put("recoveredProbeRunId", probeRun.id());
        updates.put("recoveredHttpStatusCode", probeRun.httpStatusCode());
        updates.put("recoveredLatencyMs", probeRun.latencyMs());
        updates.put("recoveredAt", resolvedAt.toString());
        existing.setMetadataJson(json(merge(existing.getMetadataJson(), updates)));
        existing.setUpdatedAt(resolvedAt);
        alertMapper.updateById(existing);
    }

    private MarketingMonitorAlertDO openAlert(Long tenantId, String scopeKey) {
        return alertMapper.selectOne(new LambdaQueryWrapper<MarketingMonitorAlertDO>()
                .eq(MarketingMonitorAlertDO::getTenantId, tenantId)
                .eq(MarketingMonitorAlertDO::getAlertType, ALERT_TYPE)
                .eq(MarketingMonitorAlertDO::getStatus, "OPEN")
                .eq(MarketingMonitorAlertDO::getScopeKey, scopeKey)
                .last("LIMIT 1"));
    }

    private void dispatch(MarketingMonitorAlertDO row, String actor) {
        if (fanoutService == null) {
            return;
        }
        try {
            fanoutService.dispatchAlert(row.getTenantId(), row, actor);
        } catch (RuntimeException ex) {
            log.warn("[MARKETING-INTEGRATION] probe alert fanout skipped alert={} error={}",
                    row.getId(), ex.getMessage());
        }
    }

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

    private Map<String, Object> merge(String existingJson, Map<String, Object> updates) {
        Map<String, Object> merged = new LinkedHashMap<>(map(existingJson));
        updates.forEach((key, value) -> {
            if (value != null) {
                merged.put(key, value);
            }
        });
        return merged;
    }

    private String failureReason(ContractSnapshot contract, MarketingIntegrationContractProbeRunView probeRun) {
        String message = defaultString(probeRun.errorMessage(), defaultString(probeRun.summary(), "probe failed"));
        return trimToLimit(contract.contractKey() + " failed " + probeRun.probeKey() + ": " + message, 1000);
    }

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

    private String json(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("integration probe alert metadata must be JSON serializable", ex);
        }
    }

    private Map<String, Object> map(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    private LocalDateTime observedAt(MarketingIntegrationContractProbeRunView probeRun) {
        if (probeRun.createdAt() != null) {
            return probeRun.createdAt().withNano(0);
        }
        String observedAt = probeRun.observedAt();
        if (observedAt != null && !observedAt.isBlank()) {
            try {
                return LocalDateTime.parse(observedAt.trim()).withNano(0);
            } catch (RuntimeException ignored) {
                return now();
            }
        }
        return now();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).withNano(0);
    }

    private String severity(String slaTier) {
        return "CRITICAL".equals(normalizeUpper(slaTier)) ? "CRITICAL" : "HIGH";
    }

    private String dedupeKey(ContractSnapshot contract) {
        return trimToLimit(ALERT_TYPE.toLowerCase(Locale.ROOT) + ":" + contract.contractKey(), 256);
    }

    private static String normalizeUpper(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? "" : trimmed.toUpperCase(Locale.ROOT);
    }

    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    private static String actor(String actor) {
        String trimmed = actor == null ? "" : actor.trim();
        return trimmed.isBlank() ? "marketing-integration-probe-scheduler" : trimmed;
    }

    private static String defaultString(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed;
    }

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
