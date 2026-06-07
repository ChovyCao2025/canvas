package org.chovy.canvas.domain.marketing;

import org.chovy.canvas.domain.warehouse.CdpWarehouseJobLeaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@EnableScheduling
public class MarketingIntegrationContractProbeScheduler {

    private static final String LEASE_KEY = "MARKETING_INTEGRATION_CONTRACT_PROBES";

    private final MarketingIntegrationContractProbeAutomationService automationService;
    private final CdpWarehouseJobLeaseService leaseService;
    private final boolean enabled;
    private final Long tenantId;
    private final int limit;
    private final String operator;
    private final long leaseTtlSeconds;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public MarketingIntegrationContractProbeScheduler(
            MarketingIntegrationContractProbeAutomationService automationService,
            @Value("${canvas.marketing-integrations.probe-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.marketing-integrations.probe-scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.marketing-integrations.probe-scheduler.limit:100}") int limit,
            @Value("${canvas.marketing-integrations.probe-scheduler.operator:marketing-integration-probe-scheduler}") String operator) {
        this(automationService, null, enabled, tenantId, limit, operator, 120);
    }

    @Autowired
    public MarketingIntegrationContractProbeScheduler(
            MarketingIntegrationContractProbeAutomationService automationService,
            CdpWarehouseJobLeaseService leaseService,
            @Value("${canvas.marketing-integrations.probe-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.marketing-integrations.probe-scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.marketing-integrations.probe-scheduler.limit:100}") int limit,
            @Value("${canvas.marketing-integrations.probe-scheduler.operator:marketing-integration-probe-scheduler}") String operator,
            @Value("${canvas.marketing-integrations.probe-scheduler.lease-ttl-seconds:120}") long leaseTtlSeconds) {
        this.automationService = automationService;
        this.leaseService = leaseService;
        this.enabled = enabled;
        this.tenantId = tenantId;
        this.limit = limit;
        this.operator = operator;
        this.leaseTtlSeconds = leaseTtlSeconds;
    }

    @Scheduled(fixedDelayString = "${canvas.marketing-integrations.probe-scheduler.fixed-delay-ms:300000}")
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
            automationService.scanProductionContracts(tenantId, limit, operator);
            return true;
        } finally {
            running.set(false);
        }
    }

    private Duration leaseTtl() {
        return Duration.ofSeconds(Math.max(leaseTtlSeconds, 1));
    }
}
