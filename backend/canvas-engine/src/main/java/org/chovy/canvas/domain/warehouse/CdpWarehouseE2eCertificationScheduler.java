package org.chovy.canvas.domain.warehouse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@EnableScheduling
public class CdpWarehouseE2eCertificationScheduler {

    private static final String LEASE_KEY = "CDP_WAREHOUSE_E2E_CERTIFICATION";

    private final CdpWarehouseE2eCertificationRunService runService;
    private final CdpWarehouseJobLeaseService leaseService;
    private final boolean enabled;
    private final Long tenantId;
    private final long windowMinutes;
    private final String mode;
    private final String contractKeysCsv;
    private final boolean requirePhysical;
    private final boolean requireRealtime;
    private final boolean requireDataPathProof;
    private final String requestedBy;
    private final long leaseTtlSeconds;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public CdpWarehouseE2eCertificationScheduler(
            CdpWarehouseE2eCertificationRunService runService,
            @Value("${canvas.warehouse.e2e-certification-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.e2e-certification-scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.e2e-certification-scheduler.window-minutes:60}") long windowMinutes,
            @Value("${canvas.warehouse.e2e-certification-scheduler.mode:HYBRID}") String mode,
            @Value("${canvas.warehouse.e2e-certification-scheduler.contract-keys:}") String contractKeysCsv,
            @Value("${canvas.warehouse.e2e-certification-scheduler.require-physical:true}") boolean requirePhysical,
            @Value("${canvas.warehouse.e2e-certification-scheduler.requested-by:e2e-certification-scheduler}") String requestedBy,
            @Value("${canvas.warehouse.e2e-certification-scheduler.lease-ttl-seconds:60}") long leaseTtlSeconds) {
        this(runService, null, enabled, tenantId, windowMinutes, mode, contractKeysCsv,
                requirePhysical, false, false, requestedBy, leaseTtlSeconds);
    }

    public CdpWarehouseE2eCertificationScheduler(
            CdpWarehouseE2eCertificationRunService runService,
            @Value("${canvas.warehouse.e2e-certification-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.e2e-certification-scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.e2e-certification-scheduler.window-minutes:60}") long windowMinutes,
            @Value("${canvas.warehouse.e2e-certification-scheduler.mode:HYBRID}") String mode,
            @Value("${canvas.warehouse.e2e-certification-scheduler.contract-keys:}") String contractKeysCsv,
            @Value("${canvas.warehouse.e2e-certification-scheduler.require-physical:true}") boolean requirePhysical,
            @Value("${canvas.warehouse.e2e-certification-scheduler.require-realtime:true}") boolean requireRealtime,
            @Value("${canvas.warehouse.e2e-certification-scheduler.requested-by:e2e-certification-scheduler}") String requestedBy,
            @Value("${canvas.warehouse.e2e-certification-scheduler.lease-ttl-seconds:60}") long leaseTtlSeconds) {
        this(runService, null, enabled, tenantId, windowMinutes, mode, contractKeysCsv,
                requirePhysical, requireRealtime, false, requestedBy, leaseTtlSeconds);
    }

    public CdpWarehouseE2eCertificationScheduler(
            CdpWarehouseE2eCertificationRunService runService,
            @Value("${canvas.warehouse.e2e-certification-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.e2e-certification-scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.e2e-certification-scheduler.window-minutes:60}") long windowMinutes,
            @Value("${canvas.warehouse.e2e-certification-scheduler.mode:HYBRID}") String mode,
            @Value("${canvas.warehouse.e2e-certification-scheduler.contract-keys:}") String contractKeysCsv,
            @Value("${canvas.warehouse.e2e-certification-scheduler.require-physical:true}") boolean requirePhysical,
            @Value("${canvas.warehouse.e2e-certification-scheduler.require-realtime:true}") boolean requireRealtime,
            @Value("${canvas.warehouse.e2e-certification-scheduler.require-data-path-proof:true}") boolean requireDataPathProof,
            @Value("${canvas.warehouse.e2e-certification-scheduler.requested-by:e2e-certification-scheduler}") String requestedBy,
            @Value("${canvas.warehouse.e2e-certification-scheduler.lease-ttl-seconds:60}") long leaseTtlSeconds) {
        this(runService, null, enabled, tenantId, windowMinutes, mode, contractKeysCsv,
                requirePhysical, requireRealtime, requireDataPathProof, requestedBy, leaseTtlSeconds);
    }

    public CdpWarehouseE2eCertificationScheduler(
            CdpWarehouseE2eCertificationRunService runService,
            CdpWarehouseJobLeaseService leaseService,
            boolean enabled,
            Long tenantId,
            long windowMinutes,
            String mode,
            String contractKeysCsv,
            boolean requirePhysical,
            String requestedBy,
            long leaseTtlSeconds) {
        this(runService, leaseService, enabled, tenantId, windowMinutes, mode, contractKeysCsv,
                requirePhysical, false, false, requestedBy, leaseTtlSeconds);
    }

    @Autowired
    public CdpWarehouseE2eCertificationScheduler(
            CdpWarehouseE2eCertificationRunService runService,
            CdpWarehouseJobLeaseService leaseService,
            @Value("${canvas.warehouse.e2e-certification-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.e2e-certification-scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.e2e-certification-scheduler.window-minutes:60}") long windowMinutes,
            @Value("${canvas.warehouse.e2e-certification-scheduler.mode:HYBRID}") String mode,
            @Value("${canvas.warehouse.e2e-certification-scheduler.contract-keys:}") String contractKeysCsv,
            @Value("${canvas.warehouse.e2e-certification-scheduler.require-physical:true}") boolean requirePhysical,
            @Value("${canvas.warehouse.e2e-certification-scheduler.require-realtime:true}") boolean requireRealtime,
            @Value("${canvas.warehouse.e2e-certification-scheduler.require-data-path-proof:true}") boolean requireDataPathProof,
            @Value("${canvas.warehouse.e2e-certification-scheduler.requested-by:e2e-certification-scheduler}") String requestedBy,
            @Value("${canvas.warehouse.e2e-certification-scheduler.lease-ttl-seconds:60}") long leaseTtlSeconds) {
        this.runService = runService;
        this.leaseService = leaseService;
        this.enabled = enabled;
        this.tenantId = tenantId == null ? 0L : tenantId;
        this.windowMinutes = Math.max(windowMinutes, 1);
        this.mode = mode == null || mode.isBlank() ? "HYBRID" : mode.trim().toUpperCase();
        this.contractKeysCsv = contractKeysCsv == null ? "" : contractKeysCsv;
        this.requirePhysical = requirePhysical;
        this.requireRealtime = requireRealtime;
        this.requireDataPathProof = requireDataPathProof;
        this.requestedBy = requestedBy == null || requestedBy.isBlank()
                ? "e2e-certification-scheduler"
                : requestedBy.trim();
        this.leaseTtlSeconds = Math.max(leaseTtlSeconds, 1);
    }

    @Scheduled(fixedDelayString = "${canvas.warehouse.e2e-certification-scheduler.fixed-delay-ms:300000}")
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
            LocalDateTime to = LocalDateTime.now();
            LocalDateTime from = to.minusMinutes(windowMinutes);
            runService.run(tenantId, from, to, mode, contractKeys(),
                    requirePhysical, requireRealtime, requireDataPathProof, requestedBy);
            return true;
        } finally {
            running.set(false);
        }
    }

    private List<String> contractKeys() {
        if (contractKeysCsv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(contractKeysCsv.split(","))
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private Duration leaseTtl() {
        return Duration.ofSeconds(leaseTtlSeconds);
    }
}
