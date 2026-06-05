package org.chovy.canvas.domain.warehouse;

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
public class CdpWarehouseConsumerAvailabilityIncidentScheduler {

    private static final String LEASE_KEY = "CDP_WAREHOUSE_CONSUMER_AVAILABILITY_INCIDENT";

    private final CdpWarehouseConsumerAvailabilityIncidentService incidentService;
    private final CdpWarehouseJobLeaseService leaseService;
    private final boolean enabled;
    private final Long tenantId;
    private final String consumerType;
    private final long windowMinutes;
    private final String operator;
    private final long leaseTtlSeconds;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public CdpWarehouseConsumerAvailabilityIncidentScheduler(
            CdpWarehouseConsumerAvailabilityIncidentService incidentService,
            @Value("${canvas.warehouse.consumer-availability-incident-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.consumer-availability-incident-scheduler.tenant-id:0}") Long tenantId) {
        this(incidentService, null, enabled, tenantId, null, 60,
                "consumer-availability-incident-scheduler", 60);
    }

    @Autowired
    public CdpWarehouseConsumerAvailabilityIncidentScheduler(
            CdpWarehouseConsumerAvailabilityIncidentService incidentService,
            CdpWarehouseJobLeaseService leaseService,
            @Value("${canvas.warehouse.consumer-availability-incident-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.consumer-availability-incident-scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.consumer-availability-incident-scheduler.consumer-type:}") String consumerType,
            @Value("${canvas.warehouse.consumer-availability-incident-scheduler.window-minutes:60}") long windowMinutes,
            @Value("${canvas.warehouse.consumer-availability-incident-scheduler.operator:consumer-availability-incident-scheduler}") String operator,
            @Value("${canvas.warehouse.consumer-availability-incident-scheduler.lease-ttl-seconds:60}") long leaseTtlSeconds) {
        this.incidentService = incidentService;
        this.leaseService = leaseService;
        this.enabled = enabled;
        this.tenantId = tenantId;
        this.consumerType = blankToNull(consumerType);
        this.windowMinutes = windowMinutes;
        this.operator = operator == null || operator.isBlank()
                ? "consumer-availability-incident-scheduler"
                : operator.trim();
        this.leaseTtlSeconds = leaseTtlSeconds;
    }

    @Scheduled(fixedDelayString = "${canvas.warehouse.consumer-availability-incident-scheduler.fixed-delay-ms:60000}")
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
            LocalDateTime evaluatedAt = now == null ? LocalDateTime.now() : now;
            incidentService.scan(
                    tenantId,
                    null,
                    consumerType,
                    evaluatedAt.minusMinutes(Math.max(windowMinutes, 1)),
                    evaluatedAt,
                    operator);
            return true;
        } finally {
            running.set(false);
        }
    }

    private Duration leaseTtl() {
        return Duration.ofSeconds(Math.max(leaseTtlSeconds, 1));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
