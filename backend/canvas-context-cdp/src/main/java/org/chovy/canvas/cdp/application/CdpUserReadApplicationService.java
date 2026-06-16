package org.chovy.canvas.cdp.application;

import java.util.List;

import org.chovy.canvas.cdp.api.CdpUserReadFacade;
import org.chovy.canvas.cdp.domain.CdpUserReadCatalog;
import org.springframework.stereotype.Service;

/**
 * 编排 CdpUserRead 的应用服务流程。
 */
@Service
public class CdpUserReadApplicationService implements CdpUserReadFacade {

    /**
     * 领域目录组件。
     */
    private final CdpUserReadCatalog catalog;

    /**
     * 创建当前组件实例。
     */
    public CdpUserReadApplicationService() {
        this(new CdpUserReadCatalog());
    }

    CdpUserReadApplicationService(CdpUserReadCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 查询Users列表。
     */
    @Override
    public List<CdpUserRowView> listUsers(Long tenantId, String keyword) {
        return catalog.listUsers(tenantId, keyword);
    }

    /**
     * 返回user。
     */
    @Override
    public CdpUserProfileView getUser(Long tenantId, String userId) {
        return catalog.getUser(tenantId, userId);
    }

    /**
     * 返回insight。
     */
    @Override
    public CdpUserInsightView getInsight(Long tenantId, String userId) {
        return catalog.getInsight(tenantId, userId);
    }
}
