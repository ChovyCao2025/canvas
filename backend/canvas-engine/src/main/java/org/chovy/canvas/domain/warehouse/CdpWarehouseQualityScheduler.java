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
public class CdpWarehouseQualityScheduler {

    private static final String OPERATOR = "warehouse-quality-scheduler";
    private static final String LEASE_KEY = "CDP_WAREHOUSE_QUALITY";

    private final CdpWarehouseQualityService qualityService;
    private final CdpWarehouseJobLeaseService leaseService;
    private final boolean enabled;
    private final Long tenantId;
    private final int windowMinutes;
    private final long tolerance;
    private final long maxLagMinutes;
    private final long leaseTtlSeconds;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public CdpWarehouseQualityScheduler(
            CdpWarehouseQualityService qualityService,
            @Value("${canvas.warehouse.quality.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.quality.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.quality.window-minutes:60}") int windowMinutes,
            @Value("${canvas.warehouse.quality.ods-count-tolerance:0}") long tolerance,
            @Value("${canvas.warehouse.quality.max-aggregate-lag-minutes:30}") long maxLagMinutes) {
        this(qualityService, null, enabled, tenantId, windowMinutes, tolerance, maxLagMinutes, 600);
    }

    @Autowired
    public CdpWarehouseQualityScheduler(
            CdpWarehouseQualityService qualityService,
            CdpWarehouseJobLeaseService leaseService,
            @Value("${canvas.warehouse.quality.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.quality.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.quality.window-minutes:60}") int windowMinutes,
            @Value("${canvas.warehouse.quality.ods-count-tolerance:0}") long tolerance,
            @Value("${canvas.warehouse.quality.max-aggregate-lag-minutes:30}") long maxLagMinutes,
            @Value("${canvas.warehouse.quality.lease-ttl-seconds:600}") long leaseTtlSeconds) {
        this.qualityService = qualityService;
        this.leaseService = leaseService;
        this.enabled = enabled;
        this.tenantId = tenantId;
        this.windowMinutes = windowMinutes;
        this.tolerance = tolerance;
        this.maxLagMinutes = maxLagMinutes;
        this.leaseTtlSeconds = leaseTtlSeconds;
    }

    @Scheduled(fixedDelayString = "${canvas.warehouse.quality.fixed-delay-ms:300000}")
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
            LocalDateTime effectiveNow = now == null ? LocalDateTime.now() : now;
            LocalDateTime from = effectiveNow.minusMinutes(Math.max(windowMinutes, 1));
            qualityService.reconcileOds(tenantId, from, effectiveNow, tolerance, OPERATOR);
            qualityService.checkAggregateLag(tenantId, effectiveNow, maxLagMinutes, OPERATOR);
            return true;
        } finally {
            running.set(false);
        }
    }

    private Duration leaseTtl() {
        return Duration.ofSeconds(Math.max(leaseTtlSeconds, 1));
    }
}
