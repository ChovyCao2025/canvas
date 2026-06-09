package org.chovy.canvas.domain.warehouse;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
/**
 * CdpWarehouseRealtimeJobIncidentService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class CdpWarehouseRealtimeJobIncidentService {

    private static final String STATUS_PASS = "PASS";

    private final CdpWarehouseRealtimeJobControlService jobControlService;
    private final CdpWarehouseIncidentService incidentService;

    /**
     * 初始化 CdpWarehouseRealtimeJobIncidentService 实例。
     *
     * @param jobControlService 依赖组件，用于完成数据访问或外部能力调用。
     * @param incidentService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseRealtimeJobIncidentService(CdpWarehouseRealtimeJobControlService jobControlService,
                                                  CdpWarehouseIncidentService incidentService) {
        this.jobControlService = jobControlService;
        this.incidentService = incidentService;
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param pipelineKey 业务键，用于在同一租户下定位资源。
     * @param maxHeartbeatAgeSeconds max heartbeat age seconds 参数，用于 scan 流程中的校验、计算或对象转换。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回流程执行后的业务结果。
     */
    public ScanResult scan(Long tenantId, String pipelineKey, long maxHeartbeatAgeSeconds, int limit) {
        CdpWarehouseRealtimeJobControlService.JobStatusSummary status =
                jobControlService.status(tenantId, pipelineKey, maxHeartbeatAgeSeconds, limit);
        int opened = 0;
        int skipped = 0;
        int failed = 0;
        List<CdpWarehouseRealtimeJobControlService.JobInstanceView> jobs =
                status.jobs() == null ? List.of() : status.jobs();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (CdpWarehouseRealtimeJobControlService.JobInstanceView job : jobs) {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new ScanResult(status.tenantId(), status.total(), opened, skipped, failed);
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param job job 参数，用于 toIncidentInput 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * ScanResult 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record ScanResult(
            Long tenantId,
            int total,
            int opened,
            int skipped,
            int failed) {
    }
}
