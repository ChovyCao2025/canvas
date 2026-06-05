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
public class CdpWarehouseRetentionScheduler {

    private static final String OPERATOR = "warehouse-retention-scheduler";
    private static final String LEASE_KEY = "CDP_WAREHOUSE_RETENTION";

    private final CdpWarehouseRetentionService retentionService;
    private final CdpWarehouseJobLeaseService leaseService;
    private final boolean enabled;
    private final Long tenantId;
    private final int syncRunRetentionDays;
    private final int realtimeRetryRetentionDays;
    private final int resolvedIncidentRetentionDays;
    private final long leaseTtlSeconds;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public CdpWarehouseRetentionScheduler(
            CdpWarehouseRetentionService retentionService,
            @Value("${canvas.warehouse.retention.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.retention.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.retention.sync-run-retention-days:30}") int syncRunRetentionDays,
            @Value("${canvas.warehouse.retention.realtime-retry-retention-days:14}") int realtimeRetryRetentionDays,
            @Value("${canvas.warehouse.retention.resolved-incident-retention-days:90}") int resolvedIncidentRetentionDays) {
        this(retentionService, null, enabled, tenantId, syncRunRetentionDays, realtimeRetryRetentionDays,
                resolvedIncidentRetentionDays, 300);
    }

    @Autowired
    public CdpWarehouseRetentionScheduler(
            CdpWarehouseRetentionService retentionService,
            CdpWarehouseJobLeaseService leaseService,
            @Value("${canvas.warehouse.retention.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.retention.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.retention.sync-run-retention-days:30}") int syncRunRetentionDays,
            @Value("${canvas.warehouse.retention.realtime-retry-retention-days:14}") int realtimeRetryRetentionDays,
            @Value("${canvas.warehouse.retention.resolved-incident-retention-days:90}") int resolvedIncidentRetentionDays,
            @Value("${canvas.warehouse.retention.lease-ttl-seconds:300}") long leaseTtlSeconds) {
        this.retentionService = retentionService;
        this.leaseService = leaseService;
        this.enabled = enabled;
        this.tenantId = tenantId;
        this.syncRunRetentionDays = syncRunRetentionDays;
        this.realtimeRetryRetentionDays = realtimeRetryRetentionDays;
        this.resolvedIncidentRetentionDays = resolvedIncidentRetentionDays;
        this.leaseTtlSeconds = leaseTtlSeconds;
    }

    @Scheduled(fixedDelayString = "${canvas.warehouse.retention.fixed-delay-ms:86400000}")
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
            retentionService.cleanup(
                    tenantId,
                    now,
                    syncRunRetentionDays,
                    realtimeRetryRetentionDays,
                    resolvedIncidentRetentionDays,
                    OPERATOR);
            return true;
        } finally {
            running.set(false);
        }
    }

    private Duration leaseTtl() {
        return Duration.ofSeconds(Math.max(leaseTtlSeconds, 1));
    }
}
