package org.chovy.canvas.conversation.application;

import java.time.Clock;
import java.util.List;

import org.chovy.canvas.conversation.api.DemoSandboxFacade;
import org.chovy.canvas.conversation.domain.DemoSandboxCatalog;
import org.springframework.stereotype.Service;

@Service
public class DemoSandboxApplicationService implements DemoSandboxFacade {

    private final DemoSandboxCatalog catalog;

    public DemoSandboxApplicationService() {
        this(Clock.systemDefaultZone());
    }

    DemoSandboxApplicationService(Clock clock) {
        this(new DemoSandboxCatalog(clock));
    }

    DemoSandboxApplicationService(DemoSandboxCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public SandboxView install(InstallCommand command, String actor) {
        return catalog.install(command, actor);
    }

    @Override
    public ResetResult reset(Long tenantId, String actor) {
        return catalog.reset(tenantId, actor);
    }

    @Override
    public List<SandboxView> expired() {
        return catalog.expired();
    }

    @Override
    public ConversationReplyResult reply(Long tenantId, ConversationReplyCommand command, String actor) {
        return catalog.reply(tenantId, command, actor);
    }
}
