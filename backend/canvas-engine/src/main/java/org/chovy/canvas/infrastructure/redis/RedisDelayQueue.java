package org.chovy.canvas.infrastructure.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.context.NodeStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Redis sorted-set delay queue used for crash-safe special-node timeout timers.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisDelayQueue {

    private static final int DEFAULT_CLAIM_LIMIT = 100;
    private static final long DEFAULT_REQUEUE_DELAY_MS = 1_000L;

    private final StringRedisTemplate redis;
    private final RedisKeyUtil keys;
    private final ObjectMapper objectMapper;

    /**
     * SpecialNodeTimeout 处理 infrastructure.redis 场景的业务逻辑。
     * @param timeoutId 业务对象 ID，用于定位具体记录。
     * @param executionId 业务对象 ID，用于定位具体记录。
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param versionId 业务对象 ID，用于定位具体记录。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param nodeId 业务对象 ID，用于定位具体记录。
     * @param nodeType 类型标识，用于选择对应处理分支。
     * @param triggerType 类型标识，用于选择对应处理分支。
     * @param timerKey 业务键，用于在同一租户下定位资源。
     * @param scheduledAtEpochMs 时间参数，用于计算窗口、过期或审计时间。
     * @param fireAtEpochMs fire at epoch ms 参数，用于 SpecialNodeTimeout 流程中的校验、计算或对象转换。
     * @param timeoutSec 时间参数，用于计算窗口、过期或审计时间。
     * @param expectedStatus 业务状态，用于筛选或推进状态流转。
     * @return 返回 SpecialNodeTimeout 流程生成的业务结果。
     */
    @Value("${canvas.special-timeout.claim-lease-ms:60000}")
    private long claimLeaseMs = 60_000L;

    /**
     * SpecialNodeTimeout 数据记录。
     */
    public record SpecialNodeTimeout(
            String timeoutId,
            String executionId,
            Long canvasId,
            Long versionId,
            String userId,
            String nodeId,
            String nodeType,
            String triggerType,
            String timerKey,
            long scheduledAtEpochMs,
            long fireAtEpochMs,
            long timeoutSec,
            String expectedStatus) {
    }

    /**
     * scheduleSpecialNodeTimeout 处理 infrastructure.redis 场景的业务逻辑。
     * @param ctx ctx 参数，用于 scheduleSpecialNodeTimeout 流程中的校验、计算或对象转换。
     * @param nodeId 业务对象 ID，用于定位具体记录。
     * @param nodeType 类型标识，用于选择对应处理分支。
     * @param timeoutSec 时间参数，用于计算窗口、过期或审计时间。
     */
    public void scheduleSpecialNodeTimeout(ExecutionContext ctx, String nodeId, String nodeType, long timeoutSec) {
        scheduleSpecialNodeTimeout(ctx, nodeId, nodeType, timerKey(nodeType, nodeId), timeoutSec);
    }

    /**
     * scheduleSpecialNodeTimeout 处理 infrastructure.redis 场景的业务逻辑。
     * @param ctx ctx 参数，用于 scheduleSpecialNodeTimeout 流程中的校验、计算或对象转换。
     * @param nodeId 业务对象 ID，用于定位具体记录。
     * @param nodeType 类型标识，用于选择对应处理分支。
     * @param timerKey 业务键，用于在同一租户下定位资源。
     * @param timeoutSec 时间参数，用于计算窗口、过期或审计时间。
     */
    public void scheduleSpecialNodeTimeout(ExecutionContext ctx, String nodeId, String nodeType,
                                           String timerKey, long timeoutSec) {
        try {
            SpecialNodeTimeout item = buildSpecialNodeTimeout(ctx, nodeId, nodeType, timerKey, timeoutSec);
            String member = objectMapper.writeValueAsString(item);
            // The serialized item is the ZSET member, making schedule/cancel deterministic for the same timer identity.
            redis.opsForZSet().add(keys.delayQueue(), member, item.fireAtEpochMs());
            redis.opsForZSet().remove(keys.delayQueueInflight(), member);
            log.debug("[DELAY-Q] scheduled special timeout id={} fireAt={}",
                    item.timeoutId(), item.fireAtEpochMs());
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            throw new IllegalStateException("Failed to schedule special node timeout", e);
        }
    }

    /**
     * cancelSpecialNodeTimeout 删除或清理 infrastructure.redis 场景的业务数据。
     * @param ctx ctx 参数，用于 cancelSpecialNodeTimeout 流程中的校验、计算或对象转换。
     * @param nodeId 业务对象 ID，用于定位具体记录。
     * @param nodeType 类型标识，用于选择对应处理分支。
     * @param timeoutSec 时间参数，用于计算窗口、过期或审计时间。
     */
    public void cancelSpecialNodeTimeout(ExecutionContext ctx, String nodeId, String nodeType, long timeoutSec) {
        cancelSpecialNodeTimeout(ctx, nodeId, nodeType, timerKey(nodeType, nodeId), timeoutSec);
    }

    /**
     * cancelSpecialNodeTimeout 删除或清理 infrastructure.redis 场景的业务数据。
     * @param ctx ctx 参数，用于 cancelSpecialNodeTimeout 流程中的校验、计算或对象转换。
     * @param nodeId 业务对象 ID，用于定位具体记录。
     * @param nodeType 类型标识，用于选择对应处理分支。
     * @param timerKey 业务键，用于在同一租户下定位资源。
     * @param timeoutSec 时间参数，用于计算窗口、过期或审计时间。
     */
    public void cancelSpecialNodeTimeout(ExecutionContext ctx, String nodeId, String nodeType,
                                         String timerKey, long timeoutSec) {
        try {
            SpecialNodeTimeout item = buildSpecialNodeTimeout(ctx, nodeId, nodeType, timerKey, timeoutSec);
            String member = objectMapper.writeValueAsString(item);
            redis.opsForZSet().remove(keys.delayQueue(), member);
            redis.opsForZSet().remove(keys.delayQueueInflight(), member);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            log.warn("[DELAY-Q] cancel special timeout failed executionId={} nodeId={}: {}",
                    ctx == null ? null : ctx.getExecutionId(), nodeId, e.getMessage());
        }
    }

    /**
     * pollDueSpecialNodeTimeouts 处理 infrastructure.redis 场景的业务逻辑。
     * @return 返回 poll due special node timeouts 汇总后的集合、分页或映射视图。
     */
    public List<SpecialNodeTimeout> pollDueSpecialNodeTimeouts() {
        return pollDueSpecialNodeTimeouts(DEFAULT_CLAIM_LIMIT);
    }

    /**
     * pollDueSpecialNodeTimeouts 处理 infrastructure.redis 场景的业务逻辑。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 poll due special node timeouts 汇总后的集合、分页或映射视图。
     */
    public List<SpecialNodeTimeout> pollDueSpecialNodeTimeouts(int limit) {
        List<String> members = claimDueMembers(limit);
        if (members.isEmpty()) {
            return List.of();
        }
        List<SpecialNodeTimeout> due = new ArrayList<>(members.size());
        for (String member : members) {
            try {
                due.add(objectMapper.readValue(member, SpecialNodeTimeout.class));
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (Exception e) {
                log.warn("[DELAY-Q] invalid delay item ignored: {}", e.getMessage());
                // Malformed items cannot be retried safely, so acknowledge the claim to prevent a poison loop.
                ackClaimedMember(member);
            }
        }
        return due;
    }

    /**
     * ackSpecialNodeTimeout 处理 infrastructure.redis 场景的业务逻辑。
     * @param item item 参数，用于 ackSpecialNodeTimeout 流程中的校验、计算或对象转换。
     */
    public void ackSpecialNodeTimeout(SpecialNodeTimeout item) {
        try {
            ackClaimedMember(objectMapper.writeValueAsString(item));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            log.warn("[DELAY-Q] ack special timeout failed id={}: {}", item == null ? null : item.timeoutId(),
                    e.getMessage());
        }
    }

    /**
     * requeueSpecialNodeTimeout 处理 infrastructure.redis 场景的业务逻辑。
     * @param item item 参数，用于 requeueSpecialNodeTimeout 流程中的校验、计算或对象转换。
     */
    public void requeueSpecialNodeTimeout(SpecialNodeTimeout item) {
        requeueSpecialNodeTimeout(item, Duration.ofMillis(DEFAULT_REQUEUE_DELAY_MS));
    }

    /**
     * requeueSpecialNodeTimeout 处理 infrastructure.redis 场景的业务逻辑。
     * @param item item 参数，用于 requeueSpecialNodeTimeout 流程中的校验、计算或对象转换。
     * @param delay delay 参数，用于 requeueSpecialNodeTimeout 流程中的校验、计算或对象转换。
     */
    public void requeueSpecialNodeTimeout(SpecialNodeTimeout item, Duration delay) {
        try {
            String member = objectMapper.writeValueAsString(item);
            long fireAt = System.currentTimeMillis() + Math.max(0L, delay == null ? 0L : delay.toMillis());
            // Requeue moves the item out of inflight and back to the ready queue with a new fire time atomically.
            redis.execute(REQUEUE_SCRIPT, List.of(keys.delayQueue(), keys.delayQueueInflight()),
                    member, String.valueOf(fireAt));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            log.warn("[DELAY-Q] requeue special timeout failed id={}: {}",
                    item == null ? null : item.timeoutId(), e.getMessage());
        }
    }

    /**
     * delayUntil 处理 infrastructure.redis 场景的业务逻辑。
     * @param item item 参数，用于 delayUntil 流程中的校验、计算或对象转换。
     * @return 返回 delayUntil 流程生成的业务结果。
     */
    public Duration delayUntil(SpecialNodeTimeout item) {
        long millis = Math.max(0L, item.fireAtEpochMs() - System.currentTimeMillis());
        return Duration.ofMillis(millis);
    }

    /**
     * timerKey 处理 infrastructure.redis 场景的业务逻辑。
     * @param nodeType 类型标识，用于选择对应处理分支。
     * @param nodeId 业务对象 ID，用于定位具体记录。
     * @return 返回 timer key 生成的文本或业务键。
     */
    public static String timerKey(String nodeType, String nodeId) {
        return switch (nodeType) {
            case NodeType.AGGREGATE -> "ag:" + nodeId;
            case NodeType.THRESHOLD -> "th:" + nodeId;
            default -> nodeId;
        };
    }

    /**
     * triggerTypeForNodeType 创建或触发 infrastructure.redis 场景的业务处理。
     * @param nodeType 类型标识，用于选择对应处理分支。
     * @return 返回 trigger type for node type 生成的文本或业务键。
     */
    public static String triggerTypeForNodeType(String nodeType) {
        return switch (nodeType) {
            case NodeType.HUB -> TriggerType.HUB_TIMEOUT;
            case NodeType.AGGREGATE -> TriggerType.AGGREGATE_TIMEOUT;
            case NodeType.THRESHOLD -> TriggerType.THRESHOLD_TIMEOUT;
            default -> throw new IllegalArgumentException("Unsupported special node type: " + nodeType);
        };
    }

    /**
     * isSpecialTimeoutTrigger 校验或转换 infrastructure.redis 场景的数据。
     * @param triggerType 类型标识，用于选择对应处理分支。
     * @return 返回布尔判断结果。
     */
    public static boolean isSpecialTimeoutTrigger(String triggerType) {
        return TriggerType.HUB_TIMEOUT.equals(triggerType)
                || TriggerType.AGGREGATE_TIMEOUT.equals(triggerType)
                || TriggerType.THRESHOLD_TIMEOUT.equals(triggerType);
    }

    /**
     * 构建业务对象或响应数据。
     *
     * @param ctx ctx 参数，用于 buildSpecialNodeTimeout 流程中的校验、计算或对象转换。
     * @param nodeId 业务对象 ID，用于定位具体记录。
     * @param nodeType 类型标识，用于选择对应处理分支。
     * @param timerKey 业务键，用于在同一租户下定位资源。
     * @param timeoutSec 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回组装或转换后的结果对象。
     */
    private SpecialNodeTimeout buildSpecialNodeTimeout(ExecutionContext ctx, String nodeId, String nodeType,
                                                       String timerKey, long timeoutSec) {
        long scheduledAt = ctx.getHubStartTimes().getOrDefault(timerKey, System.currentTimeMillis());
        long fireAt = scheduledAt + Math.max(0L, timeoutSec) * 1000L;
        // timeoutId includes scheduledAt so refreshed timers for the same node do not collide with older claims.
        String timeoutId = ctx.getExecutionId() + ":" + timerKey + ":" + scheduledAt;
        return new SpecialNodeTimeout(
                timeoutId,
                ctx.getExecutionId(),
                ctx.getCanvasId(),
                ctx.getVersionId(),
                ctx.getUserId(),
                nodeId,
                nodeType,
                triggerTypeForNodeType(nodeType),
                timerKey,
                scheduledAt,
                fireAt,
                timeoutSec,
                NodeStatus.WAITING.name());
    }

    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 claim due members 汇总后的集合、分页或映射视图。
     */
    @SuppressWarnings("unchecked")
    private List<String> claimDueMembers(int limit) {
        Long now = System.currentTimeMillis();
        Long batchSize = Long.valueOf(Math.max(1, limit));
        Long leaseUntil = now + Math.max(1_000L, claimLeaseMs);
        // The Lua script first recovers expired inflight leases, then leases due ready items to this poller.
        List<String> claimed = redis.execute(CLAIM_DUE_SCRIPT,
                List.of(keys.delayQueue(), keys.delayQueueInflight()),
                now.toString(), batchSize.toString(), leaseUntil.toString());
        return claimed == null ? Collections.emptyList() : claimed;
    }

    /**
     * 执行 ackClaimedMember 流程，围绕 ack claimed member 完成校验、计算或结果组装。
     *
     * @param member member 参数，用于 ackClaimedMember 流程中的校验、计算或对象转换。
     */
    private void ackClaimedMember(String member) {
        try {
            redis.opsForZSet().remove(keys.delayQueueInflight(), member);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            log.warn("[DELAY-Q] ack claimed member failed: {}", e.getMessage());
        }
    }

    private static final RedisScript<List> CLAIM_DUE_SCRIPT = RedisScript.of(
            "local expired = redis.call('ZRANGEBYSCORE', KEYS[2], '-inf', ARGV[1], 'LIMIT', 0, ARGV[2]); "
                    + "if #expired > 0 then "
                    + "  redis.call('ZREM', KEYS[2], unpack(expired)); "
                    /**
                     * 执行 ipairs 流程，围绕 ipairs 完成校验、计算或结果组装。
                     *
                     * @param _  参数，用于 ipairs 流程中的校验、计算或对象转换。
                     * @param class class 参数，用于 ipairs 流程中的校验、计算或对象转换。
                     * @return 返回 ipairs 流程生成的业务结果。
                     */
                    + "  for _, item in ipairs(expired) do redis.call('ZADD', KEYS[1], ARGV[1], item); end; "
                    + "end; "
                    + "local items = redis.call('ZRANGEBYSCORE', KEYS[1], '-inf', ARGV[1], 'LIMIT', 0, ARGV[2]); "
                    + "if #items > 0 then "
                    + "  redis.call('ZREM', KEYS[1], unpack(items)); "
                    /**
                     * 执行 ipairs 流程，围绕 ipairs 完成校验、计算或结果组装。
                     *
                     * @param class class 参数，用于 ipairs 流程中的校验、计算或对象转换。
                     * @return 返回 ipairs 流程生成的业务结果。
                     */
                    + "  for _, item in ipairs(items) do redis.call('ZADD', KEYS[2], ARGV[3], item); end; "
                    + "end; "
                    + "return items;",
            List.class
    );

    private static final RedisScript<Long> REQUEUE_SCRIPT = RedisScript.of(
            "redis.call('ZREM', KEYS[2], ARGV[1]); "
                    + "return redis.call('ZADD', KEYS[1], ARGV[2], ARGV[1]);",
            Long.class
    );
}
