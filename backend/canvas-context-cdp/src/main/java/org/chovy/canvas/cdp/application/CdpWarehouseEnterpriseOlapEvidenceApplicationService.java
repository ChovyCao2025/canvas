package org.chovy.canvas.cdp.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseEnterpriseOlapEvidenceFacade;
import org.chovy.canvas.cdp.domain.CdpWarehouseEnterpriseOlapEvidenceCatalog;
import org.springframework.stereotype.Service;

@Service
public class CdpWarehouseEnterpriseOlapEvidenceApplicationService implements CdpWarehouseEnterpriseOlapEvidenceFacade {

    private final CdpWarehouseEnterpriseOlapEvidenceCatalog catalog;

    public CdpWarehouseEnterpriseOlapEvidenceApplicationService() {
        this(new CdpWarehouseEnterpriseOlapEvidenceCatalog());
    }

    CdpWarehouseEnterpriseOlapEvidenceApplicationService(CdpWarehouseEnterpriseOlapEvidenceCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public Map<String, Object> record(Long tenantId, EvidenceCommand command, String actor) {
        return catalog.record(tenantIdOrDefault(tenantId), command, defaultString(actor, "system"));
    }

    @Override
    public Map<String, Object> latest(Long tenantId) {
        return catalog.latest(tenantIdOrDefault(tenantId));
    }

    @Override
    public List<Map<String, Object>> proof(Long tenantId) {
        return catalog.proof(tenantIdOrDefault(tenantId));
    }

    @Override
    public Map<String, Object> collect(Long tenantId, String triggerType, String actor) {
        return catalog.collect(tenantIdOrDefault(tenantId), defaultString(triggerType, "MANUAL"),
                defaultString(actor, "system"));
    }

    @Override
    public List<Map<String, Object>> collections(Long tenantId, Integer limit) {
        return catalog.collections(tenantIdOrDefault(tenantId), limit);
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private static String defaultString(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
