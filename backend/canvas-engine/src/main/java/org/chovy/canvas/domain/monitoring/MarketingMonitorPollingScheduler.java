package org.chovy.canvas.domain.monitoring;

import org.chovy.canvas.domain.warehouse.CdpWarehouseJobLeaseService;
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
public class MarketingMonitorPollingScheduler {

    private static final String LEASE_KEY = "MARKETING_MONITOR_POLLING";

    private final MarketingMonitorPollingScheduleService scheduleService;
    private final CdpWarehouseJobLeaseService leaseService;
    private final boolean enabled;
    private final Long tenantId;
    private final int limit;
    private final String operator;
    private final long leaseTtlSeconds;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public MarketingMonitorPollingScheduler(
            MarketingMonitorPollingScheduleService scheduleService,
            @Value("${canvas.monitoring.polling-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.monitoring.polling-scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.monitoring.polling-scheduler.limit:50}") int limit,
            @Value("${canvas.monitoring.polling-scheduler.operator:monitoring-scheduler}") String operator) {
        this(scheduleService, null, enabled, tenantId, limit, operator, 120);
    }

    @Autowired
    public MarketingMonitorPollingScheduler(
            MarketingMonitorPollingScheduleService scheduleService,
            CdpWarehouseJobLeaseService leaseService,
            @Value("${canvas.monitoring.polling-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.monitoring.polling-scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.monitoring.polling-scheduler.limit:50}") int limit,
            @Value("${canvas.monitoring.polling-scheduler.operator:monitoring-scheduler}") String operator,
            @Value("${canvas.monitoring.polling-scheduler.lease-ttl-seconds:120}") long leaseTtlSeconds) {
        this.scheduleService = scheduleService;
        this.leaseService = leaseService;
        this.enabled = enabled;
        this.tenantId = tenantId;
        this.limit = limit;
        this.operator = operator;
        this.leaseTtlSeconds = leaseTtlSeconds;
    }

    @Scheduled(fixedDelayString = "${canvas.monitoring.polling-scheduler.fixed-delay-ms:60000}")
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
            scheduleService.pollDueSources(tenantId, now, limit, operator);
            return true;
        } finally {
            running.set(false);
        }
    }

    private Duration leaseTtl() {
        return Duration.ofSeconds(Math.max(leaseTtlSeconds, 1));
    }
}
