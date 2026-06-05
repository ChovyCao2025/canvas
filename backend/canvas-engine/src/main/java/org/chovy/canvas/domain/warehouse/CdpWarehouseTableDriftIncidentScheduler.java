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
public class CdpWarehouseTableDriftIncidentScheduler {

    private static final String OPERATOR = "warehouse-table-drift-scheduler";
    private static final String LEASE_KEY = "CDP_WAREHOUSE_TABLE_DRIFT_INCIDENT";

    private final CdpWarehouseTableDriftIncidentService incidentService;
    private final CdpWarehouseJobLeaseService leaseService;
    private final boolean enabled;
    private final Long tenantId;
    private final boolean live;
    private final long leaseTtlSeconds;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public CdpWarehouseTableDriftIncidentScheduler(
            CdpWarehouseTableDriftIncidentService incidentService,
            @Value("${canvas.warehouse.table-drift-incident-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.table-drift-incident-scheduler.tenant-id:0}") Long tenantId) {
        this(incidentService, null, enabled, tenantId, true, 300);
    }

    @Autowired
    public CdpWarehouseTableDriftIncidentScheduler(
            CdpWarehouseTableDriftIncidentService incidentService,
            CdpWarehouseJobLeaseService leaseService,
            @Value("${canvas.warehouse.table-drift-incident-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.table-drift-incident-scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.table-drift-incident-scheduler.live:true}") boolean live,
            @Value("${canvas.warehouse.table-drift-incident-scheduler.lease-ttl-seconds:300}") long leaseTtlSeconds) {
        this.incidentService = incidentService;
        this.leaseService = leaseService;
        this.enabled = enabled;
        this.tenantId = tenantId;
        this.live = live;
        this.leaseTtlSeconds = leaseTtlSeconds;
    }

    @Scheduled(fixedDelayString = "${canvas.warehouse.table-drift-incident-scheduler.fixed-delay-ms:300000}")
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
            incidentService.scan(tenantId, live, OPERATOR);
            return true;
        } finally {
            running.set(false);
        }
    }

    private Duration leaseTtl() {
        return Duration.ofSeconds(Math.max(leaseTtlSeconds, 1));
    }
}
