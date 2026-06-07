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
public class CdpWarehouseEnterpriseOlapEvidenceScheduler {

    static final String LEASE_KEY = "CDP_WAREHOUSE_ENTERPRISE_OLAP_EVIDENCE";

    private final CdpWarehouseEnterpriseOlapEvidenceCollectionService collectionService;
    private final CdpWarehouseJobLeaseService leaseService;
    private final boolean enabled;
    private final Long tenantId;
    private final String triggerType;
    private final String actor;
    private final long leaseTtlSeconds;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Autowired
    public CdpWarehouseEnterpriseOlapEvidenceScheduler(
            CdpWarehouseEnterpriseOlapEvidenceCollectionService collectionService,
            CdpWarehouseJobLeaseService leaseService,
            @Value("${canvas.warehouse.enterprise-olap-evidence-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.enterprise-olap-evidence-scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.enterprise-olap-evidence-scheduler.trigger-type:SCHEDULED}") String triggerType,
            @Value("${canvas.warehouse.enterprise-olap-evidence-scheduler.actor:enterprise-olap-evidence-scheduler}") String actor,
            @Value("${canvas.warehouse.enterprise-olap-evidence-scheduler.lease-ttl-seconds:60}") long leaseTtlSeconds) {
        this.collectionService = collectionService;
        this.leaseService = leaseService;
        this.enabled = enabled;
        this.tenantId = tenantId == null ? 0L : tenantId;
        this.triggerType = triggerType == null || triggerType.isBlank() ? "SCHEDULED" : triggerType.trim();
        this.actor = actor == null || actor.isBlank() ? "enterprise-olap-evidence-scheduler" : actor.trim();
        this.leaseTtlSeconds = Math.max(leaseTtlSeconds, 1);
    }

    @Scheduled(fixedDelayString = "${canvas.warehouse.enterprise-olap-evidence-scheduler.fixed-delay-ms:300000}")
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
            collectionService.run(tenantId, triggerType, actor);
            return true;
        } finally {
            running.set(false);
        }
    }

    private Duration leaseTtl() {
        return Duration.ofSeconds(leaseTtlSeconds);
    }
}
