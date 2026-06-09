package org.chovy.canvas.domain.analytics;

import org.chovy.canvas.domain.warehouse.CdpWarehouseJobLeaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@EnableScheduling
/**
 * CdpWarehouseAudienceMaterializationScheduler 承载对应领域的业务规则、流程编排和结果转换。
 */
public class CdpWarehouseAudienceMaterializationScheduler {

    private static final String LEASE_KEY = "CDP_AUDIENCE_MATERIALIZATION";

    private final AudienceMaterializationScheduleService scheduleService;
    private final CdpWarehouseJobLeaseService leaseService;
    private final boolean enabled;
    private final Long tenantId;
    private final int limit;
    private final String operator;
    private final long leaseTtlSeconds;
    private final boolean availabilityGateEnabled;
    private final String availabilityMode;
    private final boolean availabilityAllowWarn;
    private final boolean consumerContractGateEnabled;
    private final String consumerContractPrefix;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 初始化 CdpWarehouseAudienceMaterializationScheduler 实例。
     *
     * @param scheduleService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 CdpWarehouseAudienceMaterializationScheduler 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param operator 操作人标识，用于审计和权限判断。
     */
    public CdpWarehouseAudienceMaterializationScheduler(
            AudienceMaterializationScheduleService scheduleService,
            @Value("${canvas.warehouse.audience-materialization-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.audience-materialization-scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.audience-materialization-scheduler.limit:50}") int limit,
            @Value("${canvas.warehouse.audience-materialization-scheduler.operator:scheduler}") String operator) {
        this(scheduleService, null, enabled, tenantId, limit, operator, 120,
                false, "HYBRID", false, false, "audience_");
    }

    /**
     * 初始化 CdpWarehouseAudienceMaterializationScheduler 实例。
     *
     * @param scheduleService 依赖组件，用于完成数据访问或外部能力调用。
     * @param leaseService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 CdpWarehouseAudienceMaterializationScheduler 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param operator 操作人标识，用于审计和权限判断。
     * @param leaseTtlSeconds lease ttl seconds 参数，用于 CdpWarehouseAudienceMaterializationScheduler 流程中的校验、计算或对象转换。
     */
    public CdpWarehouseAudienceMaterializationScheduler(
            AudienceMaterializationScheduleService scheduleService,
            CdpWarehouseJobLeaseService leaseService,
            @Value("${canvas.warehouse.audience-materialization-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.audience-materialization-scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.audience-materialization-scheduler.limit:50}") int limit,
            @Value("${canvas.warehouse.audience-materialization-scheduler.operator:scheduler}") String operator,
            @Value("${canvas.warehouse.audience-materialization-scheduler.lease-ttl-seconds:120}") long leaseTtlSeconds) {
        this(scheduleService, leaseService, enabled, tenantId, limit, operator, leaseTtlSeconds,
                false, "HYBRID", false, false, "audience_");
    }

    @Autowired
    /**
     * 初始化 CdpWarehouseAudienceMaterializationScheduler 实例。
     *
     * @param scheduleService 依赖组件，用于完成数据访问或外部能力调用。
     * @param leaseService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 CdpWarehouseAudienceMaterializationScheduler 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param operator 操作人标识，用于审计和权限判断。
     * @param leaseTtlSeconds lease ttl seconds 参数，用于 CdpWarehouseAudienceMaterializationScheduler 流程中的校验、计算或对象转换。
     * @param availabilityGateEnabled availability gate enabled 参数，用于 CdpWarehouseAudienceMaterializationScheduler 流程中的校验、计算或对象转换。
     * @param availabilityMode availability mode 参数，用于 CdpWarehouseAudienceMaterializationScheduler 流程中的校验、计算或对象转换。
     * @param availabilityAllowWarn availability allow warn 参数，用于 CdpWarehouseAudienceMaterializationScheduler 流程中的校验、计算或对象转换。
     * @param consumerContractGateEnabled consumer contract gate enabled 参数，用于 CdpWarehouseAudienceMaterializationScheduler 流程中的校验、计算或对象转换。
     * @param consumerContractPrefix consumer contract prefix 参数，用于 CdpWarehouseAudienceMaterializationScheduler 流程中的校验、计算或对象转换。
     */
    public CdpWarehouseAudienceMaterializationScheduler(
            AudienceMaterializationScheduleService scheduleService,
            CdpWarehouseJobLeaseService leaseService,
            @Value("${canvas.warehouse.audience-materialization-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.audience-materialization-scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.audience-materialization-scheduler.limit:50}") int limit,
            @Value("${canvas.warehouse.audience-materialization-scheduler.operator:scheduler}") String operator,
            @Value("${canvas.warehouse.audience-materialization-scheduler.lease-ttl-seconds:120}") long leaseTtlSeconds,
            @Value("${canvas.warehouse.audience-materialization-scheduler.availability-gate.enabled:false}") boolean availabilityGateEnabled,
            @Value("${canvas.warehouse.audience-materialization-scheduler.availability-gate.mode:HYBRID}") String availabilityMode,
            @Value("${canvas.warehouse.audience-materialization-scheduler.availability-gate.allow-warn:false}") boolean availabilityAllowWarn,
            @Value("${canvas.warehouse.audience-materialization-scheduler.consumer-contract-gate.enabled:false}") boolean consumerContractGateEnabled,
            @Value("${canvas.warehouse.audience-materialization-scheduler.consumer-contract-gate.contract-prefix:audience_}") String consumerContractPrefix) {
        this.scheduleService = scheduleService;
        this.leaseService = leaseService;
        this.enabled = enabled;
        this.tenantId = tenantId;
        this.limit = limit;
        this.operator = operator;
        this.leaseTtlSeconds = leaseTtlSeconds;
        this.availabilityGateEnabled = availabilityGateEnabled;
        this.availabilityMode = availabilityMode;
        this.availabilityAllowWarn = availabilityAllowWarn;
        this.consumerContractGateEnabled = consumerContractGateEnabled;
        this.consumerContractPrefix = consumerContractPrefix;
    }

    /**
     * 初始化 CdpWarehouseAudienceMaterializationScheduler 实例。
     *
     * @param scheduleService 依赖组件，用于完成数据访问或外部能力调用。
     * @param leaseService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 CdpWarehouseAudienceMaterializationScheduler 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param operator 操作人标识，用于审计和权限判断。
     * @param leaseTtlSeconds lease ttl seconds 参数，用于 CdpWarehouseAudienceMaterializationScheduler 流程中的校验、计算或对象转换。
     * @param availabilityGateEnabled availability gate enabled 参数，用于 CdpWarehouseAudienceMaterializationScheduler 流程中的校验、计算或对象转换。
     * @param availabilityMode availability mode 参数，用于 CdpWarehouseAudienceMaterializationScheduler 流程中的校验、计算或对象转换。
     * @param availabilityAllowWarn availability allow warn 参数，用于 CdpWarehouseAudienceMaterializationScheduler 流程中的校验、计算或对象转换。
     */
    public CdpWarehouseAudienceMaterializationScheduler(
            AudienceMaterializationScheduleService scheduleService,
            CdpWarehouseJobLeaseService leaseService,
            boolean enabled,
            Long tenantId,
            int limit,
            String operator,
            long leaseTtlSeconds,
            boolean availabilityGateEnabled,
            String availabilityMode,
            boolean availabilityAllowWarn) {
        this(scheduleService, leaseService, enabled, tenantId, limit, operator, leaseTtlSeconds,
                availabilityGateEnabled, availabilityMode, availabilityAllowWarn, false, "audience_");
    }

    @Scheduled(fixedDelayString = "${canvas.warehouse.audience-materialization-scheduler.fixed-delay-ms:60000}")
    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     */
    public void scheduledCycle() {
        runCycle(LocalDateTime.now());
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回流程执行后的业务结果。
     */
    boolean runCycle(LocalDateTime now) {
        if (!enabled) {
            return false;
        }
        if (leaseService != null) {
            return leaseService.runWithLease(tenantId, LEASE_KEY, leaseTtl(), () -> executeCycle(now));
        }
        return executeCycle(now);
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回流程执行后的业务结果。
     */
    private boolean executeCycle(LocalDateTime now) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!running.compareAndSet(false, true)) {
            return false;
        }
        try {
            if (consumerContractGateEnabled) {
                scheduleService.refreshDueWithConsumerAvailabilityContracts(
                        tenantId,
                        now,
                        limit,
                        operator,
                        null,
                        now,
                        consumerContractPrefix);
            } else if (availabilityGateEnabled) {
                scheduleService.refreshDueWithAvailabilityGate(
                        tenantId,
                        now,
                        limit,
                        operator,
                        null,
                        now,
                        availabilityMode,
                        availabilityAllowWarn);
            } else {
                scheduleService.refreshDue(tenantId, now, limit, operator);
            }
            // 汇总前面计算出的状态和明细，返回给调用方。
            return true;
        } finally {
            running.set(false);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 leaseTtl 流程生成的业务结果。
     */
    private Duration leaseTtl() {
        return Duration.ofSeconds(Math.max(leaseTtlSeconds, 1));
    }
}
