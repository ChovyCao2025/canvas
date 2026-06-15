package org.chovy.canvas.cdp.application;

import java.util.List;

import org.chovy.canvas.cdp.api.CdpUserReadFacade;
import org.chovy.canvas.cdp.domain.CdpUserReadCatalog;
import org.springframework.stereotype.Service;

@Service
public class CdpUserReadApplicationService implements CdpUserReadFacade {

    private final CdpUserReadCatalog catalog;

    public CdpUserReadApplicationService() {
        this(new CdpUserReadCatalog());
    }

    CdpUserReadApplicationService(CdpUserReadCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public List<CdpUserRowView> listUsers(Long tenantId, String keyword) {
        return catalog.listUsers(tenantId, keyword);
    }

    @Override
    public CdpUserProfileView getUser(Long tenantId, String userId) {
        return catalog.getUser(tenantId, userId);
    }

    @Override
    public CdpUserInsightView getInsight(Long tenantId, String userId) {
        return catalog.getInsight(tenantId, userId);
    }
}
