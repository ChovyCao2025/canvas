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

/**
 * 编排 AudienceSnapshot 的应用服务流程。
 */
@Service
public class AudienceSnapshotApplicationService implements AudienceSnapshotFacade {

    /**
     * 仓储依赖。
     */
    private final AudienceSnapshotRepository repository;

    /**
     * 时间源。
     */
    private final Clock clock;

    /**
     * max Snapshot Users。
     */
    private final int maxSnapshotUsers;

    /**
     * 创建当前组件实例。
     */
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

    /**
     * 执行 lockSnapshot 对应的 CDP 业务操作。
     */
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

    /**
     * 返回默认的Mode For Audience。
     */
    @Override
    public String defaultModeForAudience(Long audienceId) {
        return AudienceSnapshotMode.normalize(repository.defaultSnapshotMode(audienceId)).name();
    }

    /**
     * 执行 users 对应的 CDP 业务操作。
     */
    @Override
    public List<String> users(Long snapshotId) {
        AudienceSnapshot snapshot = requiredSnapshot(snapshotId);
        return snapshot.userIds() == null ? List.of() : snapshot.userIds();
    }

    /**
     * 执行 contains 对应的 CDP 业务操作。
     */
    @Override
    public boolean contains(Long snapshotId, String userId) {
        return users(snapshotId).contains(userId);
    }

    /**
     * 读取并校验必填的d Snapshot。
     */
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

    /**
     * 转换为View。
     */
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
