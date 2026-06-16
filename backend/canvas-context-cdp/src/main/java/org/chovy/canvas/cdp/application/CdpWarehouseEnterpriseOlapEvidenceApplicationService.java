package org.chovy.canvas.cdp.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseEnterpriseOlapEvidenceFacade;
import org.chovy.canvas.cdp.domain.CdpWarehouseEnterpriseOlapEvidenceCatalog;
import org.springframework.stereotype.Service;

/**
 * 编排 CdpWarehouseEnterpriseOlapEvidence 的应用服务流程。
 */
@Service
public class CdpWarehouseEnterpriseOlapEvidenceApplicationService implements CdpWarehouseEnterpriseOlapEvidenceFacade {

    /**
     * 领域目录组件。
     */
    private final CdpWarehouseEnterpriseOlapEvidenceCatalog catalog;

    /**
     * 创建当前组件实例。
     */
    public CdpWarehouseEnterpriseOlapEvidenceApplicationService() {
        this(new CdpWarehouseEnterpriseOlapEvidenceCatalog());
    }

    CdpWarehouseEnterpriseOlapEvidenceApplicationService(CdpWarehouseEnterpriseOlapEvidenceCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 执行 record 对应的 CDP 业务操作。
     */
    @Override
    public Map<String, Object> record(Long tenantId, EvidenceCommand command, String actor) {
        return catalog.record(tenantIdOrDefault(tenantId), command, defaultString(actor, "system"));
    }

    /**
     * 执行 latest 对应的 CDP 业务操作。
     */
    @Override
    public Map<String, Object> latest(Long tenantId) {
        return catalog.latest(tenantIdOrDefault(tenantId));
    }

    /**
     * 执行 proof 对应的 CDP 业务操作。
     */
    @Override
    public List<Map<String, Object>> proof(Long tenantId) {
        return catalog.proof(tenantIdOrDefault(tenantId));
    }

    /**
     * 执行 collect 对应的 CDP 业务操作。
     */
    @Override
    public Map<String, Object> collect(Long tenantId, String triggerType, String actor) {
        return catalog.collect(tenantIdOrDefault(tenantId), defaultString(triggerType, "MANUAL"),
                defaultString(actor, "system"));
    }

    /**
     * 执行 collections 对应的 CDP 业务操作。
     */
    @Override
    public List<Map<String, Object>> collections(Long tenantId, Integer limit) {
        return catalog.collections(tenantIdOrDefault(tenantId), limit);
    }

    /**
     * 执行 tenantIdOrDefault 对应的 CDP 业务操作。
     */
    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 返回默认的String。
     */
    private static String defaultString(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
