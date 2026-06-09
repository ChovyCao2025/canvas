package org.chovy.canvas.domain.warehouse;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
/**
 * CdpWarehouseTableDriftIncidentService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class CdpWarehouseTableDriftIncidentService {

    private static final String STATUS_PASS = "PASS";

    private final CdpWarehouseTableGovernanceService tableGovernanceService;
    private final CdpWarehouseIncidentService incidentService;

    /**
     * 初始化 CdpWarehouseTableDriftIncidentService 实例。
     *
     * @param tableGovernanceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param incidentService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseTableDriftIncidentService(CdpWarehouseTableGovernanceService tableGovernanceService,
                                                 CdpWarehouseIncidentService incidentService) {
        this.tableGovernanceService = tableGovernanceService;
        this.incidentService = incidentService;
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param live live 参数，用于 scan 流程中的校验、计算或对象转换。
     * @param inspectedBy inspected by 参数，用于 scan 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    public ScanResult scan(Long tenantId, boolean live, String inspectedBy) {
        CdpWarehouseTableGovernanceService.InspectionSummary summary = live
                ? tableGovernanceService.inspectLiveAll(tenantId, normalizeOperator(inspectedBy))
                : tableGovernanceService.inspectAll(tenantId, normalizeOperator(inspectedBy));
        List<CdpWarehouseTableGovernanceService.InspectionReport> reports =
                summary.reports() == null ? List.of() : summary.reports();
        int opened = 0;
        int resolved = 0;
        int skipped = 0;
        int failed = 0;
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (CdpWarehouseTableGovernanceService.InspectionReport report : reports) {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (report == null) {
                skipped++;
                continue;
            }
            if (STATUS_PASS.equalsIgnoreCase(report.status())) {
                try {
                    if (incidentService.resolveTableDriftIncident(
                            summary.tenantId(), report.tableKey(), normalizeOperator(inspectedBy))) {
                        resolved++;
                    } else {
                        skipped++;
                    }
                } catch (RuntimeException ignored) {
                    failed++;
                }
                continue;
            }
            try {
                incidentService.recordTableDriftIncident(toIncidentInput(summary.tenantId(), report));
                opened++;
            } catch (RuntimeException ignored) {
                failed++;
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new ScanResult(summary.tenantId(), live, reports.size(), opened, resolved, skipped, failed);
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param report report 参数，用于 toIncidentInput 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private CdpWarehouseIncidentService.TableDriftIncidentInput toIncidentInput(
            Long tenantId,
            CdpWarehouseTableGovernanceService.InspectionReport report) {
        return new CdpWarehouseIncidentService.TableDriftIncidentInput(
                tenantId,
                report.id(),
                report.tableKey(),
                report.physicalName(),
                report.status(),
                report.checkedItems(),
                report.violationCount(),
                report.violations(),
                report.message(),
                report.ddlAssetPath(),
                report.inspectedAt());
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeOperator(String operator) {
        return operator == null || operator.isBlank() ? "warehouse-table-drift" : operator.trim();
    }

    /**
     * ScanResult 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record ScanResult(
            Long tenantId,
            boolean live,
            int total,
            int opened,
            int resolved,
            int skipped,
            int failed) {
    }
}
