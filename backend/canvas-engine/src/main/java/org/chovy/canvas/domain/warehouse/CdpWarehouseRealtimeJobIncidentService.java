package org.chovy.canvas.domain.warehouse;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CdpWarehouseRealtimeJobIncidentService {

    private static final String STATUS_PASS = "PASS";

    private final CdpWarehouseRealtimeJobControlService jobControlService;
    private final CdpWarehouseIncidentService incidentService;

    public CdpWarehouseRealtimeJobIncidentService(CdpWarehouseRealtimeJobControlService jobControlService,
                                                  CdpWarehouseIncidentService incidentService) {
        this.jobControlService = jobControlService;
        this.incidentService = incidentService;
    }

    public ScanResult scan(Long tenantId, String pipelineKey, long maxHeartbeatAgeSeconds, int limit) {
        CdpWarehouseRealtimeJobControlService.JobStatusSummary status =
                jobControlService.status(tenantId, pipelineKey, maxHeartbeatAgeSeconds, limit);
        int opened = 0;
        int skipped = 0;
        int failed = 0;
        List<CdpWarehouseRealtimeJobControlService.JobInstanceView> jobs =
                status.jobs() == null ? List.of() : status.jobs();
        for (CdpWarehouseRealtimeJobControlService.JobInstanceView job : jobs) {
            if (job == null || STATUS_PASS.equals(job.healthStatus())) {
                skipped++;
                continue;
            }
            try {
                incidentService.recordRealtimeJobIncident(toIncidentInput(status.tenantId(), job));
                opened++;
            } catch (RuntimeException ignored) {
                failed++;
            }
        }
        return new ScanResult(status.tenantId(), status.total(), opened, skipped, failed);
    }

    private CdpWarehouseIncidentService.RealtimeJobIncidentInput toIncidentInput(
            Long tenantId,
            CdpWarehouseRealtimeJobControlService.JobInstanceView job) {
        return new CdpWarehouseIncidentService.RealtimeJobIncidentInput(
                tenantId,
                job.id(),
                job.pipelineKey(),
                job.jobKey(),
                job.engineType(),
                job.engineJobId(),
                job.deploymentRef(),
                job.runtimeStatus(),
                job.desiredStatus(),
                job.healthStatus(),
                job.lastHeartbeatAt(),
                job.ownerName(),
                job.lastErrorMessage(),
                job.reasons());
    }

    public record ScanResult(
            Long tenantId,
            int total,
            int opened,
            int skipped,
            int failed) {
    }
}
