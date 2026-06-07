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

    public CdpWarehouseAudienceMaterializationScheduler(
            AudienceMaterializationScheduleService scheduleService,
            @Value("${canvas.warehouse.audience-materialization-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.audience-materialization-scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.audience-materialization-scheduler.limit:50}") int limit,
            @Value("${canvas.warehouse.audience-materialization-scheduler.operator:scheduler}") String operator) {
        this(scheduleService, null, enabled, tenantId, limit, operator, 120,
                false, "HYBRID", false, false, "audience_");
    }

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
    public void scheduledCycle() {
        runCycle(LocalDateTime.now());
    }

    boolean runCycle(LocalDateTime now) {
        if (!enabled) {
            return false;
        }
        if (leaseService != null) {
            return leaseService.runWithLease(tenantId, LEASE_KEY, leaseTtl(), () -> executeCycle(now));
        }
        return executeCycle(now);
    }

    private boolean executeCycle(LocalDateTime now) {
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
            return true;
        } finally {
            running.set(false);
        }
    }

    private Duration leaseTtl() {
        return Duration.ofSeconds(Math.max(leaseTtlSeconds, 1));
    }
}
