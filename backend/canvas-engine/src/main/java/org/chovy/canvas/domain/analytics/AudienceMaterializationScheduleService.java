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
/**
 * AudienceMaterializationScheduleService 承载对应领域的业务规则、流程编排和结果转换。
 */
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

    /**
     * 初始化 AudienceMaterializationScheduleService 实例。
     *
     * @param definitionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param runMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param materializationService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public AudienceMaterializationScheduleService(AudienceDefinitionMapper definitionMapper,
                                                  AudienceMaterializationRunMapper runMapper,
                                                  AudienceMaterializationService materializationService) {
        this(definitionMapper, runMapper, materializationService,
                (CdpWarehouseAvailabilityService) null,
                (CdpWarehouseConsumerAvailabilityService) null);
    }

    @Autowired
    /**
     * 初始化 AudienceMaterializationScheduleService 实例。
     *
     * @param definitionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param runMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param materializationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param availabilityServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param consumerAvailabilityServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     */
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

    /**
     * 初始化 AudienceMaterializationScheduleService 实例。
     *
     * @param definitionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param runMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param materializationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param availabilityServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     */
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

    /**
     * 初始化 AudienceMaterializationScheduleService 实例。
     *
     * @param definitionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param runMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param materializationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param availabilityService 依赖组件，用于完成数据访问或外部能力调用。
     */
    AudienceMaterializationScheduleService(AudienceDefinitionMapper definitionMapper,
                                           AudienceMaterializationRunMapper runMapper,
                                           AudienceMaterializationService materializationService,
                                           CdpWarehouseAvailabilityService availabilityService) {
        this(definitionMapper, runMapper, materializationService, availabilityService, null);
    }

    /**
     * 初始化 AudienceMaterializationScheduleService 实例。
     *
     * @param definitionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param runMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param materializationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param availabilityService 依赖组件，用于完成数据访问或外部能力调用。
     * @param consumerAvailabilityService 依赖组件，用于完成数据访问或外部能力调用。
     */
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

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回 refreshDue 流程生成的业务结果。
     */
    public ScheduledRefreshResult refreshDue(Long tenantId, LocalDateTime now, int limit, String operator) {
        Long scopedTenantId = normalizeTenant(tenantId);
        LocalDateTime evaluatedAt = now == null ? LocalDateTime.now() : now;
        int boundedLimit = boundLimit(limit);
        List<AudienceDefinitionDO> candidates =
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                definitionMapper.selectMaterializationCandidates(scopedTenantId, boundedLimit);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (candidates == null || candidates.isEmpty()) {
            return new ScheduledRefreshResult(scopedTenantId, 0, 0, 0, 0, 0, evaluatedAt);
        }

        int due = 0;
        int succeeded = 0;
        int failed = 0;
        int skipped = 0;
        String scopedOperator = normalizeOperator(operator);
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param operator 操作人标识，用于审计和权限判断。
     * @param from 时间或范围边界，用于限定统计窗口。
     * @param to 时间或范围边界，用于限定统计窗口。
     * @param mode mode 参数，用于 refreshDueWithAvailabilityGate 流程中的校验、计算或对象转换。
     * @param allowWarn allow warn 参数，用于 refreshDueWithAvailabilityGate 流程中的校验、计算或对象转换。
     * @return 返回 refreshDueWithAvailabilityGate 流程生成的业务结果。
     */
    public GatedScheduledRefreshResult refreshDueWithAvailabilityGate(Long tenantId,
                                                                      LocalDateTime now,
                                                                      int limit,
                                                                      String operator,
                                                                      LocalDateTime from,
                                                                      LocalDateTime to,
                                                                      String mode,
                                                                      boolean allowWarn) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new GatedScheduledRefreshResult(
                scopedTenantId,
                "EXECUTED",
                reason,
                availability,
                refreshResult);
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param operator 操作人标识，用于审计和权限判断。
     * @param from 时间或范围边界，用于限定统计窗口。
     * @param to 时间或范围边界，用于限定统计窗口。
     * @param defaultContractPrefix default contract prefix 参数，用于 refreshDueWithConsumerAvailabilityContracts 流程中的校验、计算或对象转换。
     * @return 返回 refreshDueWithConsumerAvailabilityContracts 流程生成的业务结果。
     */
    public ContractGatedScheduledRefreshResult refreshDueWithConsumerAvailabilityContracts(Long tenantId,
                                                                                           LocalDateTime now,
                                                                                           int limit,
                                                                                           String operator,
                                                                                           LocalDateTime from,
                                                                                           LocalDateTime to,
                                                                                           String defaultContractPrefix) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (consumerAvailabilityService == null) {
            throw new IllegalStateException("warehouse consumer availability service is not configured");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        LocalDateTime evaluatedAt = now == null ? LocalDateTime.now() : now;
        int boundedLimit = boundLimit(limit);
        List<AudienceDefinitionDO> candidates =
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param definition definition 参数，用于 isDue 流程中的校验、计算或对象转换。
     * @param latest latest 参数，用于 isDue 流程中的校验、计算或对象转换。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回布尔判断结果。
     */
    boolean isDue(AudienceDefinitionDO definition, AudienceMaterializationRunDO latest, LocalDateTime now) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return next != null && !next.isAfter(now == null ? LocalDateTime.now() : now);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param expression expression 参数，用于 parseCron 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
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

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int boundLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeOperator(String operator) {
        return hasText(operator) ? operator.trim() : DEFAULT_OPERATOR;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param definition definition 参数，用于 contractKey 流程中的校验、计算或对象转换。
     * @param defaultContractPrefix default contract prefix 参数，用于 contractKey 流程中的校验、计算或对象转换。
     * @return 返回 contract key 生成的文本或业务键。
     */
    private String contractKey(AudienceDefinitionDO definition, String defaultContractPrefix) {
        String override = contractKeyOverride(definition == null ? null : definition.getDataSourceConfig());
        if (hasText(override)) {
            return override.trim();
        }
        String prefix = hasText(defaultContractPrefix) ? defaultContractPrefix.trim() : "audience_";
        Long audienceId = definition == null ? null : definition.getId();
        return prefix + (audienceId == null ? "unknown" : audienceId);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param dataSourceConfig 配置对象，用于控制运行参数和策略开关。
     * @return 返回 contract key override 生成的文本或业务键。
     */
    private String contractKeyOverride(String dataSourceConfig) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!hasText(dataSourceConfig)) {
            return null;
        }
        try {
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            JsonNode root = objectMapper.readTree(dataSourceConfig);
            String warehouseKey = text(root, "warehouseAvailabilityContractKey");
            if (hasText(warehouseKey)) {
                return warehouseKey;
            }
            return text(root, "consumerAvailabilityContractKey");
        } catch (JsonProcessingException | RuntimeException ex) {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return null;
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param root root 参数，用于 text 流程中的校验、计算或对象转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 text 生成的文本或业务键。
     */
    private String text(JsonNode root, String fieldName) {
        if (root == null || !root.has(fieldName)) {
            return null;
        }
        JsonNode value = root.get(fieldName);
        return value == null || value.isNull() ? null : value.asText();
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
     * ScheduledRefreshResult 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record ScheduledRefreshResult(
            Long tenantId,
            int scanned,
            int due,
            int succeeded,
            int failed,
            int skipped,
            LocalDateTime evaluatedAt) {
    }

    /**
     * GatedScheduledRefreshResult 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record GatedScheduledRefreshResult(
            Long tenantId,
            String status,
            String reason,
            CdpWarehouseAvailabilityService.AvailabilityDecision availability,
            ScheduledRefreshResult refreshResult) {
    }

    /**
     * ContractGatedScheduledRefreshResult 承载对应领域的业务规则、流程编排和结果转换。
     */
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
