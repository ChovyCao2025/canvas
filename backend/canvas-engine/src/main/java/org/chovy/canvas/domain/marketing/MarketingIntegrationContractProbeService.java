package org.chovy.canvas.domain.marketing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractDO;
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractProbeObservationDO;
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractProbeRunDO;
import org.chovy.canvas.dal.mapper.MarketingIntegrationContractMapper;
import org.chovy.canvas.dal.mapper.MarketingIntegrationContractProbeObservationMapper;
import org.chovy.canvas.dal.mapper.MarketingIntegrationContractProbeRunMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
public class MarketingIntegrationContractProbeService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final MarketingIntegrationContractMapper contractMapper;
    private final MarketingIntegrationContractProbeRunMapper probeMapper;
    private final MarketingIntegrationContractProbeObservationMapper observationMapper;
    private final ObjectMapper objectMapper;
    private final MarketingIntegrationContractProbeAlertService alertService;
    private final MarketingIntegrationContractSloService sloService;
    private final Clock clock;

    @Autowired
    public MarketingIntegrationContractProbeService(MarketingIntegrationContractMapper contractMapper,
                                                    MarketingIntegrationContractProbeRunMapper probeMapper,
                                                    ObjectProvider<MarketingIntegrationContractProbeObservationMapper>
                                                            observationMapperProvider,
                                                    ObjectMapper objectMapper,
                                                    ObjectProvider<MarketingIntegrationContractProbeAlertService>
                                                            alertServiceProvider,
                                                    ObjectProvider<MarketingIntegrationContractSloService>
                                                            sloServiceProvider) {
        this(contractMapper,
                probeMapper,
                observationMapperProvider == null ? null : observationMapperProvider.getIfAvailable(),
                objectMapper,
                alertServiceProvider == null ? null : alertServiceProvider.getIfAvailable(),
                sloServiceProvider == null ? null : sloServiceProvider.getIfAvailable(),
                Clock.systemDefaultZone());
    }

    MarketingIntegrationContractProbeService(MarketingIntegrationContractMapper contractMapper,
                                             MarketingIntegrationContractProbeRunMapper probeMapper,
                                             ObjectMapper objectMapper,
                                             Clock clock) {
        this(contractMapper, probeMapper, null, objectMapper, null, null, clock);
    }

    MarketingIntegrationContractProbeService(MarketingIntegrationContractMapper contractMapper,
                                             MarketingIntegrationContractProbeRunMapper probeMapper,
                                             ObjectMapper objectMapper,
                                             MarketingIntegrationContractProbeAlertService alertService,
                                             Clock clock) {
        this(contractMapper, probeMapper, null, objectMapper, alertService, null, clock);
    }

    MarketingIntegrationContractProbeService(MarketingIntegrationContractMapper contractMapper,
                                             MarketingIntegrationContractProbeRunMapper probeMapper,
                                             MarketingIntegrationContractProbeObservationMapper observationMapper,
                                             ObjectMapper objectMapper,
                                             MarketingIntegrationContractProbeAlertService alertService,
                                             MarketingIntegrationContractSloService sloService,
                                             Clock clock) {
        this.contractMapper = contractMapper;
        this.probeMapper = probeMapper;
        this.observationMapper = observationMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.alertService = alertService;
        this.sloService = sloService;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    @Transactional(rollbackFor = Exception.class)
    public MarketingIntegrationContractProbeView recordProbe(Long tenantId,
                                                            Long contractId,
                                                            MarketingIntegrationContractProbeCommand command,
                                                            String actor) {
        if (command == null) {
            throw new IllegalArgumentException("integration contract probe command is required");
        }
        Long scopedTenantId = safeTenantId(tenantId);
        MarketingIntegrationContractDO contract = contract(scopedTenantId, contractId);
        String probeKey = normalizeKey(command.probeKey(), "probeKey");
        MarketingIntegrationContractProbeRunDO row =
                probeMapper.selectOne(new LambdaQueryWrapper<MarketingIntegrationContractProbeRunDO>()
                        .eq(MarketingIntegrationContractProbeRunDO::getTenantId, scopedTenantId)
                        .eq(MarketingIntegrationContractProbeRunDO::getContractId, contract.getId())
                        .eq(MarketingIntegrationContractProbeRunDO::getProbeKey, probeKey)
                        .last("LIMIT 1"));
        boolean insert = row == null;
        if (insert) {
            row = new MarketingIntegrationContractProbeRunDO();
            row.setTenantId(scopedTenantId);
            row.setContractId(contract.getId());
            row.setContractKey(contract.getContractKey());
            row.setProbeKey(probeKey);
            row.setCreatedBy(defaultString(actor, "system"));
        }
        row.setProviderFamily(contract.getProviderFamily());
        row.setEnvironment(normalizeEnvironment(command.environment(), contract.getEnvironment()));
        row.setStatus(normalizeStatus(command.status()));
        row.setHttpStatusCode(command.httpStatusCode());
        row.setLatencyMs(nonNegative(command.latencyMs()));
        row.setErrorType(trimToLimit(command.errorType(), 255));
        row.setProblemTypeUri(trimToLimit(command.problemTypeUri(), 512));
        row.setProblemTitle(trimToLimit(command.problemTitle(), 255));
        row.setProblemDetail(trimToLimit(command.problemDetail(), 1000));
        row.setErrorMessage(null);
        row.setSummary(null);
        row.setObservedAt(command.observedAt() == null ? LocalDateTime.now(clock).withNano(0) : command.observedAt());
        row.setEvidenceJson(toJson(command.evidence()));
        row.setUpdatedBy(defaultString(actor, "system"));
        if (insert) {
            probeMapper.insert(row);
        } else {
            probeMapper.updateById(row);
        }
        MarketingIntegrationContractProbeRunView runView = toRunView(row);
        appendObservation(scopedTenantId, contract, runView, actor);
        syncSlo(scopedTenantId, contract, runView.probeKey(), actor);
        return toView(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public MarketingIntegrationContractProbeRunView recordProbeRun(Long tenantId,
                                                                   Long contractId,
                                                                   MarketingIntegrationContractProbeRunCommand command,
                                                                   String actor) {
        if (command == null) {
            throw new IllegalArgumentException("integration contract probe command is required");
        }
        Long scopedTenantId = safeTenantId(tenantId);
        MarketingIntegrationContractDO contract = contract(scopedTenantId, contractId);
        String probeKey = normalizeKey(command.probeKey(), "probeKey");
        MarketingIntegrationContractProbeRunDO row =
                probeMapper.selectOne(new LambdaQueryWrapper<MarketingIntegrationContractProbeRunDO>()
                        .eq(MarketingIntegrationContractProbeRunDO::getTenantId, scopedTenantId)
                        .eq(MarketingIntegrationContractProbeRunDO::getContractId, contract.getId())
                        .eq(MarketingIntegrationContractProbeRunDO::getProbeKey, probeKey)
                        .last("LIMIT 1"));
        boolean insert = row == null;
        if (insert) {
            row = new MarketingIntegrationContractProbeRunDO();
            row.setTenantId(scopedTenantId);
            row.setContractId(contract.getId());
            row.setContractKey(contract.getContractKey());
            row.setProbeKey(probeKey);
            row.setCreatedBy(defaultString(actor, "system"));
        }
        row.setProviderFamily(defaultString(contract.getProviderFamily(), "UNKNOWN"));
        row.setEnvironment(normalizeEnvironment(null, contract.getEnvironment()));
        row.setStatus(normalizeStatus(command.status()));
        row.setHttpStatusCode(validateHttpStatus(command.httpStatusCode()));
        row.setLatencyMs(validateLatency(command.latencyMs()));
        row.setProblemTypeUri(trimToLimit(command.problemTypeUri(), 512));
        row.setErrorMessage(trimToLimit(command.errorMessage(), 1000));
        row.setSummary(trimToLimit(command.summary(), 512));
        row.setObservedAt(LocalDateTime.now(clock).withNano(0));
        row.setEvidenceJson(toJson(command.evidence()));
        row.setUpdatedBy(defaultString(actor, "system"));
        if (insert) {
            probeMapper.insert(row);
        } else {
            probeMapper.updateById(row);
        }
        MarketingIntegrationContractProbeRunView view = toRunView(row);
        appendObservation(scopedTenantId, contract, view, actor);
        syncAlert(scopedTenantId, contract, view, actor);
        syncSlo(scopedTenantId, contract, view.probeKey(), actor);
        return view;
    }

    public List<MarketingIntegrationContractProbeView> listContractProbes(Long tenantId,
                                                                         Long contractId,
                                                                         Integer limit) {
        Long scopedTenantId = safeTenantId(tenantId);
        contract(scopedTenantId, contractId);
        return probeMapper.selectList(new LambdaQueryWrapper<MarketingIntegrationContractProbeRunDO>()
                        .eq(MarketingIntegrationContractProbeRunDO::getTenantId, scopedTenantId)
                        .eq(MarketingIntegrationContractProbeRunDO::getContractId, requiredId(contractId, "contractId"))
                        .orderByDesc(MarketingIntegrationContractProbeRunDO::getObservedAt)
                        .last("LIMIT " + normalizedLimit(limit)))
                .stream()
                .map(this::toView)
                .toList();
    }

    public List<MarketingIntegrationContractProbeView> listRecentProbes(Long tenantId,
                                                                       String status,
                                                                       Integer limit) {
        Long scopedTenantId = safeTenantId(tenantId);
        String normalizedStatus = normalizeOptionalStatus(status);
        return probeMapper.selectList(new LambdaQueryWrapper<MarketingIntegrationContractProbeRunDO>()
                        .eq(MarketingIntegrationContractProbeRunDO::getTenantId, scopedTenantId)
                        .eq(normalizedStatus != null,
                                MarketingIntegrationContractProbeRunDO::getStatus,
                                normalizedStatus)
                        .orderByDesc(MarketingIntegrationContractProbeRunDO::getObservedAt)
                        .last("LIMIT " + normalizedLimit(limit)))
                .stream()
                .filter(row -> normalizedStatus == null || normalizedStatus.equals(row.getStatus()))
                .map(this::toView)
                .toList();
    }

    public List<MarketingIntegrationContractProbeRunView> listProbeRuns(Long tenantId,
                                                                        String status,
                                                                        String providerFamily,
                                                                        Integer limit) {
        Long scopedTenantId = safeTenantId(tenantId);
        String normalizedStatus = normalizeOptionalStatus(status);
        String normalizedProvider = normalizeOptionalUpper(providerFamily);
        return probeMapper.selectList(new LambdaQueryWrapper<MarketingIntegrationContractProbeRunDO>()
                        .eq(MarketingIntegrationContractProbeRunDO::getTenantId, scopedTenantId)
                        .eq(normalizedStatus != null,
                                MarketingIntegrationContractProbeRunDO::getStatus,
                                normalizedStatus)
                        .eq(normalizedProvider != null,
                                MarketingIntegrationContractProbeRunDO::getProviderFamily,
                                normalizedProvider)
                        .orderByDesc(MarketingIntegrationContractProbeRunDO::getObservedAt)
                        .last("LIMIT " + normalizedLimit(limit)))
                .stream()
                .filter(row -> normalizedStatus == null || normalizedStatus.equals(row.getStatus()))
                .filter(row -> normalizedProvider == null || normalizedProvider.equals(row.getProviderFamily()))
                .map(this::toRunView)
                .toList();
    }

    private MarketingIntegrationContractDO contract(Long tenantId, Long contractId) {
        MarketingIntegrationContractDO contract = contractMapper.selectById(requiredId(contractId, "contractId"));
        validateTenant(tenantId, contract == null ? null : contract.getTenantId(), "integration contract");
        return contract;
    }

    private void syncAlert(Long tenantId,
                           MarketingIntegrationContractDO contract,
                           MarketingIntegrationContractProbeRunView view,
                           String actor) {
        if (alertService == null) {
            return;
        }
        try {
            alertService.syncProbeResult(tenantId, contract, view, defaultString(actor, "system"));
        } catch (RuntimeException ex) {
            log.warn("[MARKETING-INTEGRATION] probe alert sync skipped contract={} error={}",
                    contract.getContractKey(), ex.getMessage());
        }
    }

    private void appendObservation(Long tenantId,
                                   MarketingIntegrationContractDO contract,
                                   MarketingIntegrationContractProbeRunView view,
                                   String actor) {
        if (observationMapper == null) {
            return;
        }
        try {
            MarketingIntegrationContractProbeObservationDO row = new MarketingIntegrationContractProbeObservationDO();
            row.setTenantId(tenantId);
            row.setContractId(contract.getId());
            row.setProbeRunId(view.id());
            row.setContractKey(contract.getContractKey());
            row.setProviderFamily(defaultString(contract.getProviderFamily(), "UNKNOWN"));
            row.setProbeKey(view.probeKey());
            row.setEnvironment(view.environment());
            row.setStatus(view.status());
            row.setHttpStatusCode(view.httpStatusCode());
            row.setLatencyMs(view.latencyMs());
            row.setProblemTypeUri(view.problemTypeUri());
            row.setErrorMessage(view.errorMessage());
            row.setSummary(view.summary());
            row.setObservedAt(parseObservedAt(view.observedAt()));
            row.setEvidenceJson(toJson(view.evidence()));
            row.setCreatedBy(defaultString(actor, "system"));
            row.setCreatedAt(LocalDateTime.now(clock).withNano(0));
            observationMapper.insert(row);
        } catch (RuntimeException ex) {
            log.warn("[MARKETING-INTEGRATION] probe observation append skipped contract={} probe={} error={}",
                    contract.getContractKey(), view.probeKey(), ex.getMessage());
        }
    }

    private void syncSlo(Long tenantId,
                         MarketingIntegrationContractDO contract,
                         String probeKey,
                         String actor) {
        if (sloService == null) {
            return;
        }
        try {
            sloService.evaluateAndSyncContract(tenantId, contract, probeKey, defaultString(actor, "system"));
        } catch (RuntimeException ex) {
            log.warn("[MARKETING-INTEGRATION] SLO evaluation skipped contract={} probe={} error={}",
                    contract.getContractKey(), probeKey, ex.getMessage());
        }
    }

    private MarketingIntegrationContractProbeView toView(MarketingIntegrationContractProbeRunDO row) {
        return new MarketingIntegrationContractProbeView(
                row.getId(),
                row.getTenantId(),
                row.getContractId(),
                row.getContractKey(),
                row.getProbeKey(),
                row.getEnvironment(),
                row.getStatus(),
                row.getHttpStatusCode(),
                row.getLatencyMs(),
                row.getErrorType(),
                row.getProblemTypeUri(),
                row.getProblemTitle(),
                row.getProblemDetail(),
                row.getObservedAt(),
                fromJson(row.getEvidenceJson()),
                row.getCreatedBy(),
                row.getCreatedAt());
    }

    private MarketingIntegrationContractProbeRunView toRunView(MarketingIntegrationContractProbeRunDO row) {
        return new MarketingIntegrationContractProbeRunView(
                row.getId(),
                row.getTenantId(),
                row.getContractId(),
                row.getContractKey(),
                row.getProviderFamily(),
                row.getEnvironment(),
                row.getProbeKey(),
                row.getStatus(),
                row.getHttpStatusCode(),
                row.getLatencyMs(),
                row.getProblemTypeUri(),
                row.getErrorMessage(),
                row.getSummary(),
                fromJson(row.getEvidenceJson()),
                row.getObservedAt() == null ? null : row.getObservedAt().toString(),
                row.getCreatedBy(),
                row.getUpdatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private String toJson(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("probe evidence must be JSON serializable", e);
        }
    }

    private Map<String, Object> fromJson(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(value, MAP_TYPE);
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    private LocalDateTime parseObservedAt(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.now(clock).withNano(0);
        }
        try {
            return LocalDateTime.parse(value.trim()).withNano(0);
        } catch (RuntimeException ex) {
            return LocalDateTime.now(clock).withNano(0);
        }
    }

    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    private static Long requiredId(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private static String normalizeKey(String value, String field) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return trimmed.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private static String normalizeStatus(String value) {
        String status = normalizeUpper(value, "PASS");
        return switch (status) {
            case "PASS", "WARN", "FAIL" -> status;
            default -> throw new IllegalArgumentException("unsupported integration probe status: " + status);
        };
    }

    private static String normalizeOptionalStatus(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? null : normalizeStatus(trimmed);
    }

    private static String normalizeOptionalUpper(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private static String normalizeEnvironment(String value, String fallback) {
        String environment = normalizeUpper(value, defaultString(fallback, "PRODUCTION"));
        return switch (environment) {
            case "PRODUCTION", "STAGING", "SANDBOX" -> environment;
            default -> throw new IllegalArgumentException("unsupported integration environment: " + environment);
        };
    }

    private static String normalizeUpper(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed.toUpperCase(Locale.ROOT);
    }

    private static Long nonNegative(Long value) {
        return value == null || value < 0 ? null : value;
    }

    private static Integer validateHttpStatus(Integer value) {
        if (value == null) {
            return null;
        }
        if (value < 100 || value > 599) {
            throw new IllegalArgumentException("httpStatusCode must be between 100 and 599");
        }
        return value;
    }

    private static Long validateLatency(Long value) {
        if (value == null) {
            return null;
        }
        if (value < 0) {
            throw new IllegalArgumentException("latencyMs must be non-negative");
        }
        return value;
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

    private static String defaultString(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed;
    }

    private static int normalizedLimit(Integer limit) {
        if (limit == null) {
            return 50;
        }
        return Math.max(1, Math.min(limit, 200));
    }

    private static void validateTenant(Long expected, Long actual, String entity) {
        if (actual == null || !actual.equals(expected)) {
            throw new IllegalArgumentException(entity + " does not belong to tenant");
        }
    }
}
