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
public class MarketingMonitorProviderCredentialRefreshScheduler {

    static final String LEASE_KEY = "MARKETING_MONITOR_PROVIDER_CREDENTIAL_REFRESH";

    private final MarketingMonitorProviderCredentialService credentialService;
    private final CdpWarehouseJobLeaseService leaseService;
    private final boolean enabled;
    private final Long tenantId;
    private final int windowMinutes;
    private final int limit;
    private final String operator;
    private final long leaseTtlSeconds;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public MarketingMonitorProviderCredentialRefreshScheduler(
            MarketingMonitorProviderCredentialService credentialService,
            @Value("${canvas.monitoring.provider-credential-refresh-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.monitoring.provider-credential-refresh-scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.monitoring.provider-credential-refresh-scheduler.window-minutes:30}") int windowMinutes,
            @Value("${canvas.monitoring.provider-credential-refresh-scheduler.limit:50}") int limit,
            @Value("${canvas.monitoring.provider-credential-refresh-scheduler.operator:monitoring-token-scheduler}") String operator) {
        this(credentialService, null, enabled, tenantId, windowMinutes, limit, operator, 120);
    }

    @Autowired
    public MarketingMonitorProviderCredentialRefreshScheduler(
            MarketingMonitorProviderCredentialService credentialService,
            CdpWarehouseJobLeaseService leaseService,
            @Value("${canvas.monitoring.provider-credential-refresh-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.monitoring.provider-credential-refresh-scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.monitoring.provider-credential-refresh-scheduler.window-minutes:30}") int windowMinutes,
            @Value("${canvas.monitoring.provider-credential-refresh-scheduler.limit:50}") int limit,
            @Value("${canvas.monitoring.provider-credential-refresh-scheduler.operator:monitoring-token-scheduler}") String operator,
            @Value("${canvas.monitoring.provider-credential-refresh-scheduler.lease-ttl-seconds:120}") long leaseTtlSeconds) {
        this.credentialService = credentialService;
        this.leaseService = leaseService;
        this.enabled = enabled;
        this.tenantId = tenantId;
        this.windowMinutes = windowMinutes;
        this.limit = limit;
        this.operator = operator == null || operator.isBlank() ? "monitoring-token-scheduler" : operator.trim();
        this.leaseTtlSeconds = leaseTtlSeconds;
    }

    @Scheduled(fixedDelayString = "${canvas.monitoring.provider-credential-refresh-scheduler.fixed-delay-ms:60000}")
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
            credentialService.refreshDue(tenantId,
                    new MarketingMonitorProviderCredentialDueRefreshCommand(windowMinutes, limit),
                    operator);
            return true;
        } finally {
            running.set(false);
        }
    }

    private Duration leaseTtl() {
        return Duration.ofSeconds(Math.max(leaseTtlSeconds, 1));
    }
}
