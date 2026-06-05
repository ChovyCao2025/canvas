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
public class CdpWarehouseScheduler {

    private static final String LEASE_KEY = "CDP_WAREHOUSE_MAIN";

    private final CdpWarehouseOperationsService operationsService;
    private final CdpWarehouseJobLeaseService leaseService;
    private final boolean enabled;
    private final Long tenantId;
    private final int backfillLimit;
    private final int aggregationWindowMinutes;
    private final long leaseTtlSeconds;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public CdpWarehouseScheduler(
            CdpWarehouseOperationsService operationsService,
            @Value("${canvas.warehouse.scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.scheduler.backfill-limit:1000}") int backfillLimit,
            @Value("${canvas.warehouse.scheduler.aggregation-window-minutes:30}") int aggregationWindowMinutes) {
        this(operationsService, null, enabled, tenantId, backfillLimit, aggregationWindowMinutes, 120);
    }

    @Autowired
    public CdpWarehouseScheduler(
            CdpWarehouseOperationsService operationsService,
            CdpWarehouseJobLeaseService leaseService,
            @Value("${canvas.warehouse.scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.scheduler.backfill-limit:1000}") int backfillLimit,
            @Value("${canvas.warehouse.scheduler.aggregation-window-minutes:30}") int aggregationWindowMinutes,
            @Value("${canvas.warehouse.scheduler.lease-ttl-seconds:120}") long leaseTtlSeconds) {
        this.operationsService = operationsService;
        this.leaseService = leaseService;
        this.enabled = enabled;
        this.tenantId = tenantId;
        this.backfillLimit = backfillLimit;
        this.aggregationWindowMinutes = aggregationWindowMinutes;
        this.leaseTtlSeconds = leaseTtlSeconds;
    }

    @Scheduled(fixedDelayString = "${canvas.warehouse.scheduler.fixed-delay-ms:60000}")
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
            operationsService.runScheduledOfflineCycle(tenantId, now, backfillLimit, aggregationWindowMinutes);
            return true;
        } finally {
            running.set(false);
        }
    }

    private Duration leaseTtl() {
        return Duration.ofSeconds(Math.max(leaseTtlSeconds, 1));
    }
}
