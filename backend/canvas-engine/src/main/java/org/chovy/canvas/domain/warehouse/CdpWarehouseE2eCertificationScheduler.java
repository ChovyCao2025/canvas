package org.chovy.canvas.domain.warehouse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@EnableScheduling
/**
 * CdpWarehouseE2eCertificationScheduler 承载对应领域的业务规则、流程编排和结果转换。
 */
public class CdpWarehouseE2eCertificationScheduler {

    private static final String LEASE_KEY = "CDP_WAREHOUSE_E2E_CERTIFICATION";

    private final CdpWarehouseE2eCertificationRunService runService;
    private final CdpWarehouseJobLeaseService leaseService;
    private final boolean enabled;
    private final Long tenantId;
    private final long windowMinutes;
    private final String mode;
    private final String contractKeysCsv;
    private final boolean requirePhysical;
    private final boolean requireRealtime;
    private final boolean requireDataPathProof;
    private final String requestedBy;
    private final long leaseTtlSeconds;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 初始化 CdpWarehouseE2eCertificationScheduler 实例。
     *
     * @param runService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 CdpWarehouseE2eCertificationScheduler 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param windowMinutes window minutes 参数，用于 CdpWarehouseE2eCertificationScheduler 流程中的校验、计算或对象转换。
     * @param mode mode 参数，用于 CdpWarehouseE2eCertificationScheduler 流程中的校验、计算或对象转换。
     * @param contractKeysCsv contract keys csv 参数，用于 CdpWarehouseE2eCertificationScheduler 流程中的校验、计算或对象转换。
     * @param requirePhysical require physical 参数，用于 CdpWarehouseE2eCertificationScheduler 流程中的校验、计算或对象转换。
     * @param requestedBy requested by 参数，用于 CdpWarehouseE2eCertificationScheduler 流程中的校验、计算或对象转换。
     * @param leaseTtlSeconds lease ttl seconds 参数，用于 CdpWarehouseE2eCertificationScheduler 流程中的校验、计算或对象转换。
     */
    public CdpWarehouseE2eCertificationScheduler(
            CdpWarehouseE2eCertificationRunService runService,
            @Value("${canvas.warehouse.e2e-certification-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.e2e-certification-scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.e2e-certification-scheduler.window-minutes:60}") long windowMinutes,
            @Value("${canvas.warehouse.e2e-certification-scheduler.mode:HYBRID}") String mode,
            @Value("${canvas.warehouse.e2e-certification-scheduler.contract-keys:}") String contractKeysCsv,
            @Value("${canvas.warehouse.e2e-certification-scheduler.require-physical:true}") boolean requirePhysical,
            @Value("${canvas.warehouse.e2e-certification-scheduler.requested-by:e2e-certification-scheduler}") String requestedBy,
            @Value("${canvas.warehouse.e2e-certification-scheduler.lease-ttl-seconds:60}") long leaseTtlSeconds) {
        this(runService, null, enabled, tenantId, windowMinutes, mode, contractKeysCsv,
                requirePhysical, false, false, requestedBy, leaseTtlSeconds);
    }

    /**
     * 初始化 CdpWarehouseE2eCertificationScheduler 实例。
     *
     * @param runService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 CdpWarehouseE2eCertificationScheduler 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param windowMinutes window minutes 参数，用于 CdpWarehouseE2eCertificationScheduler 流程中的校验、计算或对象转换。
     * @param mode mode 参数，用于 CdpWarehouseE2eCertificationScheduler 流程中的校验、计算或对象转换。
     * @param contractKeysCsv contract keys csv 参数，用于 CdpWarehouseE2eCertificationScheduler 流程中的校验、计算或对象转换。
     * @param requirePhysical require physical 参数，用于 CdpWarehouseE2eCertificationScheduler 流程中的校验、计算或对象转换。
     * @param requireRealtime 时间参数，用于计算窗口、过期或审计时间。
     * @param requestedBy requested by 参数，用于 CdpWarehouseE2eCertificationScheduler 流程中的校验、计算或对象转换。
     * @param leaseTtlSeconds lease ttl seconds 参数，用于 CdpWarehouseE2eCertificationScheduler 流程中的校验、计算或对象转换。
     */
    public CdpWarehouseE2eCertificationScheduler(
            CdpWarehouseE2eCertificationRunService runService,
            @Value("${canvas.warehouse.e2e-certification-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.e2e-certification-scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.e2e-certification-scheduler.window-minutes:60}") long windowMinutes,
            @Value("${canvas.warehouse.e2e-certification-scheduler.mode:HYBRID}") String mode,
            @Value("${canvas.warehouse.e2e-certification-scheduler.contract-keys:}") String contractKeysCsv,
            @Value("${canvas.warehouse.e2e-certification-scheduler.require-physical:true}") boolean requirePhysical,
            @Value("${canvas.warehouse.e2e-certification-scheduler.require-realtime:true}") boolean requireRealtime,
            @Value("${canvas.warehouse.e2e-certification-scheduler.requested-by:e2e-certification-scheduler}") String requestedBy,
            @Value("${canvas.warehouse.e2e-certification-scheduler.lease-ttl-seconds:60}") long leaseTtlSeconds) {
        this(runService, null, enabled, tenantId, windowMinutes, mode, contractKeysCsv,
                requirePhysical, requireRealtime, false, requestedBy, leaseTtlSeconds);
    }

    /**
     * 初始化 CdpWarehouseE2eCertificationScheduler 实例。
     *
     * @param runService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 CdpWarehouseE2eCertificationScheduler 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param windowMinutes window minutes 参数，用于 CdpWarehouseE2eCertificationScheduler 流程中的校验、计算或对象转换。
     * @param mode mode 参数，用于 CdpWarehouseE2eCertificationScheduler 流程中的校验、计算或对象转换。
     * @param contractKeysCsv contract keys csv 参数，用于 CdpWarehouseE2eCertificationScheduler 流程中的校验、计算或对象转换。
     * @param requirePhysical require physical 参数，用于 CdpWarehouseE2eCertificationScheduler 流程中的校验、计算或对象转换。
     * @param requireRealtime 时间参数，用于计算窗口、过期或审计时间。
     * @param requireDataPathProof require data path proof 参数，用于 CdpWarehouseE2eCertificationScheduler 流程中的校验、计算或对象转换。
     * @param requestedBy requested by 参数，用于 CdpWarehouseE2eCertificationScheduler 流程中的校验、计算或对象转换。
     * @param leaseTtlSeconds lease ttl seconds 参数，用于 CdpWarehouseE2eCertificationScheduler 流程中的校验、计算或对象转换。
     */
    public CdpWarehouseE2eCertificationScheduler(
            CdpWarehouseE2eCertificationRunService runService,
            @Value("${canvas.warehouse.e2e-certification-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.e2e-certification-scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.e2e-certification-scheduler.window-minutes:60}") long windowMinutes,
            @Value("${canvas.warehouse.e2e-certification-scheduler.mode:HYBRID}") String mode,
            @Value("${canvas.warehouse.e2e-certification-scheduler.contract-keys:}") String contractKeysCsv,
            @Value("${canvas.warehouse.e2e-certification-scheduler.require-physical:true}") boolean requirePhysical,
            @Value("${canvas.warehouse.e2e-certification-scheduler.require-realtime:true}") boolean requireRealtime,
            @Value("${canvas.warehouse.e2e-certification-scheduler.require-data-path-proof:true}") boolean requireDataPathProof,
            @Value("${canvas.warehouse.e2e-certification-scheduler.requested-by:e2e-certification-scheduler}") String requestedBy,
            @Value("${canvas.warehouse.e2e-certification-scheduler.lease-ttl-seconds:60}") long leaseTtlSeconds) {
        this(runService, null, enabled, tenantId, windowMinutes, mode, contractKeysCsv,
                requirePhysical, requireRealtime, requireDataPathProof, requestedBy, leaseTtlSeconds);
    }

    /**
     * 初始化 CdpWarehouseE2eCertificationScheduler 实例。
     *
     * @param runService 依赖组件，用于完成数据访问或外部能力调用。
     * @param leaseService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 CdpWarehouseE2eCertificationScheduler 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param windowMinutes window minutes 参数，用于 CdpWarehouseE2eCertificationScheduler 流程中的校验、计算或对象转换。
     * @param mode mode 参数，用于 CdpWarehouseE2eCertificationScheduler 流程中的校验、计算或对象转换。
     * @param contractKeysCsv contract keys csv 参数，用于 CdpWarehouseE2eCertificationScheduler 流程中的校验、计算或对象转换。
     * @param requirePhysical require physical 参数，用于 CdpWarehouseE2eCertificationScheduler 流程中的校验、计算或对象转换。
     * @param requestedBy requested by 参数，用于 CdpWarehouseE2eCertificationScheduler 流程中的校验、计算或对象转换。
     * @param leaseTtlSeconds lease ttl seconds 参数，用于 CdpWarehouseE2eCertificationScheduler 流程中的校验、计算或对象转换。
     */
    public CdpWarehouseE2eCertificationScheduler(
            CdpWarehouseE2eCertificationRunService runService,
            CdpWarehouseJobLeaseService leaseService,
            boolean enabled,
            Long tenantId,
            long windowMinutes,
            String mode,
            String contractKeysCsv,
            boolean requirePhysical,
            String requestedBy,
            long leaseTtlSeconds) {
        this(runService, leaseService, enabled, tenantId, windowMinutes, mode, contractKeysCsv,
                requirePhysical, false, false, requestedBy, leaseTtlSeconds);
    }

    @Autowired
    /**
     * 初始化 CdpWarehouseE2eCertificationScheduler 实例。
     *
     * @param runService 依赖组件，用于完成数据访问或外部能力调用。
     * @param leaseService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 CdpWarehouseE2eCertificationScheduler 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param windowMinutes window minutes 参数，用于 CdpWarehouseE2eCertificationScheduler 流程中的校验、计算或对象转换。
     * @param mode mode 参数，用于 CdpWarehouseE2eCertificationScheduler 流程中的校验、计算或对象转换。
     * @param contractKeysCsv contract keys csv 参数，用于 CdpWarehouseE2eCertificationScheduler 流程中的校验、计算或对象转换。
     * @param requirePhysical require physical 参数，用于 CdpWarehouseE2eCertificationScheduler 流程中的校验、计算或对象转换。
     * @param requireRealtime 时间参数，用于计算窗口、过期或审计时间。
     * @param requireDataPathProof require data path proof 参数，用于 CdpWarehouseE2eCertificationScheduler 流程中的校验、计算或对象转换。
     * @param requestedBy requested by 参数，用于 CdpWarehouseE2eCertificationScheduler 流程中的校验、计算或对象转换。
     * @param leaseTtlSeconds lease ttl seconds 参数，用于 CdpWarehouseE2eCertificationScheduler 流程中的校验、计算或对象转换。
     */
    public CdpWarehouseE2eCertificationScheduler(
            CdpWarehouseE2eCertificationRunService runService,
            CdpWarehouseJobLeaseService leaseService,
            @Value("${canvas.warehouse.e2e-certification-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.e2e-certification-scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.e2e-certification-scheduler.window-minutes:60}") long windowMinutes,
            @Value("${canvas.warehouse.e2e-certification-scheduler.mode:HYBRID}") String mode,
            @Value("${canvas.warehouse.e2e-certification-scheduler.contract-keys:}") String contractKeysCsv,
            @Value("${canvas.warehouse.e2e-certification-scheduler.require-physical:true}") boolean requirePhysical,
            @Value("${canvas.warehouse.e2e-certification-scheduler.require-realtime:true}") boolean requireRealtime,
            @Value("${canvas.warehouse.e2e-certification-scheduler.require-data-path-proof:true}") boolean requireDataPathProof,
            @Value("${canvas.warehouse.e2e-certification-scheduler.requested-by:e2e-certification-scheduler}") String requestedBy,
            @Value("${canvas.warehouse.e2e-certification-scheduler.lease-ttl-seconds:60}") long leaseTtlSeconds) {
        this.runService = runService;
        this.leaseService = leaseService;
        this.enabled = enabled;
        this.tenantId = tenantId == null ? 0L : tenantId;
        this.windowMinutes = Math.max(windowMinutes, 1);
        this.mode = mode == null || mode.isBlank() ? "HYBRID" : mode.trim().toUpperCase();
        this.contractKeysCsv = contractKeysCsv == null ? "" : contractKeysCsv;
        this.requirePhysical = requirePhysical;
        this.requireRealtime = requireRealtime;
        this.requireDataPathProof = requireDataPathProof;
        this.requestedBy = requestedBy == null || requestedBy.isBlank()
                ? "e2e-certification-scheduler"
                : requestedBy.trim();
        this.leaseTtlSeconds = Math.max(leaseTtlSeconds, 1);
    }

    @Scheduled(fixedDelayString = "${canvas.warehouse.e2e-certification-scheduler.fixed-delay-ms:300000}")
    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     */
    public void scheduledCycle() {
        runCycle();
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @return 返回流程执行后的业务结果。
     */
    boolean runCycle() {
        if (!enabled) {
            return false;
        }
        if (leaseService != null) {
            return leaseService.runWithLease(tenantId, LEASE_KEY, leaseTtl(), this::executeCycle);
        }
        return executeCycle();
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @return 返回流程执行后的业务结果。
     */
    private boolean executeCycle() {
        if (!running.compareAndSet(false, true)) {
            return false;
        }
        try {
            LocalDateTime to = LocalDateTime.now();
            LocalDateTime from = to.minusMinutes(windowMinutes);
            runService.run(tenantId, from, to, mode, contractKeys(),
                    requirePhysical, requireRealtime, requireDataPathProof, requestedBy);
            return true;
        } finally {
            running.set(false);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 contract keys 汇总后的集合、分页或映射视图。
     */
    private List<String> contractKeys() {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (contractKeysCsv.isBlank()) {
            return List.of();
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return Arrays.stream(contractKeysCsv.split(","))
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 leaseTtl 流程生成的业务结果。
     */
    private Duration leaseTtl() {
        return Duration.ofSeconds(leaseTtlSeconds);
    }
}
