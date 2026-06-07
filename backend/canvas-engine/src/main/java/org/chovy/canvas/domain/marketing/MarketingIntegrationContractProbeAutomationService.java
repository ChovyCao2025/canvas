package org.chovy.canvas.domain.marketing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractDO;
import org.chovy.canvas.dal.mapper.MarketingIntegrationContractMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class MarketingIntegrationContractProbeAutomationService {

    static final String PROBE_KEY = "prod-readiness-probe";
    static final String FAILURE_PROBLEM_TYPE_URI = "urn:canvas:marketing-integration:probe-failure";

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final MarketingIntegrationContractMapper contractMapper;
    private final MarketingIntegrationContractProbeService probeService;
    private final MarketingIntegrationContractProbeClient probeClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public MarketingIntegrationContractProbeAutomationService(
            MarketingIntegrationContractMapper contractMapper,
            MarketingIntegrationContractProbeService probeService,
            MarketingIntegrationContractProbeClient probeClient,
            ObjectMapper objectMapper) {
        this(contractMapper, probeService, probeClient, objectMapper, Clock.systemDefaultZone());
    }

    MarketingIntegrationContractProbeAutomationService(
            MarketingIntegrationContractMapper contractMapper,
            MarketingIntegrationContractProbeService probeService,
            MarketingIntegrationContractProbeClient probeClient,
            Clock clock) {
        this(contractMapper, probeService, probeClient, new ObjectMapper(), clock);
    }

    MarketingIntegrationContractProbeAutomationService(
            MarketingIntegrationContractMapper contractMapper,
            MarketingIntegrationContractProbeService probeService,
            MarketingIntegrationContractProbeClient probeClient,
            ObjectMapper objectMapper,
            Clock clock) {
        this.contractMapper = contractMapper;
        this.probeService = probeService;
        this.probeClient = probeClient;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    public ProbeAutomationSummary scanProductionContracts(Long tenantId, Integer limit, String actor) {
        Long scopedTenantId = safeTenantId(tenantId);
        LocalDateTime evaluatedAt = LocalDateTime.now(clock).withNano(0);
        int boundedLimit = boundedLimit(limit);
        List<MarketingIntegrationContractDO> candidates = safeList(contractMapper.selectList(
                new LambdaQueryWrapper<MarketingIntegrationContractDO>()
                        .eq(MarketingIntegrationContractDO::getTenantId, scopedTenantId)
                        .eq(MarketingIntegrationContractDO::getEnvironment, "PRODUCTION")
                        .eq(MarketingIntegrationContractDO::getStatus, "ACTIVE")
                        .orderByDesc(MarketingIntegrationContractDO::getUpdatedAt)
                        .last("LIMIT " + boundedLimit)));
        List<MarketingIntegrationContractDO> contracts = candidates.stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> "PRODUCTION".equalsIgnoreCase(defaultString(row.getEnvironment(), "")))
                .filter(row -> "ACTIVE".equalsIgnoreCase(defaultString(row.getStatus(), "")))
                .limit(boundedLimit)
                .toList();
        List<ProbeAutomationResult> results = contracts.stream()
                .map(contract -> probeContract(scopedTenantId, contract, actor, evaluatedAt))
                .toList();
        long passed = results.stream().filter(result -> "PASS".equals(result.status())).count();
        long failed = results.stream().filter(result -> !"PASS".equals(result.status())).count();
        return new ProbeAutomationSummary(
                scopedTenantId,
                candidates.size(),
                results.size(),
                Math.toIntExact(passed),
                Math.toIntExact(failed),
                Math.max(0, candidates.size() - results.size()),
                evaluatedAt,
                results);
    }

    private ProbeAutomationResult probeContract(Long tenantId,
                                                MarketingIntegrationContractDO contract,
                                                String actor,
                                                LocalDateTime evaluatedAt) {
        try {
            MarketingIntegrationContractProbeClient.ProbeResult probe = probeClient.probe(toTarget(contract));
            MarketingIntegrationContractProbeRunView view = probeService.recordProbeRun(
                    tenantId,
                    contract.getId(),
                    new MarketingIntegrationContractProbeRunCommand(
                            PROBE_KEY,
                            normalizeStatus(probe.status()),
                            probe.httpStatusCode(),
                            probe.latencyMs(),
                            defaultString(probe.problemTypeUri(), HttpMarketingIntegrationContractProbeClient.PROBLEM_TYPE_URI),
                            probe.errorMessage(),
                            defaultString(probe.summary(), "Provider health endpoint probed"),
                            evidence(contract, probe.evidence(), evaluatedAt)),
                    actor(actor));
            return toResult(view);
        } catch (RuntimeException ex) {
            MarketingIntegrationContractProbeRunView view = probeService.recordProbeRun(
                    tenantId,
                    contract.getId(),
                    new MarketingIntegrationContractProbeRunCommand(
                            PROBE_KEY,
                            "FAIL",
                            null,
                            null,
                            FAILURE_PROBLEM_TYPE_URI,
                            message(ex),
                            "Automatic probe failed",
                            failureEvidence(contract, ex, evaluatedAt)),
                    actor(actor));
            return toResult(view);
        }
    }

    private MarketingIntegrationContractProbeClient.ProbeTarget toTarget(MarketingIntegrationContractDO contract) {
        return new MarketingIntegrationContractProbeClient.ProbeTarget(
                contract.getId(),
                contract.getTenantId(),
                contract.getContractKey(),
                contract.getDisplayName(),
                contract.getProviderFamily(),
                contract.getApiRoot(),
                contract.getAuthMode(),
                contract.getTimeoutMs(),
                fromJson(contract.getSchemaContractJson()),
                fromJson(contract.getMetadataJson()));
    }

    private ProbeAutomationResult toResult(MarketingIntegrationContractProbeRunView view) {
        return new ProbeAutomationResult(
                view.contractId(),
                view.contractKey(),
                view.providerFamily(),
                view.probeKey(),
                view.status(),
                view.httpStatusCode(),
                view.latencyMs(),
                view.summary(),
                view.errorMessage(),
                view.observedAt());
    }

    private Map<String, Object> evidence(MarketingIntegrationContractDO contract,
                                         Map<String, Object> clientEvidence,
                                         LocalDateTime evaluatedAt) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        if (clientEvidence != null) {
            evidence.putAll(clientEvidence);
        }
        evidence.put("probeSource", "AUTOMATED");
        evidence.put("contractKey", contract.getContractKey());
        evidence.put("providerFamily", contract.getProviderFamily());
        evidence.put("evaluatedAt", evaluatedAt.toString());
        return evidence;
    }

    private Map<String, Object> failureEvidence(MarketingIntegrationContractDO contract,
                                                RuntimeException exception,
                                                LocalDateTime evaluatedAt) {
        Map<String, Object> evidence = evidence(contract, Map.of(), evaluatedAt);
        evidence.put("exceptionType", exception.getClass().getSimpleName());
        return evidence;
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

    private static String normalizeStatus(String value) {
        String status = defaultString(value, "PASS").toUpperCase(Locale.ROOT);
        return switch (status) {
            case "PASS", "WARN", "FAIL" -> status;
            default -> "FAIL";
        };
    }

    private static List<MarketingIntegrationContractDO> safeList(List<MarketingIntegrationContractDO> rows) {
        return rows == null ? List.of() : rows;
    }

    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    private static int boundedLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private static String actor(String actor) {
        return actor == null || actor.isBlank() ? "marketing-integration-probe-scheduler" : actor.trim();
    }

    private static String defaultString(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed;
    }

    private static String message(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    public record ProbeAutomationSummary(
            Long tenantId,
            int candidateCount,
            int probedCount,
            int passedCount,
            int failedCount,
            int skippedCount,
            LocalDateTime evaluatedAt,
            List<ProbeAutomationResult> results) {
    }

    public record ProbeAutomationResult(
            Long contractId,
            String contractKey,
            String providerFamily,
            String probeKey,
            String status,
            Integer httpStatusCode,
            Long latencyMs,
            String summary,
            String errorMessage,
            String observedAt) {
    }
}
