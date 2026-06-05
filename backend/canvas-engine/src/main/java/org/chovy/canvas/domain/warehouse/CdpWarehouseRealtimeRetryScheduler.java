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
public class CdpWarehouseRealtimeRetryScheduler {

    private static final String LEASE_KEY = "CDP_WAREHOUSE_REALTIME_RETRY";

    private final CdpWarehouseRealtimeRetryService retryService;
    private final CdpWarehouseJobLeaseService leaseService;
    private final boolean enabled;
    private final Long tenantId;
    private final int limit;
    private final int maxAttempts;
    private final long leaseTtlSeconds;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public CdpWarehouseRealtimeRetryScheduler(
            CdpWarehouseRealtimeRetryService retryService,
            @Value("${canvas.warehouse.realtime-retry.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.realtime-retry.limit:100}") int limit,
            @Value("${canvas.warehouse.realtime-retry.max-attempts:3}") int maxAttempts) {
        this(retryService, null, enabled, 0L, limit, maxAttempts, 60);
    }

    @Autowired
    public CdpWarehouseRealtimeRetryScheduler(
            CdpWarehouseRealtimeRetryService retryService,
            CdpWarehouseJobLeaseService leaseService,
            @Value("${canvas.warehouse.realtime-retry.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.realtime-retry.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.realtime-retry.limit:100}") int limit,
            @Value("${canvas.warehouse.realtime-retry.max-attempts:3}") int maxAttempts,
            @Value("${canvas.warehouse.realtime-retry.lease-ttl-seconds:60}") long leaseTtlSeconds) {
        this.retryService = retryService;
        this.leaseService = leaseService;
        this.enabled = enabled;
        this.tenantId = tenantId;
        this.limit = limit;
        this.maxAttempts = maxAttempts;
        this.leaseTtlSeconds = leaseTtlSeconds;
    }

    @Scheduled(fixedDelayString = "${canvas.warehouse.realtime-retry.fixed-delay-ms:30000}")
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
            retryService.retryDue(now, limit, maxAttempts);
            return true;
        } finally {
            running.set(false);
        }
    }

    private Duration leaseTtl() {
        return Duration.ofSeconds(Math.max(leaseTtlSeconds, 1));
    }
}
