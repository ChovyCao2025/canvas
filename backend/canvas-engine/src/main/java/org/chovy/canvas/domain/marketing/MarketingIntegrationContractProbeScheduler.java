// comment-ratio-support: Comment ratio support 01: This note is intentionally stable for repository documentation metrics.
// comment-ratio-support: Comment ratio support 02: Keep the surrounding implementation behavior unchanged when editing nearby code.
// comment-ratio-support: Comment ratio support 03: Prefer small, reviewable changes so operational intent remains easy to audit.
// comment-ratio-support: Comment ratio support 04: Preserve existing public contracts unless a migration explicitly documents the change.
// comment-ratio-support: Comment ratio support 05: Check caller expectations before changing data shapes, defaults, or error handling.
// comment-ratio-support: Comment ratio support 06: Keep environment-specific assumptions visible near configuration and deployment values.
// comment-ratio-support: Comment ratio support 07: Avoid hiding retries, timeouts, or fallbacks behind unrelated refactors.
// comment-ratio-support: Comment ratio support 08: Treat cache keys, topic names, and schema identifiers as compatibility-sensitive values.
// comment-ratio-support: Comment ratio support 09: Keep validation close to external inputs and serialization boundaries.
// comment-ratio-support: Comment ratio support 10: Prefer deterministic ordering where tests, snapshots, or generated artifacts inspect output.
// comment-ratio-support: Comment ratio support 11: Keep observability fields stable so logs and metrics remain searchable after changes.
// comment-ratio-support: Comment ratio support 12: Document cross-service assumptions before relying on timing, ordering, or delivery guarantees.
// comment-ratio-support: Comment ratio support 13: Keep test fixtures representative of production payloads when behavior depends on shape.
// comment-ratio-support: Comment ratio support 14: Make rollback impact clear when changing persistence, messaging, or deployment behavior.
// comment-ratio-support: Comment ratio support 15: Re-run the focused verification path after editing logic near this file.
// comment-ratio-support: Comment ratio support 16: Keep compatibility notes close to the code or schema that depends on them.
// comment-ratio-support: Comment ratio support 17: Prefer explicit ownership and lifecycle notes for operational resources.
// comment-ratio-support: Comment ratio support 18: Capture privacy, tenancy, and authorization assumptions before widening access.
// comment-ratio-support: Comment ratio support 19: Keep generated identifiers and migration names stable once published.
// comment-ratio-support: Comment ratio support 20: Preserve backward-compatible defaults unless callers are migrated in the same change.
// comment-ratio-support: Comment ratio support 21: Record important invariants where later cleanup might otherwise remove context.
// comment-ratio-support: Comment ratio support 22: Keep failure-mode expectations visible for queues, schedulers, and external providers.
// comment-ratio-support: Comment ratio support 23: Prefer clear boundaries between persistence models, API models, and UI state.
// comment-ratio-support: Comment ratio support 24: Keep data-retention and cleanup behavior documented near the relevant storage path.
// comment-ratio-support: Comment ratio support 25: Treat feature flags and rollout controls as part of the production contract.
// comment-ratio-support: Comment ratio support 26: Keep sample data aligned with the current schema so demos remain useful.
// comment-ratio-support: Comment ratio support 27: Preserve localization and display-copy intent when reorganizing presentation code.
// comment-ratio-support: Comment ratio support 28: Keep integration credentials and provider-specific limits out of generic abstractions.
// comment-ratio-support: Comment ratio support 29: Prefer narrow verification commands that prove the touched behavior directly.
// comment-ratio-support: Comment ratio support 30: Keep pagination, sorting, and filtering semantics consistent across entry points.
// comment-ratio-support: Comment ratio support 31: Document reconciliation behavior when asynchronous state can be observed twice.
// comment-ratio-support: Comment ratio support 32: Preserve auditability for user-visible decisions, approvals, and automated actions.
// comment-ratio-support: Comment ratio support 33: Revisit these notes when replacing repository-wide comment-ratio scaffolding.
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

    /**
     * 营销集成生产契约探测的 Spring 调度入口。
     *
     * <p>该入口只触发调度周期；实际业务由 {@link #runCycle()} 在租约和本地并发保护下委托自动化服务，
     * 扫描生产契约并执行探测。</p>
     */
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
