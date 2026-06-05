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
public class CdpWarehouseAvailabilityIncidentScheduler {

    private static final String LEASE_KEY = "CDP_WAREHOUSE_AVAILABILITY_INCIDENT";

    private final CdpWarehouseAvailabilityIncidentService incidentService;
    private final CdpWarehouseJobLeaseService leaseService;
    private final boolean enabled;
    private final Long tenantId;
    private final String mode;
    private final long windowMinutes;
    private final String operator;
    private final long leaseTtlSeconds;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public CdpWarehouseAvailabilityIncidentScheduler(
            CdpWarehouseAvailabilityIncidentService incidentService,
            @Value("${canvas.warehouse.availability-incident-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.availability-incident-scheduler.tenant-id:0}") Long tenantId) {
        this(incidentService, null, enabled, tenantId, "HYBRID", 60, "availability-incident-scheduler", 60);
    }

    @Autowired
    public CdpWarehouseAvailabilityIncidentScheduler(
            CdpWarehouseAvailabilityIncidentService incidentService,
            CdpWarehouseJobLeaseService leaseService,
            @Value("${canvas.warehouse.availability-incident-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.availability-incident-scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.availability-incident-scheduler.mode:HYBRID}") String mode,
            @Value("${canvas.warehouse.availability-incident-scheduler.window-minutes:60}") long windowMinutes,
            @Value("${canvas.warehouse.availability-incident-scheduler.operator:availability-incident-scheduler}") String operator,
            @Value("${canvas.warehouse.availability-incident-scheduler.lease-ttl-seconds:60}") long leaseTtlSeconds) {
        this.incidentService = incidentService;
        this.leaseService = leaseService;
        this.enabled = enabled;
        this.tenantId = tenantId;
        this.mode = mode == null || mode.isBlank() ? "HYBRID" : mode.trim();
        this.windowMinutes = windowMinutes;
        this.operator = operator == null || operator.isBlank() ? "availability-incident-scheduler" : operator.trim();
        this.leaseTtlSeconds = leaseTtlSeconds;
    }

    @Scheduled(fixedDelayString = "${canvas.warehouse.availability-incident-scheduler.fixed-delay-ms:60000}")
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
                    evaluatedAt.minusMinutes(Math.max(windowMinutes, 1)),
                    evaluatedAt,
                    mode,
                    operator);
            return true;
        } finally {
            running.set(false);
        }
    }

    private Duration leaseTtl() {
        return Duration.ofSeconds(Math.max(leaseTtlSeconds, 1));
    }
}
