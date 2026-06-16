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

/**
 * 编排 CdpWarehouseReadiness 的应用服务流程。
 */
@Service
public class CdpWarehouseReadinessApplicationService implements CdpWarehouseReadinessFacade {

    /**
     * 仓储依赖。
     */
    private final CdpWarehouseReadinessRepository repository;

    /**
     * 执行 CdpWarehouseReadinessPolicy 对应的 CDP 业务操作。
     */
    private final CdpWarehouseReadinessPolicy policy = new CdpWarehouseReadinessPolicy();

    /**
     * 时间源。
     */
    private final Clock clock;

    /**
     * 创建当前组件实例。
     */
    @Autowired
    public CdpWarehouseReadinessApplicationService(CdpWarehouseReadinessRepository repository) {
        this(repository, Clock.systemDefaultZone());
    }

    CdpWarehouseReadinessApplicationService(CdpWarehouseReadinessRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 执行 readiness 对应的 CDP 业务操作。
     */
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

    /**
     * 执行 scanIncidents 对应的 CDP 业务操作。
     */
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
