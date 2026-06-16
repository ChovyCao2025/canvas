package org.chovy.canvas.marketing.application;

import java.time.Clock;
import java.util.List;

import org.chovy.canvas.marketing.api.PaidMediaFacade;
import org.chovy.canvas.marketing.domain.PaidMediaCatalog;
import org.springframework.stereotype.Service;

/**
 * 编排PaidMedia相关的应用层用例。
 */
@Service
public class PaidMediaApplicationService implements PaidMediaFacade {

    /**
     * 承载该应用服务的内存目录。
     */
    private final PaidMediaCatalog catalog;

    /**
     * 创建PaidMediaApplicationService实例。
     */
    public PaidMediaApplicationService() {
        this(Clock.systemDefaultZone());
    }

    PaidMediaApplicationService(Clock clock) {
        this(new PaidMediaCatalog(clock));
    }

    PaidMediaApplicationService(PaidMediaCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 执行upsertDestination业务操作。
     */
    @Override
    public DestinationView upsertDestination(Long tenantId, DestinationCommand command, String actor) {
        return catalog.upsertDestination(tenantId, command, actor);
    }

    /**
     * 执行syncAudience业务操作。
     */
    @Override
    public SyncRunView syncAudience(Long tenantId, SyncCommand command, String actor) {
        return catalog.syncAudience(tenantId, command, actor);
    }

    /**
     * 执行runs业务操作。
     */
    @Override
    public List<SyncRunView> runs(Long tenantId, RunQuery query) {
        return catalog.runs(tenantId, query);
    }

    /**
     * 执行members业务操作。
     */
    @Override
    public List<MemberView> members(Long tenantId, MemberQuery query) {
        return catalog.members(tenantId, query);
    }

    /**
     * 执行registerAudience业务操作。
     */
    @Override
    public void registerAudience(Long tenantId, Long audienceId, boolean active) {
        catalog.registerAudience(tenantId, audienceId, active);
    }

    /**
     * 执行registerProfile业务操作。
     */
    @Override
    public void registerProfile(Long tenantId, String userId, String email, String phone) {
        catalog.registerProfile(tenantId, userId, email, phone);
    }

    /**
     * 执行grantConsent业务操作。
     */
    @Override
    public void grantConsent(Long tenantId, String userId, String channel) {
        catalog.grantConsent(tenantId, userId, channel);
    }
}
