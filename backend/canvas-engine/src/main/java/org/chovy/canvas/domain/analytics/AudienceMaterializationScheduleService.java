package org.chovy.canvas.domain.analytics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;
import org.chovy.canvas.dal.dataobject.AudienceMaterializationRunDO;
import org.chovy.canvas.dal.mapper.AudienceDefinitionMapper;
import org.chovy.canvas.dal.mapper.AudienceMaterializationRunMapper;
import org.chovy.canvas.domain.warehouse.CdpWarehouseAvailabilityService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseConsumerAvailabilityService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AudienceMaterializationScheduleService {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 100;
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String DEFAULT_OPERATOR = "scheduler";

    private final AudienceDefinitionMapper definitionMapper;
    private final AudienceMaterializationRunMapper runMapper;
    private final AudienceMaterializationService materializationService;
    private final CdpWarehouseAvailabilityService availabilityService;
    private final CdpWarehouseConsumerAvailabilityService consumerAvailabilityService;
    private final ObjectMapper objectMapper;

    public AudienceMaterializationScheduleService(AudienceDefinitionMapper definitionMapper,
                                                  AudienceMaterializationRunMapper runMapper,
                                                  AudienceMaterializationService materializationService) {
        this(definitionMapper, runMapper, materializationService,
                (CdpWarehouseAvailabilityService) null,
                (CdpWarehouseConsumerAvailabilityService) null);
    }

    @Autowired
    public AudienceMaterializationScheduleService(
            AudienceDefinitionMapper definitionMapper,
            AudienceMaterializationRunMapper runMapper,
            AudienceMaterializationService materializationService,
            ObjectProvider<CdpWarehouseAvailabilityService> availabilityServiceProvider,
            ObjectProvider<CdpWarehouseConsumerAvailabilityService> consumerAvailabilityServiceProvider) {
        this(definitionMapper,
                runMapper,
                materializationService,
                availabilityServiceProvider == null ? null : availabilityServiceProvider.getIfAvailable(),
                consumerAvailabilityServiceProvider == null
                        ? null
                        : consumerAvailabilityServiceProvider.getIfAvailable());
    }

    public AudienceMaterializationScheduleService(
            AudienceDefinitionMapper definitionMapper,
            AudienceMaterializationRunMapper runMapper,
            AudienceMaterializationService materializationService,
            ObjectProvider<CdpWarehouseAvailabilityService> availabilityServiceProvider) {
        this(definitionMapper,
                runMapper,
                materializationService,
                availabilityServiceProvider == null ? null : availabilityServiceProvider.getIfAvailable(),
                null);
    }

    AudienceMaterializationScheduleService(AudienceDefinitionMapper definitionMapper,
                                           AudienceMaterializationRunMapper runMapper,
                                           AudienceMaterializationService materializationService,
                                           CdpWarehouseAvailabilityService availabilityService) {
        this(definitionMapper, runMapper, materializationService, availabilityService, null);
    }

    AudienceMaterializationScheduleService(AudienceDefinitionMapper definitionMapper,
                                           AudienceMaterializationRunMapper runMapper,
                                           AudienceMaterializationService materializationService,
                                           CdpWarehouseAvailabilityService availabilityService,
                                           CdpWarehouseConsumerAvailabilityService consumerAvailabilityService) {
        this.definitionMapper = definitionMapper;
        this.runMapper = runMapper;
        this.materializationService = materializationService;
        this.availabilityService = availabilityService;
        this.consumerAvailabilityService = consumerAvailabilityService;
        this.objectMapper = new ObjectMapper();
    }

    public ScheduledRefreshResult refreshDue(Long tenantId, LocalDateTime now, int limit, String operator) {
        Long scopedTenantId = normalizeTenant(tenantId);
        LocalDateTime evaluatedAt = now == null ? LocalDateTime.now() : now;
        int boundedLimit = boundLimit(limit);
        List<AudienceDefinitionDO> candidates =
                definitionMapper.selectMaterializationCandidates(scopedTenantId, boundedLimit);
        if (candidates == null || candidates.isEmpty()) {
            return new ScheduledRefreshResult(scopedTenantId, 0, 0, 0, 0, 0, evaluatedAt);
        }

        int due = 0;
        int succeeded = 0;
        int failed = 0;
        int skipped = 0;
        String scopedOperator = normalizeOperator(operator);
        for (AudienceDefinitionDO definition : candidates) {
            AudienceMaterializationRunDO latest = runMapper.latestSuccessfulRun(scopedTenantId, definition.getId());
            if (!isDue(definition, latest, evaluatedAt)) {
                skipped++;
                continue;
            }
            due++;
            try {
                AudienceMaterializationService.MaterializationResult result =
                        materializationService.materialize(scopedTenantId, definition.getId(), scopedOperator);
                if (STATUS_SUCCESS.equals(result.status())) {
                    succeeded++;
                } else {
                    failed++;
                }
            } catch (RuntimeException ex) {
                failed++;
            }
        }

        return new ScheduledRefreshResult(scopedTenantId, candidates.size(), due, succeeded, failed, skipped, evaluatedAt);
    }

    public GatedScheduledRefreshResult refreshDueWithAvailabilityGate(Long tenantId,
                                                                      LocalDateTime now,
                                                                      int limit,
                                                                      String operator,
                                                                      LocalDateTime from,
                                                                      LocalDateTime to,
                                                                      String mode,
                                                                      boolean allowWarn) {
        if (availabilityService == null) {
            throw new IllegalStateException("warehouse availability service is not configured");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        CdpWarehouseAvailabilityService.AvailabilityDecision availability =
                availabilityService.evaluate(scopedTenantId, from, to, mode);
        String availabilityStatus = availability == null ? "FAIL" : availability.status();
        if ("FAIL".equalsIgnoreCase(availabilityStatus)) {
            return new GatedScheduledRefreshResult(
                    scopedTenantId,
                    "BLOCKED",
                    "warehouse availability FAIL",
                    availability,
                    null);
        }
        if ("WARN".equalsIgnoreCase(availabilityStatus) && !allowWarn) {
            return new GatedScheduledRefreshResult(
                    scopedTenantId,
                    "BLOCKED",
                    "warehouse availability WARN requires allowWarn=true",
                    availability,
                    null);
        }
        ScheduledRefreshResult refreshResult = refreshDue(scopedTenantId, now, limit, operator);
        String reason = "WARN".equalsIgnoreCase(availabilityStatus)
                ? "warehouse availability WARN accepted by operator"
                : "warehouse availability PASS";
        return new GatedScheduledRefreshResult(
                scopedTenantId,
                "EXECUTED",
                reason,
                availability,
                refreshResult);
    }

    public ContractGatedScheduledRefreshResult refreshDueWithConsumerAvailabilityContracts(Long tenantId,
                                                                                           LocalDateTime now,
                                                                                           int limit,
                                                                                           String operator,
                                                                                           LocalDateTime from,
                                                                                           LocalDateTime to,
                                                                                           String defaultContractPrefix) {
        if (consumerAvailabilityService == null) {
            throw new IllegalStateException("warehouse consumer availability service is not configured");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        LocalDateTime evaluatedAt = now == null ? LocalDateTime.now() : now;
        int boundedLimit = boundLimit(limit);
        List<AudienceDefinitionDO> candidates =
                definitionMapper.selectMaterializationCandidates(scopedTenantId, boundedLimit);
        if (candidates == null || candidates.isEmpty()) {
            return new ContractGatedScheduledRefreshResult(scopedTenantId, 0, 0, 0, 0, 0, 0, evaluatedAt);
        }

        int due = 0;
        int succeeded = 0;
        int failed = 0;
        int blocked = 0;
        int skipped = 0;
        String scopedOperator = normalizeOperator(operator);
        for (AudienceDefinitionDO definition : candidates) {
            AudienceMaterializationRunDO latest = runMapper.latestSuccessfulRun(scopedTenantId, definition.getId());
            if (!isDue(definition, latest, evaluatedAt)) {
                skipped++;
                continue;
            }
            due++;
            String contractKey = contractKey(definition, defaultContractPrefix);
            try {
                CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityEvaluation evaluation =
                        consumerAvailabilityService.evaluateContract(scopedTenantId, contractKey, from, to);
                if (evaluation == null || !evaluation.allowed()) {
                    blocked++;
                    continue;
                }
                AudienceMaterializationService.MaterializationResult result =
                        materializationService.materialize(scopedTenantId, definition.getId(), scopedOperator);
                if (STATUS_SUCCESS.equals(result.status())) {
                    succeeded++;
                } else {
                    failed++;
                }
            } catch (RuntimeException ex) {
                failed++;
            }
        }
        return new ContractGatedScheduledRefreshResult(
                scopedTenantId,
                candidates.size(),
                due,
                succeeded,
                failed,
                blocked,
                skipped,
                evaluatedAt);
    }

    boolean isDue(AudienceDefinitionDO definition, AudienceMaterializationRunDO latest, LocalDateTime now) {
        if (definition == null || definition.getId() == null) {
            return false;
        }
        if (latest == null) {
            return true;
        }
        if (!hasText(definition.getCronExpression())) {
            return false;
        }
        LocalDateTime baseline = latest.getFinishedAt() == null ? latest.getStartedAt() : latest.getFinishedAt();
        if (baseline == null) {
            return true;
        }
        CronExpression cron = parseCron(definition.getCronExpression());
        if (cron == null) {
            return false;
        }
        LocalDateTime next = cron.next(baseline);
        return next != null && !next.isAfter(now == null ? LocalDateTime.now() : now);
    }

    private CronExpression parseCron(String expression) {
        String normalized = expression.trim().replaceAll("\\s+", " ");
        int fields = normalized.split(" ").length;
        if (fields == 5) {
            normalized = "0 " + normalized;
        }
        try {
            return CronExpression.parse(normalized);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private int boundLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private String normalizeOperator(String operator) {
        return hasText(operator) ? operator.trim() : DEFAULT_OPERATOR;
    }

    private String contractKey(AudienceDefinitionDO definition, String defaultContractPrefix) {
        String override = contractKeyOverride(definition == null ? null : definition.getDataSourceConfig());
        if (hasText(override)) {
            return override.trim();
        }
        String prefix = hasText(defaultContractPrefix) ? defaultContractPrefix.trim() : "audience_";
        Long audienceId = definition == null ? null : definition.getId();
        return prefix + (audienceId == null ? "unknown" : audienceId);
    }

    private String contractKeyOverride(String dataSourceConfig) {
        if (!hasText(dataSourceConfig)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(dataSourceConfig);
            String warehouseKey = text(root, "warehouseAvailabilityContractKey");
            if (hasText(warehouseKey)) {
                return warehouseKey;
            }
            return text(root, "consumerAvailabilityContractKey");
        } catch (JsonProcessingException | RuntimeException ex) {
            return null;
        }
    }

    private String text(JsonNode root, String fieldName) {
        if (root == null || !root.has(fieldName)) {
            return null;
        }
        JsonNode value = root.get(fieldName);
        return value == null || value.isNull() ? null : value.asText();
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record ScheduledRefreshResult(
            Long tenantId,
            int scanned,
            int due,
            int succeeded,
            int failed,
            int skipped,
            LocalDateTime evaluatedAt) {
    }

    public record GatedScheduledRefreshResult(
            Long tenantId,
            String status,
            String reason,
            CdpWarehouseAvailabilityService.AvailabilityDecision availability,
            ScheduledRefreshResult refreshResult) {
    }

    public record ContractGatedScheduledRefreshResult(
            Long tenantId,
            int scanned,
            int due,
            int succeeded,
            int failed,
            int blocked,
            int skipped,
            LocalDateTime evaluatedAt) {
    }
}
