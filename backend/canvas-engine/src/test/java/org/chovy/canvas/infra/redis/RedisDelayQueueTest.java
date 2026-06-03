package org.chovy.canvas.infra.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.context.NodeStatus;
import org.chovy.canvas.infrastructure.redis.RedisDelayQueue;
import org.chovy.canvas.infrastructure.redis.RedisKeyUtil;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.doubleThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisDelayQueueTest {

    @Test
    void scheduleSpecialNodeTimeoutAddsRecoverableItemWithFutureTimestamp() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ZSetOperations<String, String> zsets = mock(ZSetOperations.class);
        when(redis.opsForZSet()).thenReturn(zsets);
        ObjectMapper objectMapper = new ObjectMapper();
        RedisDelayQueue queue = new RedisDelayQueue(redis, new RedisKeyUtil(), objectMapper);
        ExecutionContext ctx = context();

        long before = System.currentTimeMillis();
        queue.scheduleSpecialNodeTimeout(ctx, "hub", NodeType.HUB, 60);

        ArgumentCaptor<String> member = ArgumentCaptor.forClass(String.class);
        verify(zsets).add(eq("canvas:delay-queue"),
                member.capture(),
                doubleThat(score -> score >= before + 60_000 - 100
                        && score <= before + 60_000 + 500));
        RedisDelayQueue.SpecialNodeTimeout item = objectMapper.readValue(
                member.getValue(), RedisDelayQueue.SpecialNodeTimeout.class);
        assertThat(item.executionId()).isEqualTo("exec-delay-test");
        assertThat(item.canvasId()).isEqualTo(10L);
        assertThat(item.versionId()).isEqualTo(1L);
        assertThat(item.userId()).isEqualTo("user-1");
        assertThat(item.nodeId()).isEqualTo("hub");
        assertThat(item.nodeType()).isEqualTo(NodeType.HUB);
        assertThat(item.triggerType()).isEqualTo(TriggerType.HUB_TIMEOUT);
        assertThat(item.timerKey()).isEqualTo("hub");
        assertThat(item.expectedStatus()).isEqualTo(NodeStatus.WAITING.name());
    }

    @Test
    void pollDueSpecialNodeTimeoutsReturnsAndRemovesDueItems() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper();
        RedisDelayQueue.SpecialNodeTimeout item = item();
        String member = objectMapper.writeValueAsString(item);
        when(redis.execute(any(RedisScript.class), eq(List.of("canvas:delay-queue", "canvas:delay-queue:inflight")),
                anyString(), eq("100"), anyString())).thenReturn(List.of(member));
        RedisDelayQueue queue = new RedisDelayQueue(redis, new RedisKeyUtil(), objectMapper);

        List<RedisDelayQueue.SpecialNodeTimeout> due = queue.pollDueSpecialNodeTimeouts();

        assertThat(due).containsExactly(item);
        verify(redis).execute(any(RedisScript.class), eq(List.of("canvas:delay-queue", "canvas:delay-queue:inflight")),
                anyString(), eq("100"), anyString());
    }

    @Test
    void cancelSpecialNodeTimeoutRemovesStableMember() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ZSetOperations<String, String> zsets = mock(ZSetOperations.class);
        when(redis.opsForZSet()).thenReturn(zsets);
        ObjectMapper objectMapper = new ObjectMapper();
        RedisDelayQueue queue = new RedisDelayQueue(redis, new RedisKeyUtil(), objectMapper);
        ExecutionContext ctx = context();
        ctx.getHubStartTimes().put("hub", 1_000L);

        queue.cancelSpecialNodeTimeout(ctx, "hub", NodeType.HUB, 60);

        RedisDelayQueue.SpecialNodeTimeout item = item();
        verify(zsets).remove("canvas:delay-queue", objectMapper.writeValueAsString(item));
        verify(zsets).remove("canvas:delay-queue:inflight", objectMapper.writeValueAsString(item));
    }

    @Test
    void delayQueueKeyUsesRedisKeyUtilPrefix() {
        assertThat(new RedisKeyUtil().delayQueue()).isEqualTo("canvas:delay-queue");
        assertThat(new RedisKeyUtil().delayQueueInflight()).isEqualTo("canvas:delay-queue:inflight");
    }

    @Test
    void ackSpecialNodeTimeoutRemovesInflightMember() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ZSetOperations<String, String> zsets = mock(ZSetOperations.class);
        when(redis.opsForZSet()).thenReturn(zsets);
        ObjectMapper objectMapper = new ObjectMapper();
        RedisDelayQueue queue = new RedisDelayQueue(redis, new RedisKeyUtil(), objectMapper);
        RedisDelayQueue.SpecialNodeTimeout item = item();

        queue.ackSpecialNodeTimeout(item);

        verify(zsets).remove("canvas:delay-queue:inflight", objectMapper.writeValueAsString(item));
    }

    @Test
    void requeueSpecialNodeTimeoutMovesInflightBackToReadyQueue() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper();
        RedisDelayQueue queue = new RedisDelayQueue(redis, new RedisKeyUtil(), objectMapper);
        RedisDelayQueue.SpecialNodeTimeout item = item();

        queue.requeueSpecialNodeTimeout(item, Duration.ZERO);

        verify(redis).execute(any(RedisScript.class),
                eq(List.of("canvas:delay-queue", "canvas:delay-queue:inflight")),
                anyString(), anyString());
    }

    private ExecutionContext context() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-delay-test");
        ctx.setCanvasId(10L);
        ctx.setVersionId(1L);
        ctx.setUserId("user-1");
        return ctx;
    }

    private RedisDelayQueue.SpecialNodeTimeout item() {
        return new RedisDelayQueue.SpecialNodeTimeout(
                "exec-delay-test:hub:1000",
                "exec-delay-test",
                10L,
                1L,
                "user-1",
                "hub",
                NodeType.HUB,
                TriggerType.HUB_TIMEOUT,
                "hub",
                1_000L,
                61_000L,
                60L,
                NodeStatus.WAITING.name());
    }
}
