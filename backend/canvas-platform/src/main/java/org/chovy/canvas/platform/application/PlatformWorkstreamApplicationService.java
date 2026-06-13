package org.chovy.canvas.platform.application;

import org.chovy.canvas.platform.api.PlatformWorkstreamFacade;
import org.chovy.canvas.platform.api.WorkstreamStatusView;
import org.chovy.canvas.platform.domain.PlatformWorkstream;
import org.chovy.canvas.platform.domain.PlatformWorkstreamReadinessPolicy;
import org.chovy.canvas.platform.domain.PlatformWorkstreamRepository;
import org.chovy.canvas.platform.domain.WorkstreamKey;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlatformWorkstreamApplicationService implements PlatformWorkstreamFacade {

    private final PlatformWorkstreamRepository repository;

    public PlatformWorkstreamApplicationService(PlatformWorkstreamRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<WorkstreamStatusView> statuses() {
        return repository.list().stream()
                .map(this::toView)
                .toList();
    }

    @Override
    public WorkstreamStatusView requireExecutableChildSpec(String workstreamKey) {
        String normalizedKey = new WorkstreamKey(workstreamKey).value();
        PlatformWorkstream workstream = repository.get(normalizedKey);
        if (workstream == null) {
            throw new IllegalArgumentException("unknown workstream " + normalizedKey);
        }
        if (PlatformWorkstreamReadinessPolicy.requiresMissingChildSpec(workstream)) {
            throw new IllegalStateException(normalizedKey + " requires a child spec before implementation");
        }
        return toView(workstream);
    }

    private WorkstreamStatusView toView(PlatformWorkstream workstream) {
        return new WorkstreamStatusView(
                workstream.workstreamKey(),
                workstream.displayName(),
                workstream.priority(),
                PlatformWorkstreamReadinessPolicy.statusFor(workstream),
                workstream.childSpecPath(),
                workstream.summary());
    }
}
