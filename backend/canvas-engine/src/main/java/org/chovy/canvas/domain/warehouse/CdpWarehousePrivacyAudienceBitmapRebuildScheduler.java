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
public class CdpWarehousePrivacyAudienceBitmapRebuildScheduler {

    static final String LEASE_KEY = "CDP_WAREHOUSE_PRIVACY_AUDIENCE_REBUILD";

    private final CdpWarehousePrivacyAudienceBitmapRebuildAutomationService automationService;
    private final CdpWarehouseJobLeaseService leaseService;
    private final boolean enabled;
    private final Long tenantId;
    private final int scanLimit;
    private final int audienceLimit;
    private final boolean retryFailed;
    private final String actor;
    private final long leaseTtlSeconds;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Autowired
    public CdpWarehousePrivacyAudienceBitmapRebuildScheduler(
            CdpWarehousePrivacyAudienceBitmapRebuildAutomationService automationService,
            CdpWarehouseJobLeaseService leaseService,
            @Value("${canvas.warehouse.privacy-audience-rebuild-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.privacy-audience-rebuild-scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.privacy-audience-rebuild-scheduler.scan-limit:50}") int scanLimit,
            @Value("${canvas.warehouse.privacy-audience-rebuild-scheduler.audience-limit:100}") int audienceLimit,
            @Value("${canvas.warehouse.privacy-audience-rebuild-scheduler.retry-failed:false}") boolean retryFailed,
            @Value("${canvas.warehouse.privacy-audience-rebuild-scheduler.actor:privacy-rebuild-scheduler}") String actor,
            @Value("${canvas.warehouse.privacy-audience-rebuild-scheduler.lease-ttl-seconds:60}") long leaseTtlSeconds) {
        this.automationService = automationService;
        this.leaseService = leaseService;
        this.enabled = enabled;
        this.tenantId = tenantId == null ? 0L : tenantId;
        this.scanLimit = scanLimit;
        this.audienceLimit = audienceLimit;
        this.retryFailed = retryFailed;
        this.actor = actor == null || actor.isBlank() ? "privacy-rebuild-scheduler" : actor.trim();
        this.leaseTtlSeconds = Math.max(leaseTtlSeconds, 1);
    }

    @Scheduled(fixedDelayString = "${canvas.warehouse.privacy-audience-rebuild-scheduler.fixed-delay-ms:60000}")
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
            automationService.run(tenantId,
                    new CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationCommand(
                            actor, scanLimit, audienceLimit, retryFailed));
            return true;
        } finally {
            running.set(false);
        }
    }

    private Duration leaseTtl() {
        return Duration.ofSeconds(leaseTtlSeconds);
    }
}
