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

    @Value("${canvas.special-timeout.claim-lease-ms:60000}")
    private long claimLeaseMs = 60_000L;

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

    public void scheduleSpecialNodeTimeout(ExecutionContext ctx, String nodeId, String nodeType, long timeoutSec) {
        scheduleSpecialNodeTimeout(ctx, nodeId, nodeType, timerKey(nodeType, nodeId), timeoutSec);
    }

    public void scheduleSpecialNodeTimeout(ExecutionContext ctx, String nodeId, String nodeType,
                                           String timerKey, long timeoutSec) {
        try {
            SpecialNodeTimeout item = buildSpecialNodeTimeout(ctx, nodeId, nodeType, timerKey, timeoutSec);
            String member = objectMapper.writeValueAsString(item);
            redis.opsForZSet().add(keys.delayQueue(), member, item.fireAtEpochMs());
            redis.opsForZSet().remove(keys.delayQueueInflight(), member);
            log.debug("[DELAY-Q] scheduled special timeout id={} fireAt={}",
                    item.timeoutId(), item.fireAtEpochMs());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to schedule special node timeout", e);
        }
    }

    public void cancelSpecialNodeTimeout(ExecutionContext ctx, String nodeId, String nodeType, long timeoutSec) {
        cancelSpecialNodeTimeout(ctx, nodeId, nodeType, timerKey(nodeType, nodeId), timeoutSec);
    }

    public void cancelSpecialNodeTimeout(ExecutionContext ctx, String nodeId, String nodeType,
                                         String timerKey, long timeoutSec) {
        try {
            SpecialNodeTimeout item = buildSpecialNodeTimeout(ctx, nodeId, nodeType, timerKey, timeoutSec);
            String member = objectMapper.writeValueAsString(item);
            redis.opsForZSet().remove(keys.delayQueue(), member);
            redis.opsForZSet().remove(keys.delayQueueInflight(), member);
        } catch (Exception e) {
            log.warn("[DELAY-Q] cancel special timeout failed executionId={} nodeId={}: {}",
                    ctx == null ? null : ctx.getExecutionId(), nodeId, e.getMessage());
        }
    }

    public List<SpecialNodeTimeout> pollDueSpecialNodeTimeouts() {
        return pollDueSpecialNodeTimeouts(DEFAULT_CLAIM_LIMIT);
    }

    public List<SpecialNodeTimeout> pollDueSpecialNodeTimeouts(int limit) {
        List<String> members = claimDueMembers(limit);
        if (members.isEmpty()) {
            return List.of();
        }
        List<SpecialNodeTimeout> due = new ArrayList<>(members.size());
        for (String member : members) {
            try {
                due.add(objectMapper.readValue(member, SpecialNodeTimeout.class));
            } catch (Exception e) {
                log.warn("[DELAY-Q] invalid delay item ignored: {}", e.getMessage());
                ackClaimedMember(member);
            }
        }
        return due;
    }

    public void ackSpecialNodeTimeout(SpecialNodeTimeout item) {
        try {
            ackClaimedMember(objectMapper.writeValueAsString(item));
        } catch (Exception e) {
            log.warn("[DELAY-Q] ack special timeout failed id={}: {}", item == null ? null : item.timeoutId(),
                    e.getMessage());
        }
    }

    public void requeueSpecialNodeTimeout(SpecialNodeTimeout item) {
        requeueSpecialNodeTimeout(item, Duration.ofMillis(DEFAULT_REQUEUE_DELAY_MS));
    }

    public void requeueSpecialNodeTimeout(SpecialNodeTimeout item, Duration delay) {
        try {
            String member = objectMapper.writeValueAsString(item);
            long fireAt = System.currentTimeMillis() + Math.max(0L, delay == null ? 0L : delay.toMillis());
            redis.execute(REQUEUE_SCRIPT, List.of(keys.delayQueue(), keys.delayQueueInflight()),
                    member, String.valueOf(fireAt));
        } catch (Exception e) {
            log.warn("[DELAY-Q] requeue special timeout failed id={}: {}",
                    item == null ? null : item.timeoutId(), e.getMessage());
        }
    }

    public Duration delayUntil(SpecialNodeTimeout item) {
        long millis = Math.max(0L, item.fireAtEpochMs() - System.currentTimeMillis());
        return Duration.ofMillis(millis);
    }

    public static String timerKey(String nodeType, String nodeId) {
        return switch (nodeType) {
            case NodeType.LOGIC_RELATION -> "lr:" + nodeId;
            case NodeType.AGGREGATE -> "ag:" + nodeId;
            case NodeType.THRESHOLD -> "th:" + nodeId;
            default -> nodeId;
        };
    }

    public static String triggerTypeForNodeType(String nodeType) {
        return switch (nodeType) {
            case NodeType.HUB -> TriggerType.HUB_TIMEOUT;
            case NodeType.LOGIC_RELATION -> TriggerType.LOGIC_RELATION_TIMEOUT;
            case NodeType.AGGREGATE -> TriggerType.AGGREGATE_TIMEOUT;
            case NodeType.THRESHOLD -> TriggerType.THRESHOLD_TIMEOUT;
            default -> throw new IllegalArgumentException("Unsupported special node type: " + nodeType);
        };
    }

    public static boolean isSpecialTimeoutTrigger(String triggerType) {
        return TriggerType.HUB_TIMEOUT.equals(triggerType)
                || TriggerType.LOGIC_RELATION_TIMEOUT.equals(triggerType)
                || TriggerType.AGGREGATE_TIMEOUT.equals(triggerType)
                || TriggerType.THRESHOLD_TIMEOUT.equals(triggerType);
    }

    private SpecialNodeTimeout buildSpecialNodeTimeout(ExecutionContext ctx, String nodeId, String nodeType,
                                                       String timerKey, long timeoutSec) {
        long scheduledAt = ctx.getHubStartTimes().getOrDefault(timerKey, System.currentTimeMillis());
        long fireAt = scheduledAt + Math.max(0L, timeoutSec) * 1000L;
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

    @SuppressWarnings("unchecked")
    private List<String> claimDueMembers(int limit) {
        Long now = System.currentTimeMillis();
        Long batchSize = Long.valueOf(Math.max(1, limit));
        Long leaseUntil = now + Math.max(1_000L, claimLeaseMs);
        List<String> claimed = redis.execute(CLAIM_DUE_SCRIPT,
                List.of(keys.delayQueue(), keys.delayQueueInflight()),
                now.toString(), batchSize.toString(), leaseUntil.toString());
        return claimed == null ? Collections.emptyList() : claimed;
    }

    private void ackClaimedMember(String member) {
        try {
            redis.opsForZSet().remove(keys.delayQueueInflight(), member);
        } catch (Exception e) {
            log.warn("[DELAY-Q] ack claimed member failed: {}", e.getMessage());
        }
    }

    private static final RedisScript<List> CLAIM_DUE_SCRIPT = RedisScript.of(
            "local expired = redis.call('ZRANGEBYSCORE', KEYS[2], '-inf', ARGV[1], 'LIMIT', 0, ARGV[2]); "
                    + "if #expired > 0 then "
                    + "  redis.call('ZREM', KEYS[2], unpack(expired)); "
                    + "  for _, item in ipairs(expired) do redis.call('ZADD', KEYS[1], ARGV[1], item); end; "
                    + "end; "
                    + "local items = redis.call('ZRANGEBYSCORE', KEYS[1], '-inf', ARGV[1], 'LIMIT', 0, ARGV[2]); "
                    + "if #items > 0 then "
                    + "  redis.call('ZREM', KEYS[1], unpack(items)); "
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
