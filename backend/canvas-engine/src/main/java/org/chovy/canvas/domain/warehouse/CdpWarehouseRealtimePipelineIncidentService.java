package org.chovy.canvas.domain.warehouse;

import org.springframework.stereotype.Service;

@Service
/**
 * CdpWarehouseRealtimePipelineIncidentService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class CdpWarehouseRealtimePipelineIncidentService {

    private static final String STATUS_PASS = "PASS";

    private final CdpWarehouseRealtimePipelineService pipelineService;
    private final CdpWarehouseIncidentService incidentService;

    /**
     * 初始化 CdpWarehouseRealtimePipelineIncidentService 实例。
     *
     * @param pipelineService 依赖组件，用于完成数据访问或外部能力调用。
     * @param incidentService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseRealtimePipelineIncidentService(CdpWarehouseRealtimePipelineService pipelineService,
                                                       CdpWarehouseIncidentService incidentService) {
        this.pipelineService = pipelineService;
        this.incidentService = incidentService;
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param recentLimit recent limit 参数，用于 scan 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    public ScanResult scan(Long tenantId, int recentLimit) {
        CdpWarehouseRealtimePipelineService.PipelineStatusSummary status =
                pipelineService.status(tenantId, recentLimit);
        int opened = 0;
        int skipped = 0;
        int failed = 0;
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (CdpWarehouseRealtimePipelineService.PipelineRuntimeView pipeline : status.pipelines()) {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new ScanResult(status.tenantId(), status.total(), opened, skipped, failed);
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param runtime 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回组装或转换后的结果对象。
     */
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
