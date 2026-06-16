package org.chovy.canvas.cdp.application;

import java.util.Map;

import org.chovy.canvas.cdp.api.CdpIdentityTypeFacade;
import org.chovy.canvas.cdp.domain.CdpIdentityTypeCatalog;
import org.springframework.stereotype.Service;

/**
 * 编排 CdpIdentityType 的应用服务流程。
 */
@Service
public class CdpIdentityTypeApplicationService implements CdpIdentityTypeFacade {

    /**
     * 领域目录组件。
     */
    private final CdpIdentityTypeCatalog catalog;

    /**
     * 创建当前组件实例。
     */
    public CdpIdentityTypeApplicationService() {
        this(new CdpIdentityTypeCatalog());
    }

    CdpIdentityTypeApplicationService(CdpIdentityTypeCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 查询list列表。
     */
    @Override
    public Map<String, Object> list(Integer enabled, Integer allowImport) {
        return catalog.list(enabled, allowImport);
    }

    /**
     * 创建create。
     */
    @Override
    public Map<String, Object> create(Map<String, Object> payload) {
        return catalog.create(safePayload(payload));
    }

    /**
     * 更新update。
     */
    @Override
    public Map<String, Object> update(Long id, Map<String, Object> payload) {
        return catalog.update(id, safePayload(payload));
    }

    /**
     * 删除delete。
     */
    @Override
    public Map<String, Object> delete(Long id) {
        return catalog.delete(id);
    }

    /**
     * 返回安全的Payload。
     */
    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }
}
