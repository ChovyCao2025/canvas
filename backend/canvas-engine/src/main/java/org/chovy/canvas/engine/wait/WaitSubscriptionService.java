package org.chovy.canvas.engine.wait;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.chovy.canvas.dal.dataobject.CanvasWaitSubscriptionDO;
import org.chovy.canvas.dal.mapper.CanvasWaitSubscriptionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Wait Subscription 等待订阅服务。
 *
 * <p>负责 WAIT/GOAL_CHECK 类节点的挂起、恢复和订阅状态维护，使长周期画布可以跨事件继续执行。
 * <p>该服务需要保证恢复幂等，避免同一个等待订阅被并发重复触发。
 */
@Service
public class WaitSubscriptionService {

    /** 事件到达等待类型常量。 */
    public static final String WAIT_TYPE_EVENT = "UNTIL_EVENT";
    /** 目标达成等待类型常量。 */
    public static final String WAIT_TYPE_GOAL = "GOAL_CHECK";

    /** 等待订阅生效状态常量。 */
    public static final String STATUS_ACTIVE = "ACTIVE";
    /** 等待订阅已完成状态常量。 */
    public static final String STATUS_COMPLETED = "COMPLETED";
    /** 等待订阅已过期状态常量。 */
    public static final String STATUS_EXPIRED = "EXPIRED";

    /** 等待订阅 Mapper，用于创建等待记录和 CAS 更新恢复状态。 */
    private final CanvasWaitSubscriptionMapper mapper;
    /** 可注入时钟，便于测试时间相关策略。 */
    private final Clock clock;

    /** 默认使用系统时钟的构造器。 */
    @Autowired
    public WaitSubscriptionService(CanvasWaitSubscriptionMapper mapper) {
        this(mapper, Clock.systemDefaultZone());
    }

    /** 注入 Mapper 和时钟，测试可传入固定时钟。 */
    WaitSubscriptionService(CanvasWaitSubscriptionMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    /** 创建等待事件到达的订阅记录。 */
    public CanvasWaitSubscriptionDO createEventWait(
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
        CanvasWaitSubscriptionDO wait = newWait(
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

    /** 创建等待指定时间到达的订阅记录。 */
    public CanvasWaitSubscriptionDO createTimeWait(
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
        CanvasWaitSubscriptionDO wait = newWait(
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

    /** 创建目标达成等待订阅记录。 */
    public CanvasWaitSubscriptionDO createGoalWait(
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
        CanvasWaitSubscriptionDO wait = newWait(
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

    /**
     * 查找指定用户在该 eventCode 下所有 ACTIVE 的等待订阅（含 GOAL_CHECK）。
     *
     * <p>上限 100 条：正常场景下一个 (eventCode, userId) 组合最多几条，
     * 若因 bug 积累过多也不会拉垮 HTTP 线程。超出部分不处理（已有 CAS 保护，不会漏恢复正常数量）。
     */
    public List<CanvasWaitSubscriptionDO> findActiveEventWaits(String eventCode, String userId) {
        Objects.requireNonNull(eventCode, "eventCode");
        Objects.requireNonNull(userId, "userId");

        LocalDateTime now = now();
        return mapper.selectList(new LambdaQueryWrapper<CanvasWaitSubscriptionDO>()
                .in(CanvasWaitSubscriptionDO::getWaitType, WAIT_TYPE_EVENT, WAIT_TYPE_GOAL)
                .eq(CanvasWaitSubscriptionDO::getEventCode, eventCode)
                .eq(CanvasWaitSubscriptionDO::getUserId, userId)
                .eq(CanvasWaitSubscriptionDO::getStatus, STATUS_ACTIVE)
                .and(wrapper -> wrapper
                        .isNull(CanvasWaitSubscriptionDO::getExpiresAt)
                        .or()
                        .gt(CanvasWaitSubscriptionDO::getExpiresAt, now))
                .last("LIMIT 100"));
    }

    /** 查询已经到期但仍然活跃的等待订阅。 */
    public List<CanvasWaitSubscriptionDO> findExpiredActiveWaits(LocalDateTime now, int limit) {
        Objects.requireNonNull(now, "now");
        return mapper.selectList(new LambdaQueryWrapper<CanvasWaitSubscriptionDO>()
                .eq(CanvasWaitSubscriptionDO::getStatus, STATUS_ACTIVE)
                .isNotNull(CanvasWaitSubscriptionDO::getExpiresAt)
                .le(CanvasWaitSubscriptionDO::getExpiresAt, now)
                .orderByAsc(CanvasWaitSubscriptionDO::getExpiresAt)
                .last("LIMIT " + Math.max(1, limit)));
    }

    /** 将等待订阅 CAS 更新为已完成。 */
    public int completeWait(Long id, String resumePayload) {
        return finishWait(id, STATUS_COMPLETED, resumePayload);
    }

    /** 将等待订阅 CAS 更新为已过期。 */
    public int expireWait(Long id, String resumePayload) {
        return finishWait(id, STATUS_EXPIRED, resumePayload);
    }

    /** 通过 status=ACTIVE 条件完成 CAS 状态流转，避免并发重复恢复。 */
    private int finishWait(Long id, String status, String resumePayload) {
        Objects.requireNonNull(id, "id");

        CanvasWaitSubscriptionDO update = new CanvasWaitSubscriptionDO();
        update.setStatus(status);
        update.setResumePayload(resumePayload);
        update.setUpdatedAt(now());

        return mapper.update(update, new LambdaUpdateWrapper<CanvasWaitSubscriptionDO>()
                .eq(CanvasWaitSubscriptionDO::getId, id)
                .eq(CanvasWaitSubscriptionDO::getStatus, STATUS_ACTIVE));
    }

    /** 使用注入时钟获取当前时间。 */
    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /** 构造 ACTIVE 状态的等待订阅实体，供事件、时间和目标等待创建流程复用。 */
    private CanvasWaitSubscriptionDO newWait(
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
        CanvasWaitSubscriptionDO wait = new CanvasWaitSubscriptionDO();
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
