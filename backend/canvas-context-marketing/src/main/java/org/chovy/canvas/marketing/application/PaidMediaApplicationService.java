package org.chovy.canvas.marketing.application;

import java.time.Clock;
import java.util.List;

import org.chovy.canvas.marketing.api.PaidMediaFacade;
import org.chovy.canvas.marketing.domain.PaidMediaCatalog;
import org.springframework.stereotype.Service;

@Service
public class PaidMediaApplicationService implements PaidMediaFacade {

    private final PaidMediaCatalog catalog;

    public PaidMediaApplicationService() {
        this(Clock.systemDefaultZone());
    }

    PaidMediaApplicationService(Clock clock) {
        this(new PaidMediaCatalog(clock));
    }

    PaidMediaApplicationService(PaidMediaCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public DestinationView upsertDestination(Long tenantId, DestinationCommand command, String actor) {
        return catalog.upsertDestination(tenantId, command, actor);
    }

    @Override
    public SyncRunView syncAudience(Long tenantId, SyncCommand command, String actor) {
        return catalog.syncAudience(tenantId, command, actor);
    }

    @Override
    public List<SyncRunView> runs(Long tenantId, RunQuery query) {
        return catalog.runs(tenantId, query);
    }

    @Override
    public List<MemberView> members(Long tenantId, MemberQuery query) {
        return catalog.members(tenantId, query);
    }

    @Override
    public void registerAudience(Long tenantId, Long audienceId, boolean active) {
        catalog.registerAudience(tenantId, audienceId, active);
    }

    @Override
    public void registerProfile(Long tenantId, String userId, String email, String phone) {
        catalog.registerProfile(tenantId, userId, email, phone);
    }

    @Override
    public void grantConsent(Long tenantId, String userId, String channel) {
        catalog.grantConsent(tenantId, userId, channel);
    }
}
