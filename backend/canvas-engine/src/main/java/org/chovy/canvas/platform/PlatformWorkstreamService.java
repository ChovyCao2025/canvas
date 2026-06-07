package org.chovy.canvas.platform;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class PlatformWorkstreamService {

    private final WorkstreamRepository repository;

    public PlatformWorkstreamService(WorkstreamRepository repository) {
        this.repository = repository;
    }

    public List<WorkstreamStatus> statuses() {
        return repository.list().stream()
                .map(this::toStatus)
                .toList();
    }

    public Workstream requireExecutableChildSpec(String workstreamKey) {
        String normalizedKey = normalizeKey(workstreamKey);
        Workstream workstream = repository.get(normalizedKey);
        if (workstream == null) {
            throw new IllegalArgumentException("unknown workstream " + normalizedKey);
        }
        if (requiresMissingChildSpec(workstream)) {
            throw new IllegalStateException(normalizedKey + " requires a child spec before implementation");
        }
        return workstream;
    }

    private WorkstreamStatus toStatus(Workstream workstream) {
        String status = requiresMissingChildSpec(workstream)
                ? "BLOCKED_CHILD_SPEC_REQUIRED"
                : "READY_FOR_CHILD_EXECUTION";
        return new WorkstreamStatus(
                workstream.workstreamKey(),
                workstream.displayName(),
                workstream.priority(),
                status,
                workstream.childSpecPath(),
                workstream.summary());
    }

    private static boolean requiresMissingChildSpec(Workstream workstream) {
        return workstream.requiresChildSpec()
                && (workstream.childSpecPath() == null || workstream.childSpecPath().isBlank());
    }

    private static String normalizeKey(String workstreamKey) {
        String normalized = Objects.requireNonNull(workstreamKey, "workstreamKey")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9][a-z0-9-]{0,127}")) {
            throw new IllegalArgumentException("invalid workstream key: " + workstreamKey);
        }
        return normalized;
    }

    public record Workstream(
            String workstreamKey,
            String displayName,
            String priority,
            boolean requiresChildSpec,
            String childSpecPath,
            String summary) {
    }

    public record WorkstreamStatus(
            String workstreamKey,
            String displayName,
            String priority,
            String status,
            String childSpecPath,
            String summary) {
    }

    public interface WorkstreamRepository {
        List<Workstream> list();

        Workstream get(String workstreamKey);
    }
}
