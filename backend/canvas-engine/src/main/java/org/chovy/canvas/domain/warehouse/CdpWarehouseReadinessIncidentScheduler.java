package org.chovy.canvas.domain.warehouse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@EnableScheduling
public class CdpWarehouseReadinessIncidentScheduler {

    private static final String LEASE_KEY = "CDP_WAREHOUSE_READINESS_INCIDENT";

    private final CdpWarehouseReadinessIncidentService readinessIncidentService;
    private final CdpWarehouseJobLeaseService leaseService;
    private final boolean enabled;
    private final Long tenantId;
    private final long leaseTtlSeconds;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public CdpWarehouseReadinessIncidentScheduler(
            CdpWarehouseReadinessIncidentService readinessIncidentService,
            @Value("${canvas.warehouse.readiness-incident-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.readiness-incident-scheduler.tenant-id:0}") Long tenantId) {
        this(readinessIncidentService, null, enabled, tenantId, 60);
    }

    @Autowired
    public CdpWarehouseReadinessIncidentScheduler(
            CdpWarehouseReadinessIncidentService readinessIncidentService,
            CdpWarehouseJobLeaseService leaseService,
            @Value("${canvas.warehouse.readiness-incident-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.readiness-incident-scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.readiness-incident-scheduler.lease-ttl-seconds:60}") long leaseTtlSeconds) {
        this.readinessIncidentService = readinessIncidentService;
        this.leaseService = leaseService;
        this.enabled = enabled;
        this.tenantId = tenantId;
        this.leaseTtlSeconds = leaseTtlSeconds;
    }

    @Scheduled(fixedDelayString = "${canvas.warehouse.readiness-incident-scheduler.fixed-delay-ms:60000}")
    public void scheduledCycle() {
        runCycle();
    }

    boolean runCycle() {
        if (!enabled) {
            return false;
        }
        if (leaseService != null) {
            return leaseService.runWithLease(tenantId, LEASE_KEY, leaseTtl(), this::executeCycle);
        }
        return executeCycle();
    }

    private boolean executeCycle() {
        if (!running.compareAndSet(false, true)) {
            return false;
        }
        try {
            readinessIncidentService.scan(tenantId);
            return true;
        } finally {
            running.set(false);
        }
    }

    private Duration leaseTtl() {
        return Duration.ofSeconds(Math.max(leaseTtlSeconds, 1));
    }
}
