package org.chovy.canvas.cdp.application;

import org.chovy.canvas.cdp.api.CdpWarehouseReadinessFacade;
import org.chovy.canvas.cdp.api.CdpWarehouseReadinessSectionView;
import org.chovy.canvas.cdp.api.CdpWarehouseReadinessView;
import org.chovy.canvas.cdp.domain.CdpWarehouseReadinessEvidence;
import org.chovy.canvas.cdp.domain.CdpWarehouseReadinessPolicy;
import org.chovy.canvas.cdp.domain.CdpWarehouseReadinessReport;
import org.chovy.canvas.cdp.domain.CdpWarehouseReadinessRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CdpWarehouseReadinessApplicationService implements CdpWarehouseReadinessFacade {

    private final CdpWarehouseReadinessRepository repository;
    private final CdpWarehouseReadinessPolicy policy = new CdpWarehouseReadinessPolicy();
    private final Clock clock;

    @Autowired
    public CdpWarehouseReadinessApplicationService(CdpWarehouseReadinessRepository repository) {
        this(repository, Clock.systemDefaultZone());
    }

    CdpWarehouseReadinessApplicationService(CdpWarehouseReadinessRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    @Override
    public CdpWarehouseReadinessView readiness(Long tenantId) {
        Long scopedTenantId = tenantId == null ? 0L : tenantId;
        CdpWarehouseReadinessEvidence evidence = repository.evidence(scopedTenantId);
        CdpWarehouseReadinessReport report = policy.evaluate(evidence, LocalDateTime.now(clock));
        List<CdpWarehouseReadinessSectionView> sections = report.sections().stream()
                .map(section -> new CdpWarehouseReadinessSectionView(
                        section.key(),
                        section.status(),
                        section.reason()))
                .toList();
        return new CdpWarehouseReadinessView(report.tenantId(), report.status(), report.generatedAt(), sections);
    }

    @Override
    public Map<String, Object> scanIncidents(Long tenantId) {
        CdpWarehouseReadinessView view = readiness(tenantId);
        List<Map<String, Object>> incidents = view.sections().stream()
                .filter(section -> !"PASS".equals(section.status()))
                .map(section -> {
                    Map<String, Object> incident = new LinkedHashMap<>();
                    incident.put("section", section.key());
                    incident.put("status", section.status());
                    incident.put("reason", section.reason());
                    return incident;
                })
                .toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tenantId", view.tenantId());
        result.put("status", view.status());
        result.put("scannedAt", view.generatedAt());
        result.put("incidentCount", incidents.size());
        result.put("incidents", incidents);
        return result;
    }
}
