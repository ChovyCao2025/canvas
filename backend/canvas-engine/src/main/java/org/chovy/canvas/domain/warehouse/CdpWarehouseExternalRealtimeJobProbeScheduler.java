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
public class CdpWarehouseExternalRealtimeJobProbeScheduler {

    private static final String LEASE_KEY = "CDP_WAREHOUSE_EXTERNAL_REALTIME_JOB_PROBE";

    private final CdpWarehouseExternalRealtimeJobProbeService probeService;
    private final CdpWarehouseJobLeaseService leaseService;
    private final boolean enabled;
    private final Long tenantId;
    private final int limit;
    private final long leaseTtlSeconds;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public CdpWarehouseExternalRealtimeJobProbeScheduler(
            CdpWarehouseExternalRealtimeJobProbeService probeService,
            @Value("${canvas.warehouse.external-realtime-job-probe-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.external-realtime-job-probe-scheduler.tenant-id:0}") Long tenantId) {
        this(probeService, null, enabled, tenantId, 50, 60);
    }

    @Autowired
    public CdpWarehouseExternalRealtimeJobProbeScheduler(
            CdpWarehouseExternalRealtimeJobProbeService probeService,
            CdpWarehouseJobLeaseService leaseService,
            @Value("${canvas.warehouse.external-realtime-job-probe-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.external-realtime-job-probe-scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.external-realtime-job-probe-scheduler.limit:50}") int limit,
            @Value("${canvas.warehouse.external-realtime-job-probe-scheduler.lease-ttl-seconds:60}") long leaseTtlSeconds) {
        this.probeService = probeService;
        this.leaseService = leaseService;
        this.enabled = enabled;
        this.tenantId = tenantId;
        this.limit = limit;
        this.leaseTtlSeconds = leaseTtlSeconds;
    }

    @Scheduled(fixedDelayString = "${canvas.warehouse.external-realtime-job-probe-scheduler.fixed-delay-ms:60000}")
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
            probeService.scan(tenantId, new CdpWarehouseExternalRealtimeJobProbeService.ScanCommand(null, limit));
            return true;
        } finally {
            running.set(false);
        }
    }

    private Duration leaseTtl() {
        return Duration.ofSeconds(Math.max(leaseTtlSeconds, 1));
    }
}
