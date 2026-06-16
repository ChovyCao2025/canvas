package org.chovy.canvas.cdp.application;

import java.time.Clock;
import java.util.List;

import org.chovy.canvas.cdp.api.CdpTagOperationFacade;
import org.chovy.canvas.cdp.domain.CdpTagOperationCatalog;
import org.springframework.stereotype.Service;

/**
 * 编排 CdpTagOperation 的应用服务流程。
 */
@Service
public class CdpTagOperationApplicationService implements CdpTagOperationFacade {

    /**
     * 领域目录组件。
     */
    private final CdpTagOperationCatalog catalog;

    /**
     * 创建当前组件实例。
     */
    public CdpTagOperationApplicationService() {
        this(Clock.systemDefaultZone());
    }

    CdpTagOperationApplicationService(Clock clock) {
        this(new CdpTagOperationCatalog(clock));
    }

    CdpTagOperationApplicationService(CdpTagOperationCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 创建create。
     */
    @Override
    public TagOperationView create(Long tenantId, BatchTagCommand command, String actor) {
        return catalog.create(tenantId, command, actor);
    }

    /**
     * 查询Recent列表。
     */
    @Override
    public List<TagOperationView> listRecent(Long tenantId, int limit) {
        return catalog.listRecent(tenantId, limit);
    }

    /**
     * 返回get。
     */
    @Override
    public TagOperationView get(Long tenantId, Long id) {
        return catalog.get(tenantId, id);
    }

    /**
     * 执行 retryFailed 对应的 CDP 业务操作。
     */
    @Override
    public TagOperationView retryFailed(Long tenantId, Long id, String actor) {
        return catalog.retryFailed(tenantId, id, actor);
    }
}
