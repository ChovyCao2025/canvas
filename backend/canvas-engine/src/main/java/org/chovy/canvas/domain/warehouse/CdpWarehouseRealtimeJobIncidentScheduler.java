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
public class CdpWarehouseRealtimeJobIncidentScheduler {

    private static final String LEASE_KEY = "CDP_WAREHOUSE_REALTIME_JOB_INCIDENT";

    private final CdpWarehouseRealtimeJobIncidentService incidentService;
    private final CdpWarehouseJobLeaseService leaseService;
    private final boolean enabled;
    private final Long tenantId;
    private final String pipelineKey;
    private final long maxHeartbeatAgeSeconds;
    private final int limit;
    private final long leaseTtlSeconds;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public CdpWarehouseRealtimeJobIncidentScheduler(
            CdpWarehouseRealtimeJobIncidentService incidentService,
            @Value("${canvas.warehouse.realtime-job-incident-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.realtime-job-incident-scheduler.tenant-id:0}") Long tenantId) {
        this(incidentService, null, enabled, tenantId, null, 300, 50, 60);
    }

    @Autowired
    public CdpWarehouseRealtimeJobIncidentScheduler(
            CdpWarehouseRealtimeJobIncidentService incidentService,
            CdpWarehouseJobLeaseService leaseService,
            @Value("${canvas.warehouse.realtime-job-incident-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.realtime-job-incident-scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.realtime-job-incident-scheduler.pipeline-key:}") String pipelineKey,
            @Value("${canvas.warehouse.realtime-job-incident-scheduler.max-heartbeat-age-seconds:300}") long maxHeartbeatAgeSeconds,
            @Value("${canvas.warehouse.realtime-job-incident-scheduler.limit:50}") int limit,
            @Value("${canvas.warehouse.realtime-job-incident-scheduler.lease-ttl-seconds:60}") long leaseTtlSeconds) {
        this.incidentService = incidentService;
        this.leaseService = leaseService;
        this.enabled = enabled;
        this.tenantId = tenantId;
        this.pipelineKey = hasText(pipelineKey) ? pipelineKey.trim() : null;
        this.maxHeartbeatAgeSeconds = maxHeartbeatAgeSeconds;
        this.limit = limit;
        this.leaseTtlSeconds = leaseTtlSeconds;
    }

    @Scheduled(fixedDelayString = "${canvas.warehouse.realtime-job-incident-scheduler.fixed-delay-ms:60000}")
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
            incidentService.scan(tenantId, pipelineKey, maxHeartbeatAgeSeconds, limit);
            return true;
        } finally {
            running.set(false);
        }
    }

    private Duration leaseTtl() {
        return Duration.ofSeconds(Math.max(leaseTtlSeconds, 1));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
