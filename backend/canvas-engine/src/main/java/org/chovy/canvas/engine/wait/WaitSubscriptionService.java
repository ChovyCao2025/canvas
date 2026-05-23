package org.chovy.canvas.engine.wait;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.chovy.canvas.domain.execution.CanvasWaitSubscription;
import org.chovy.canvas.domain.execution.CanvasWaitSubscriptionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class WaitSubscriptionService {

    public static final String WAIT_TYPE_EVENT = "UNTIL_EVENT";
    public static final String WAIT_TYPE_GOAL = "GOAL_CHECK";

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    private final CanvasWaitSubscriptionMapper mapper;
    private final Clock clock;

    @Autowired
    public WaitSubscriptionService(CanvasWaitSubscriptionMapper mapper) {
        this(mapper, Clock.systemDefaultZone());
    }

    WaitSubscriptionService(CanvasWaitSubscriptionMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    public CanvasWaitSubscription createEventWait(
            String executionId,
            Long canvasId,
            Long versionId,
            String userId,
            String nodeId,
            String eventCode,
            String eventFilters,
            String resumePayload,
            LocalDateTime expiresAt
    ) {
        Objects.requireNonNull(eventCode, "eventCode");
        CanvasWaitSubscription wait = newWait(
                executionId,
                canvasId,
                versionId,
                userId,
                nodeId,
                WAIT_TYPE_EVENT,
                eventCode,
                eventFilters,
                resumePayload,
                expiresAt
        );
        mapper.insert(wait);
        return wait;
    }

    public CanvasWaitSubscription createTimeWait(
            String executionId,
            Long canvasId,
            Long versionId,
            String userId,
            String nodeId,
            String waitType,
            String resumePayload,
            LocalDateTime resumeAt
    ) {
        Objects.requireNonNull(waitType, "waitType");
        CanvasWaitSubscription wait = newWait(
                executionId,
                canvasId,
                versionId,
                userId,
                nodeId,
                waitType,
                null,
                null,
                resumePayload,
                resumeAt
        );
        mapper.insert(wait);
        return wait;
    }

    public CanvasWaitSubscription createGoalWait(
            String executionId,
            Long canvasId,
            Long versionId,
            String userId,
            String nodeId,
            String eventCode,
            String resumePayload,
            LocalDateTime expiresAt
    ) {
        Objects.requireNonNull(eventCode, "eventCode");
        CanvasWaitSubscription wait = newWait(
                executionId,
                canvasId,
                versionId,
                userId,
                nodeId,
                WAIT_TYPE_GOAL,
                eventCode,
                null,
                resumePayload,
                expiresAt
        );
        mapper.insert(wait);
        return wait;
    }

    public List<CanvasWaitSubscription> findActiveEventWaits(String eventCode, String userId) {
        Objects.requireNonNull(eventCode, "eventCode");
        Objects.requireNonNull(userId, "userId");

        LocalDateTime now = now();
        return mapper.selectList(new LambdaQueryWrapper<CanvasWaitSubscription>()
                .in(CanvasWaitSubscription::getWaitType, WAIT_TYPE_EVENT, WAIT_TYPE_GOAL)
                .eq(CanvasWaitSubscription::getEventCode, eventCode)
                .eq(CanvasWaitSubscription::getUserId, userId)
                .eq(CanvasWaitSubscription::getStatus, STATUS_ACTIVE)
                .and(wrapper -> wrapper
                        .isNull(CanvasWaitSubscription::getExpiresAt)
                        .or()
                        .gt(CanvasWaitSubscription::getExpiresAt, now)));
    }

    public List<CanvasWaitSubscription> findExpiredActiveWaits(LocalDateTime now, int limit) {
        Objects.requireNonNull(now, "now");
        return mapper.selectList(new LambdaQueryWrapper<CanvasWaitSubscription>()
                .eq(CanvasWaitSubscription::getStatus, STATUS_ACTIVE)
                .isNotNull(CanvasWaitSubscription::getExpiresAt)
                .le(CanvasWaitSubscription::getExpiresAt, now)
                .orderByAsc(CanvasWaitSubscription::getExpiresAt)
                .last("LIMIT " + Math.max(1, limit)));
    }

    public int completeWait(Long id, String resumePayload) {
        return finishWait(id, STATUS_COMPLETED, resumePayload);
    }

    public int expireWait(Long id, String resumePayload) {
        return finishWait(id, STATUS_EXPIRED, resumePayload);
    }

    private int finishWait(Long id, String status, String resumePayload) {
        Objects.requireNonNull(id, "id");

        CanvasWaitSubscription update = new CanvasWaitSubscription();
        update.setStatus(status);
        update.setResumePayload(resumePayload);
        update.setUpdatedAt(now());

        return mapper.update(update, new LambdaUpdateWrapper<CanvasWaitSubscription>()
                .eq(CanvasWaitSubscription::getId, id)
                .eq(CanvasWaitSubscription::getStatus, STATUS_ACTIVE));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private CanvasWaitSubscription newWait(
            String executionId,
            Long canvasId,
            Long versionId,
            String userId,
            String nodeId,
            String waitType,
            String eventCode,
            String eventFilters,
            String resumePayload,
            LocalDateTime expiresAt
    ) {
        Objects.requireNonNull(executionId, "executionId");
        Objects.requireNonNull(canvasId, "canvasId");
        Objects.requireNonNull(versionId, "versionId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(nodeId, "nodeId");

        LocalDateTime now = now();
        CanvasWaitSubscription wait = new CanvasWaitSubscription();
        wait.setExecutionId(executionId);
        wait.setCanvasId(canvasId);
        wait.setVersionId(versionId);
        wait.setUserId(userId);
        wait.setNodeId(nodeId);
        wait.setWaitType(waitType);
        wait.setEventCode(eventCode);
        wait.setEventFilters(eventFilters);
        wait.setResumePayload(resumePayload);
        wait.setExpiresAt(expiresAt);
        wait.setStatus(STATUS_ACTIVE);
        wait.setCreatedAt(now);
        wait.setUpdatedAt(now);
        return wait;
    }
}
