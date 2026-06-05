package org.chovy.canvas.domain.warehouse;

import org.springframework.stereotype.Service;

@Service
public class CdpWarehouseRealtimePipelineIncidentService {

    private static final String STATUS_PASS = "PASS";

    private final CdpWarehouseRealtimePipelineService pipelineService;
    private final CdpWarehouseIncidentService incidentService;

    public CdpWarehouseRealtimePipelineIncidentService(CdpWarehouseRealtimePipelineService pipelineService,
                                                       CdpWarehouseIncidentService incidentService) {
        this.pipelineService = pipelineService;
        this.incidentService = incidentService;
    }

    public ScanResult scan(Long tenantId, int recentLimit) {
        CdpWarehouseRealtimePipelineService.PipelineStatusSummary status =
                pipelineService.status(tenantId, recentLimit);
        int opened = 0;
        int skipped = 0;
        int failed = 0;
        for (CdpWarehouseRealtimePipelineService.PipelineRuntimeView pipeline : status.pipelines()) {
            if (pipeline == null || STATUS_PASS.equals(pipeline.runtimeStatus())) {
                skipped++;
                continue;
            }
            try {
                incidentService.recordRealtimePipelineIncident(toIncidentInput(status.tenantId(), pipeline));
                opened++;
            } catch (RuntimeException ignored) {
                failed++;
            }
        }
        return new ScanResult(status.tenantId(), status.total(), opened, skipped, failed);
    }

    private CdpWarehouseIncidentService.RealtimePipelineIncidentInput toIncidentInput(
            Long tenantId,
            CdpWarehouseRealtimePipelineService.PipelineRuntimeView runtime) {
        CdpWarehouseRealtimePipelineService.PipelineContractView contract = runtime.contract();
        return new CdpWarehouseIncidentService.RealtimePipelineIncidentInput(
                tenantId,
                contract == null ? null : contract.id(),
                contract == null ? null : contract.pipelineKey(),
                contract == null ? null : contract.sinkRef(),
                runtime.runtimeStatus(),
                runtime.message(),
                runtime.lastCheckpointId(),
                runtime.lastCheckpointAt(),
                runtime.lastLagMs(),
                runtime.reasons());
    }

    public record ScanResult(
            Long tenantId,
            int total,
            int opened,
            int skipped,
            int failed) {
    }
}
