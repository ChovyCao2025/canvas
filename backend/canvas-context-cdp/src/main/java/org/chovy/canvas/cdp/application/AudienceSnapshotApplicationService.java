package org.chovy.canvas.cdp.application;

import org.chovy.canvas.cdp.api.AudienceSnapshotFacade;
import org.chovy.canvas.cdp.api.AudienceSnapshotLockCommand;
import org.chovy.canvas.cdp.api.AudienceSnapshotView;
import org.chovy.canvas.cdp.domain.AudienceSnapshot;
import org.chovy.canvas.cdp.domain.AudienceSnapshotMode;
import org.chovy.canvas.cdp.domain.AudienceSnapshotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AudienceSnapshotApplicationService implements AudienceSnapshotFacade {

    private final AudienceSnapshotRepository repository;
    private final Clock clock;
    private final int maxSnapshotUsers;

    @Autowired
    public AudienceSnapshotApplicationService(AudienceSnapshotRepository repository,
                                              @Value("${canvas.audience.snapshot.max-users:100000}") int maxSnapshotUsers) {
        this(repository, Clock.systemDefaultZone(), maxSnapshotUsers);
    }

    AudienceSnapshotApplicationService(AudienceSnapshotRepository repository, Clock clock, int maxSnapshotUsers) {
        this.repository = repository;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
        this.maxSnapshotUsers = maxSnapshotUsers <= 0 ? 100000 : maxSnapshotUsers;
    }

    @Override
    public AudienceSnapshotView lockSnapshot(AudienceSnapshotLockCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("audience snapshot command is required");
        }
        List<String> users = repository.resolveUsers(command.audienceId()).stream()
                .filter(userId -> userId != null && !userId.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        if (users.size() > maxSnapshotUsers) {
            throw new IllegalStateException("AUDIENCE_SNAPSHOT_LIMIT: audienceId=" + command.audienceId()
                    + " size=" + users.size() + " max=" + maxSnapshotUsers);
        }
        AudienceSnapshot saved = repository.save(new AudienceSnapshot(
                null,
                command.audienceId(),
                command.canvasId(),
                command.canvasVersionId(),
                command.nodeId(),
                AudienceSnapshotMode.STATIC_LOCKED,
                users,
                command.operator(),
                LocalDateTime.now(clock)));
        return toView(saved);
    }

    @Override
    public String defaultModeForAudience(Long audienceId) {
        return AudienceSnapshotMode.normalize(repository.defaultSnapshotMode(audienceId)).name();
    }

    @Override
    public List<String> users(Long snapshotId) {
        AudienceSnapshot snapshot = requiredSnapshot(snapshotId);
        return snapshot.userIds() == null ? List.of() : snapshot.userIds();
    }

    @Override
    public boolean contains(Long snapshotId, String userId) {
        return users(snapshotId).contains(userId);
    }

    private AudienceSnapshot requiredSnapshot(Long snapshotId) {
        if (snapshotId == null || snapshotId <= 0) {
            throw new IllegalArgumentException("snapshotId is required");
        }
        AudienceSnapshot snapshot = repository.findSnapshot(snapshotId);
        if (snapshot == null) {
            throw new IllegalArgumentException("Audience snapshot not found: " + snapshotId);
        }
        return snapshot;
    }

    private AudienceSnapshotView toView(AudienceSnapshot snapshot) {
        return new AudienceSnapshotView(
                snapshot.id(),
                snapshot.audienceId(),
                snapshot.canvasId(),
                snapshot.canvasVersionId(),
                snapshot.nodeId(),
                snapshot.snapshotMode().name(),
                snapshot.userCount(),
                snapshot.createdBy(),
                snapshot.createdAt());
    }
}
