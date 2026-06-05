package org.chovy.canvas.domain.warehouse;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CdpWarehouseReadinessIncidentService {

    private static final String STATUS_PASS = "PASS";
    private static final String SECTION_INCIDENTS = "incidents";

    private final CdpWarehouseReadinessService readinessService;
    private final CdpWarehouseIncidentService incidentService;

    public CdpWarehouseReadinessIncidentService(CdpWarehouseReadinessService readinessService,
                                                CdpWarehouseIncidentService incidentService) {
        this.readinessService = readinessService;
        this.incidentService = incidentService;
    }

    public ScanResult scan(Long tenantId) {
        CdpWarehouseReadinessService.ReadinessSummary readiness = readinessService.readiness(tenantId);
        if (readiness == null) {
            return new ScanResult(normalizeTenant(tenantId), "UNKNOWN", 0, 0, 0, 0);
        }
        List<CdpWarehouseReadinessService.ReadinessSection> sections =
                readiness.sections() == null ? List.of() : readiness.sections();
        int opened = 0;
        int skipped = 0;
        int failed = 0;
        for (CdpWarehouseReadinessService.ReadinessSection section : sections) {
            if (shouldSkip(section)) {
                skipped++;
                continue;
            }
            try {
                incidentService.recordReadinessIncident(toIncidentInput(readiness, section));
                opened++;
            } catch (RuntimeException ignored) {
                failed++;
            }
        }
        return new ScanResult(readiness.tenantId(), readiness.status(), sections.size(), opened, skipped, failed);
    }

    private CdpWarehouseIncidentService.ReadinessIncidentInput toIncidentInput(
            CdpWarehouseReadinessService.ReadinessSummary readiness,
            CdpWarehouseReadinessService.ReadinessSection section) {
        return new CdpWarehouseIncidentService.ReadinessIncidentInput(
                readiness.tenantId(),
                section.key(),
                readiness.status(),
                section.status(),
                section.reason(),
                readiness.generatedAt());
    }

    private boolean shouldSkip(CdpWarehouseReadinessService.ReadinessSection section) {
        if (section == null || !hasText(section.status())) {
            return true;
        }
        if (STATUS_PASS.equalsIgnoreCase(section.status().trim())) {
            return true;
        }
        return hasText(section.key()) && SECTION_INCIDENTS.equalsIgnoreCase(section.key().trim());
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record ScanResult(
            Long tenantId,
            String readinessStatus,
            int totalSections,
            int opened,
            int skipped,
            int failed) {
    }
}
