package org.chovy.canvas.cdp.application;

import java.time.Clock;
import java.util.List;

import org.chovy.canvas.cdp.api.CdpTagOperationFacade;
import org.chovy.canvas.cdp.domain.CdpTagOperationCatalog;
import org.springframework.stereotype.Service;

@Service
public class CdpTagOperationApplicationService implements CdpTagOperationFacade {

    private final CdpTagOperationCatalog catalog;

    public CdpTagOperationApplicationService() {
        this(Clock.systemDefaultZone());
    }

    CdpTagOperationApplicationService(Clock clock) {
        this(new CdpTagOperationCatalog(clock));
    }

    CdpTagOperationApplicationService(CdpTagOperationCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public TagOperationView create(Long tenantId, BatchTagCommand command, String actor) {
        return catalog.create(tenantId, command, actor);
    }

    @Override
    public List<TagOperationView> listRecent(Long tenantId, int limit) {
        return catalog.listRecent(tenantId, limit);
    }

    @Override
    public TagOperationView get(Long tenantId, Long id) {
        return catalog.get(tenantId, id);
    }

    @Override
    public TagOperationView retryFailed(Long tenantId, Long id, String actor) {
        return catalog.retryFailed(tenantId, id, actor);
    }
}
