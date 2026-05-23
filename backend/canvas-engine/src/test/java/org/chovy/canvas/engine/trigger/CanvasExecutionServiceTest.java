package org.chovy.canvas.engine.trigger;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.chovy.canvas.domain.canvas.Canvas;
import org.chovy.canvas.domain.canvas.CanvasMapper;
import org.chovy.canvas.domain.canvas.CanvasVersionMapper;
import org.chovy.canvas.domain.constant.CanvasStatusEnum;
import org.chovy.canvas.domain.constant.NodeType;
import org.chovy.canvas.domain.constant.TriggerType;
import org.chovy.canvas.domain.execution.CanvasExecutionDlq;
import org.chovy.canvas.domain.execution.CanvasExecutionDlqMapper;
import org.chovy.canvas.domain.execution.CanvasExecutionMapper;
import org.chovy.canvas.domain.execution.CanvasExecutionStatsMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.scheduler.DagEngine;
import org.chovy.canvas.infra.mq.OverflowRetryMessage;
import org.chovy.canvas.infra.cache.CanvasConfigCache;
import org.chovy.canvas.infra.redis.ContextPersistenceService;
import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CanvasExecutionService}.
 */
@ExtendWith(MockitoExtension.class)
class CanvasExecutionServiceTest {

    @Mock CanvasMapper canvasMapper;
    @Mock CanvasVersionMapper canvasVersionMapper;
    @Mock CanvasExecutionMapper executionMapper;
    @Mock CanvasConfigCache configCache;
    @Mock DagParser dagParser;
    @Mock ContextPersistenceService ctxStore;
    @Mock DagEngine dagEngine;
    @Mock TriggerPreCheckService preCheckService;
    @Mock InFlightExecutionRegistry executionRegistry;
    @Mock CanvasExecutionStatsMapper statsMapper;
    @Mock CanvasExecutionDlqMapper dlqMapper;
    @Mock RocketMQTemplate rocketMQTemplate;
    @Mock DefaultMQProducer rocketProducer;

    TriggerPriorityConfig priorityConfig;
    ObjectMapper objectMapper;

    /** System under test — constructed manually so we can inject mocks via ReflectionTestUtils. */
    CanvasExecutionService sut;

    @BeforeEach
    void setUp() {
        priorityConfig = new TriggerPriorityConfig();
        objectMapper = new ObjectMapper();
        sut = new CanvasExecutionService(
                canvasMapper,
                canvasVersionMapper,
                executionMapper,
                configCache,
                dagParser,
                ctxStore,
                dagEngine,
                preCheckService,
                executionRegistry,
                statsMapper,
                dlqMapper,
                priorityConfig,
                rocketMQTemplate,
                objectMapper
        );
        // Inject @Value fields with sensible defaults
        ReflectionTestUtils.setField(sut, "ctxTtlSec", 86400L);
        ReflectionTestUtils.setField(sut, "globalTimeoutSec", 600L);
        ReflectionTestUtils.setField(sut, "globalMaxConcurrency", 1000);
    }

    // ─── canvasCache caching behaviour ───────────────────────────────────────

    /**
     * Calling trigger() twice with the same canvasId must hit canvasMapper.selectById()
     * only once — the second call should be served from the Caffeine cache.
     *
     * <p>To keep the test focused solely on the cache, we call the cache directly
     * via ReflectionTestUtils rather than driving the full trigger() pipeline
     * (which would require stubbing 8+ collaborators end-to-end).
     */
    @Test
    @SuppressWarnings("unchecked")
    void canvasCache_deduplicatesDbCallsForSameId() {
        final long canvasId = 10L;

        Canvas published = new Canvas();
        published.setId(canvasId);
        published.setStatus(CanvasStatusEnum.PUBLISHED.getCode());

        when(canvasMapper.selectById(canvasId)).thenReturn(published);

        // Obtain the internal Caffeine cache through reflection
        Cache<Long, Canvas> cache =
                (Cache<Long, Canvas>) ReflectionTestUtils.getField(sut, "canvasCache");

        // First access → loads from DB
        Canvas first = cache.get(canvasId, id -> canvasMapper.selectById(id));
        // Second access → should come from cache, NOT from DB
        Canvas second = cache.get(canvasId, id -> canvasMapper.selectById(id));

        // DB called exactly once despite two cache.get() calls
        verify(canvasMapper, times(1)).selectById(eq(canvasId));
        assert first == second : "cache should return the same object instance";
    }

    /**
     * invalidateCanvas() must remove the entry so the next cache.get() re-queries the DB.
     */
    @Test
    @SuppressWarnings("unchecked")
    void invalidateCanvas_causesNextAccessToReloadFromDb() {
        final long canvasId = 20L;

        Canvas v1 = new Canvas();
        v1.setId(canvasId);
        v1.setStatus(CanvasStatusEnum.PUBLISHED.getCode());

        Canvas v2 = new Canvas();
        v2.setId(canvasId);
        v2.setStatus(0); // simulates post-offline status

        when(canvasMapper.selectById(canvasId))
                .thenReturn(v1)   // first load
                .thenReturn(v2);  // reload after invalidation

        Cache<Long, Canvas> cache =
                (Cache<Long, Canvas>) ReflectionTestUtils.getField(sut, "canvasCache");

        // Warm the cache
        cache.get(canvasId, id -> canvasMapper.selectById(id));

        // Simulate publish/offline event
        sut.invalidateCanvas(canvasId);

        // Next access must go back to DB
        Canvas afterInvalidate = cache.get(canvasId, id -> canvasMapper.selectById(id));

        verify(canvasMapper, times(2)).selectById(eq(canvasId));
        assert afterInvalidate == v2 : "should return the freshly loaded canvas after invalidation";
    }

    @Test
    void normalPriorityOverflowQueuesRocketMqRetryWithIncrementedChainRetryCount() throws Exception {
        Canvas canvas = publishedCanvas(30L, 10);
        when(canvasMapper.selectById(30L)).thenReturn(canvas);
        when(ctxStore.acquireDedup(eq(30L), eq("user-1"), eq("msg-1"), any())).thenReturn(true);
        when(executionRegistry.activeCount(30L)).thenReturn(10);
        when(rocketMQTemplate.getProducer()).thenReturn(rocketProducer);
        when(rocketProducer.send(any(Message.class))).thenReturn(sendResult(SendStatus.SEND_OK));

        long enqueueStart = System.currentTimeMillis();
        Map<String, Object> result = sut.trigger(
                30L,
                "user-1",
                TriggerType.MQ,
                NodeType.MQ_TRIGGER,
                "topic-a",
                Map.of("orderId", "o-1", OverflowRetryMessage.CHAIN_RETRY_PAYLOAD_KEY, 2),
                "msg-1",
                false
        ).block();

        assertThat(result).containsEntry("overflow", "queued_for_retry");
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rocketProducer).send(messageCaptor.capture());
        Message message = messageCaptor.getValue();
        assertThat(message.getTopic()).isEqualTo("CANVAS_TRIGGER_OVERFLOW");
        assertThat(message.getTags()).isEqualTo(TriggerType.MQ);
        assertThat(message.getDeliverTimeMs())
                .isGreaterThanOrEqualTo(enqueueStart + priorityConfig.getOverflowRetryDelayMs())
                .isLessThanOrEqualTo(System.currentTimeMillis() + priorityConfig.getOverflowRetryDelayMs() + 1_000);
        OverflowRetryMessage retryMessage = objectMapper.readValue(message.getBody(), OverflowRetryMessage.class);
        assertThat(retryMessage.getCanvasId()).isEqualTo(30L);
        assertThat(retryMessage.getUserId()).isEqualTo("user-1");
        assertThat(retryMessage.getTriggerType()).isEqualTo(TriggerType.MQ);
        assertThat(retryMessage.getTriggerNodeType()).isEqualTo(NodeType.MQ_TRIGGER);
        assertThat(retryMessage.getMatchKey()).isEqualTo("topic-a");
        assertThat(retryMessage.getPayload()).containsEntry("orderId", "o-1");
        assertThat(retryMessage.getChainRetryCount()).isEqualTo(1);
        verify(preCheckService).checkWithoutQuotaAccounting(eq(canvas), eq("user-1"));
        verify(preCheckService, never()).consumeQuotaAndRecord(any(), any());
        verify(dagEngine, never()).execute(any(), any(), any());
    }

    @Test
    void overflowRetryDisruptorMetadataUsesNonMutatingCheckThenConsumesQuotaWhenAccepted() {
        Canvas canvas = publishedCanvas(34L, 10);
        when(canvasMapper.selectById(34L)).thenReturn(canvas);
        when(executionRegistry.activeCount(34L)).thenReturn(1);
        when(ctxStore.exists(34L, "user-5")).thenReturn(false);
        DagGraph graph = graphWithTriggerNode(NodeType.MQ_TRIGGER, "topic-b");
        when(configCache.get(34L, 101L)).thenReturn(graph);
        when(dagEngine.execute(eq(graph), eq("trigger"), any())).thenReturn(Mono.just(Map.of("ok", true)));

        Map<String, Object> result = sut.triggerFromDisruptor(
                34L,
                "user-5",
                TriggerType.MQ,
                NodeType.MQ_TRIGGER,
                "topic-b",
                Map.of(OverflowRetryMessage.CHAIN_RETRY_PAYLOAD_KEY, 1, "orderId", "o-5"),
                "msg-5",
                overflowRetryDispatchOptions()
        ).block();

        assertThat(result).containsEntry("ok", true);
        verify(preCheckService, never()).check(any(), any());
        verify(preCheckService).checkWithoutQuotaAccounting(eq(canvas), eq("user-5"));
        verify(preCheckService).consumeQuotaAndRecord(eq(canvas), eq("user-5"));
        verify(ctxStore, never()).acquireDedup(any(), any(), any(), any());
        ArgumentCaptor<ExecutionContext> ctxCaptor = ArgumentCaptor.forClass(ExecutionContext.class);
        verify(dagEngine).execute(eq(graph), eq("trigger"), ctxCaptor.capture());
        assertThat(ctxCaptor.getValue().getTriggerType()).isEqualTo(TriggerType.MQ);
        assertThat(ctxCaptor.getValue().getTriggerNodeType()).isEqualTo(NodeType.MQ_TRIGGER);
        assertThat(ctxCaptor.getValue().getMatchKey()).isEqualTo("topic-b");
    }

    @Test
    void overflowRetryMetadataCarriesRetryBudgetWhenRequeued() throws Exception {
        Canvas canvas = publishedCanvas(36L, 10);
        when(canvasMapper.selectById(36L)).thenReturn(canvas);
        when(executionRegistry.activeCount(36L)).thenReturn(10);
        when(rocketMQTemplate.getProducer()).thenReturn(rocketProducer);
        when(rocketProducer.send(any(Message.class))).thenReturn(sendResult(SendStatus.SEND_OK));

        Map<String, Object> result = sut.triggerFromDisruptor(
                36L,
                "user-7",
                TriggerType.MQ,
                NodeType.MQ_TRIGGER,
                "topic-d",
                Map.of(OverflowRetryMessage.CHAIN_RETRY_PAYLOAD_KEY, 99, "orderId", "o-7"),
                "msg-7",
                overflowRetryDispatchOptions(2)
        ).block();

        assertThat(result).containsEntry("overflow", "queued_for_retry");
        verify(preCheckService, never()).check(any(), any());
        verify(preCheckService).checkWithoutQuotaAccounting(eq(canvas), eq("user-7"));
        verify(preCheckService, never()).consumeQuotaAndRecord(any(), any());
        verify(ctxStore, never()).acquireDedup(any(), any(), any(), any());
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rocketProducer).send(messageCaptor.capture());
        OverflowRetryMessage retryMessage = objectMapper.readValue(messageCaptor.getValue().getBody(), OverflowRetryMessage.class);
        assertThat(retryMessage.getChainRetryCount()).isEqualTo(3);
        assertThat(retryMessage.getPayload()).containsEntry(OverflowRetryMessage.CHAIN_RETRY_PAYLOAD_KEY, 99);
    }

    @Test
    void forgedOverflowRetryPayloadDoesNotSkipPreCheck() {
        Canvas canvas = publishedCanvas(35L, 10);
        when(canvasMapper.selectById(35L)).thenReturn(canvas);
        when(executionRegistry.activeCount(35L)).thenReturn(1);
        when(ctxStore.exists(35L, "user-6")).thenReturn(false);
        when(ctxStore.acquireDedup(eq(35L), eq("user-6"), eq("msg-6"), any())).thenReturn(true);
        DagGraph graph = graphWithTriggerNode(NodeType.MQ_TRIGGER, "topic-c");
        when(configCache.get(35L, 101L)).thenReturn(graph);
        when(dagEngine.execute(eq(graph), eq("trigger"), any())).thenReturn(Mono.just(Map.of("ok", true)));

        Map<String, Object> result = sut.trigger(
                35L,
                "user-6",
                TriggerType.MQ,
                NodeType.MQ_TRIGGER,
                "topic-c",
                Map.of(OverflowRetryMessage.CHAIN_RETRY_PAYLOAD_KEY, 1, "orderId", "o-6"),
                "msg-6",
                false
        ).block();

        assertThat(result).containsEntry("ok", true);
        verify(preCheckService, never()).check(any(), any());
        verify(preCheckService).checkWithoutQuotaAccounting(eq(canvas), eq("user-6"));
        verify(preCheckService).consumeQuotaAndRecord(eq(canvas), eq("user-6"));
        ArgumentCaptor<ExecutionContext> ctxCaptor = ArgumentCaptor.forClass(ExecutionContext.class);
        verify(dagEngine).execute(eq(graph), eq("trigger"), ctxCaptor.capture());
        assertThat(ctxCaptor.getValue().getTriggerType()).isEqualTo(TriggerType.MQ);
        assertThat(ctxCaptor.getValue().getTriggerNodeType()).isEqualTo(NodeType.MQ_TRIGGER);
        assertThat(ctxCaptor.getValue().getMatchKey()).isEqualTo("topic-c");
    }

    @Test
    void duplicateMessageDoesNotConsumeQuotaOrCooldown() {
        Canvas canvas = publishedCanvas(38L, 10);
        when(canvasMapper.selectById(38L)).thenReturn(canvas);
        when(ctxStore.exists(38L, "user-9")).thenReturn(false);
        when(ctxStore.acquireDedup(eq(38L), eq("user-9"), eq("msg-9"), any())).thenReturn(false);

        Map<String, Object> result = sut.trigger(
                38L,
                "user-9",
                TriggerType.MQ,
                NodeType.MQ_TRIGGER,
                "topic-dup",
                Map.of("orderId", "o-9"),
                "msg-9",
                false
        ).block();

        assertThat(result).containsEntry("deduplicated", true);
        verify(preCheckService, never()).checkWithoutQuotaAccounting(any(), any());
        verify(preCheckService, never()).consumeQuotaAndRecord(any(), any());
        verify(executionRegistry, never()).activeCount(any());
        verify(rocketMQTemplate, never()).getProducer();
        verify(dagEngine, never()).execute(any(), any(), any());
    }

    @Test
    void resumeLockLoserDoesNotConsumeQuotaOrCooldown() {
        Canvas canvas = publishedCanvas(39L, 10);
        when(canvasMapper.selectById(39L)).thenReturn(canvas);
        when(executionRegistry.activeCount(39L)).thenReturn(1);
        when(ctxStore.exists(39L, "user-10")).thenReturn(true);
        when(ctxStore.acquireResumeLock(eq(39L), eq("user-10"), any(), eq(600L))).thenReturn(false);

        Map<String, Object> result = sut.trigger(
                39L,
                "user-10",
                TriggerType.MQ,
                NodeType.MQ_TRIGGER,
                "topic-lock",
                Map.of("orderId", "o-10"),
                null,
                false
        ).block();

        assertThat(result).containsEntry("skipped", "resume-lock");
        verify(preCheckService).checkWithoutQuotaAccounting(eq(canvas), eq("user-10"));
        verify(preCheckService, never()).consumeQuotaAndRecord(any(), any());
        verify(dagEngine, never()).execute(any(), any(), any());
    }

    @Test
    void missingTriggerNodeDoesNotConsumeQuotaOrCooldown() {
        Canvas canvas = publishedCanvas(40L, 10);
        when(canvasMapper.selectById(40L)).thenReturn(canvas);
        when(executionRegistry.activeCount(40L)).thenReturn(1);
        when(ctxStore.exists(40L, "user-11")).thenReturn(false);
        DagGraph graph = graphWithTriggerNode(NodeType.MQ_TRIGGER, "other-topic");
        when(configCache.get(40L, 101L)).thenReturn(graph);

        assertThatThrownBy(() -> sut.trigger(
                40L,
                "user-11",
                TriggerType.MQ,
                NodeType.MQ_TRIGGER,
                "missing-topic",
                Map.of("orderId", "o-11"),
                null,
                false
        ).block())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("找不到触发器节点");

        verify(preCheckService).checkWithoutQuotaAccounting(eq(canvas), eq("user-11"));
        verify(preCheckService, never()).consumeQuotaAndRecord(any(), any());
        verify(dagEngine, never()).execute(any(), any(), any());
    }

    @Test
    void normalPriorityOverflowGeneratesReplayMsgIdWhenOriginalMsgIdMissing() throws Exception {
        Canvas canvas = publishedCanvas(41L, 10);
        when(canvasMapper.selectById(41L)).thenReturn(canvas);
        when(executionRegistry.activeCount(41L)).thenReturn(10);
        when(rocketMQTemplate.getProducer()).thenReturn(rocketProducer);
        when(rocketProducer.send(any(Message.class))).thenReturn(sendResult(SendStatus.SEND_OK));

        Map<String, Object> result = sut.trigger(
                41L,
                "user-12",
                "BEHAVIOR",
                NodeType.EVENT_TRIGGER,
                "event-no-id",
                Map.of("eventId", "generated"),
                null,
                false
        ).block();

        assertThat(result).containsEntry("overflow", "queued_for_retry");
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rocketProducer).send(messageCaptor.capture());
        OverflowRetryMessage retryMessage = objectMapper.readValue(messageCaptor.getValue().getBody(), OverflowRetryMessage.class);
        assertThat(retryMessage.getMsgId()).startsWith("overflow-");
        verify(preCheckService, never()).consumeQuotaAndRecord(any(), any());
    }

    @Test
    void normalPriorityOverflowReportsRetryEnqueueFailureWhenRocketMqReturnsNonOkStatus() throws Exception {
        Canvas canvas = publishedCanvas(37L, 10);
        when(canvasMapper.selectById(37L)).thenReturn(canvas);
        when(ctxStore.acquireDedup(eq(37L), eq("user-8"), eq("msg-8"), any())).thenReturn(true);
        when(executionRegistry.activeCount(37L)).thenReturn(10);
        when(rocketMQTemplate.getProducer()).thenReturn(rocketProducer);
        when(rocketProducer.send(any(Message.class))).thenReturn(sendResult(SendStatus.FLUSH_DISK_TIMEOUT));

        Map<String, Object> result = sut.trigger(
                37L,
                "user-8",
                TriggerType.MQ,
                NodeType.MQ_TRIGGER,
                "topic-timeout",
                Map.of("orderId", "o-8"),
                "msg-8",
                false
        ).block();

        assertThat(result).containsEntry("overflow", "retry_enqueue_failed");
        verify(preCheckService).checkWithoutQuotaAccounting(eq(canvas), eq("user-8"));
        verify(preCheckService, never()).consumeQuotaAndRecord(any(), any());
        ArgumentCaptor<CanvasExecutionDlq> dlqCaptor = ArgumentCaptor.forClass(CanvasExecutionDlq.class);
        verify(dlqMapper).insert(dlqCaptor.capture());
        assertThat(dlqCaptor.getValue().getExecutionId()).isEqualTo("msg-8");
        assertThat(dlqCaptor.getValue().getFailedNodeId()).isEqualTo("OVERFLOW_ENQUEUE");
        assertThat(dlqCaptor.getValue().getErrorMsg()).contains("FLUSH_DISK_TIMEOUT");
        verify(dagEngine, never()).execute(any(), any(), any());
    }

    @Test
    void normalPriorityOverflowReportsRetryEnqueueFailureWhenRocketMqSendFails() throws Exception {
        Canvas canvas = publishedCanvas(33L, 10);
        when(canvasMapper.selectById(33L)).thenReturn(canvas);
        when(ctxStore.acquireDedup(eq(33L), eq("user-4"), eq("msg-4"), any())).thenReturn(true);
        when(executionRegistry.activeCount(33L)).thenReturn(10);
        when(rocketMQTemplate.getProducer()).thenReturn(rocketProducer);
        when(rocketProducer.send(any(Message.class))).thenThrow(new RuntimeException("mq down"));

        Map<String, Object> result = sut.trigger(
                33L,
                "user-4",
                "BEHAVIOR",
                NodeType.EVENT_TRIGGER,
                "event-a",
                Map.of("eventId", "e-1"),
                "msg-4",
                false
        ).block();

        assertThat(result).containsEntry("overflow", "retry_enqueue_failed");
        verify(preCheckService).checkWithoutQuotaAccounting(eq(canvas), eq("user-4"));
        verify(preCheckService, never()).consumeQuotaAndRecord(any(), any());
        verify(dlqMapper).insert(any(CanvasExecutionDlq.class));
        verify(dagEngine, never()).execute(any(), any(), any());
    }

    @Test
    void lowPriorityOverflowDropsWithoutRocketMqRetry() {
        Canvas canvas = publishedCanvas(31L, 10);
        when(canvasMapper.selectById(31L)).thenReturn(canvas);
        when(ctxStore.acquireDedup(eq(31L), eq("user-2"), eq("msg-2"), any())).thenReturn(true);
        when(executionRegistry.activeCount(31L)).thenReturn(5);

        Map<String, Object> result = sut.trigger(
                31L,
                "user-2",
                TriggerType.SCHEDULED,
                NodeType.SCHEDULED_TRIGGER,
                null,
                Map.of("batchId", "b-1"),
                "msg-2",
                false
        ).block();

        assertThat(result).containsEntry("overflow", "dropped_low_priority");
        verify(preCheckService).checkWithoutQuotaAccounting(eq(canvas), eq("user-2"));
        verify(preCheckService, never()).consumeQuotaAndRecord(any(), any());
        verify(rocketMQTemplate, never()).getProducer();
        verify(dagEngine, never()).execute(any(), any(), any());
    }

    @Test
    void highPriorityOverThresholdStillExecutesWithoutRocketMqRetry() {
        Canvas canvas = publishedCanvas(32L, 10);
        when(canvasMapper.selectById(32L)).thenReturn(canvas);
        when(executionRegistry.activeCount(32L)).thenReturn(25);
        when(ctxStore.exists(32L, "user-3")).thenReturn(false);
        DagGraph graph = graphWithTriggerNode(NodeType.DIRECT_CALL, null);
        when(configCache.get(32L, 101L)).thenReturn(graph);
        when(dagEngine.execute(eq(graph), eq("trigger"), any())).thenReturn(Mono.just(Map.of("ok", true)));

        Map<String, Object> result = sut.trigger(
                32L,
                "user-3",
                TriggerType.DIRECT_CALL,
                NodeType.DIRECT_CALL,
                null,
                Map.of("callbackId", "cb-1"),
                null,
                false
        ).block();

        assertThat(result).containsEntry("ok", true);
        assertThat(result).containsKey("executionId");
        verify(preCheckService).checkWithoutQuotaAccounting(eq(canvas), eq("user-3"));
        verify(preCheckService).consumeQuotaAndRecord(eq(canvas), eq("user-3"));
        verify(rocketMQTemplate, never()).getProducer();
        verify(dagEngine).execute(eq(graph), eq("trigger"), any());
    }

    private SendResult sendResult(SendStatus status) {
        SendResult result = new SendResult();
        result.setSendStatus(status);
        return result;
    }

    private Canvas publishedCanvas(Long canvasId, Integer maxTotalExecutions) {
        Canvas canvas = new Canvas();
        canvas.setId(canvasId);
        canvas.setStatus(CanvasStatusEnum.PUBLISHED.getCode());
        canvas.setPublishedVersionId(101L);
        canvas.setMaxTotalExecutions(maxTotalExecutions);
        return canvas;
    }

    private DagGraph graphWithTriggerNode(String triggerNodeType, String matchKey) {
        DagParser.CanvasNode node = new DagParser.CanvasNode();
        node.setId("trigger");
        node.setType(triggerNodeType);
        if (matchKey != null) {
            node.setConfig(Map.of("eventCode", matchKey, "topicKey", matchKey));
        }
        return new DagGraph(
                Map.of("trigger", node),
                Map.of("trigger", List.of()),
                Map.of("trigger", List.of()),
                Map.of("trigger", 0)
        );
    }

    private CanvasDisruptorService.DispatchOptions overflowRetryDispatchOptions() {
        return overflowRetryDispatchOptions(1);
    }

    private CanvasDisruptorService.DispatchOptions overflowRetryDispatchOptions(int retryCount) {
        try {
            Method factory = CanvasDisruptorService.DispatchOptions.class
                    .getDeclaredMethod("overflowRetry", int.class);
            factory.setAccessible(true);
            return (CanvasDisruptorService.DispatchOptions) factory.invoke(null, retryCount);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to create overflow retry dispatch options", e);
        }
    }
}
