package org.chovy.canvas.domain.warehouse;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CdpWarehouseTableDriftIncidentService {

    private static final String STATUS_PASS = "PASS";

    private final CdpWarehouseTableGovernanceService tableGovernanceService;
    private final CdpWarehouseIncidentService incidentService;

    public CdpWarehouseTableDriftIncidentService(CdpWarehouseTableGovernanceService tableGovernanceService,
                                                 CdpWarehouseIncidentService incidentService) {
        this.tableGovernanceService = tableGovernanceService;
        this.incidentService = incidentService;
    }

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
        for (CdpWarehouseTableGovernanceService.InspectionReport report : reports) {
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
        return new ScanResult(summary.tenantId(), live, reports.size(), opened, resolved, skipped, failed);
    }

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

    private String normalizeOperator(String operator) {
        return operator == null || operator.isBlank() ? "warehouse-table-drift" : operator.trim();
    }

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
