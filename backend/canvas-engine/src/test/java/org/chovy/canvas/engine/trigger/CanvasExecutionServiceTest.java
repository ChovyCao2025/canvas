package org.chovy.canvas.engine.trigger;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.common.enums.ExecutionStatus;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.domain.cdp.CdpUserService;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDO;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDlqDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionDlqMapper;
import org.chovy.canvas.dal.mapper.CanvasExecutionMapper;
import org.chovy.canvas.dal.mapper.CanvasExecutionStatsMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.context.NodeStatus;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.handler.HandlerRegistry;
import org.chovy.canvas.engine.handlers.MqTriggerHandler;
import org.chovy.canvas.engine.scheduler.CanvasMetrics;
import org.chovy.canvas.engine.scheduler.CircuitBreakerRegistry;
import org.chovy.canvas.engine.scheduler.DagEngine;
import org.chovy.canvas.engine.scheduler.TraceWriteBuffer;
import org.chovy.canvas.infrastructure.cache.CanvasConfigCache;
import org.chovy.canvas.infrastructure.cache.CanvasEntityCache;
import org.chovy.canvas.infrastructure.mq.OverflowRetryMessage;
import org.chovy.canvas.infrastructure.redis.ContextPersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.Disposables;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
    @Mock CanvasEntityCache canvasEntityCache;
    @Mock MqTriggerHandler mqTriggerHandler;
    @Mock CanvasExecutionDlqMapper dlqMapper;
    @Mock RocketMQTemplate rocketMQTemplate;
    @Mock DefaultMQProducer rocketProducer;
    @Mock CdpUserService cdpUserService;

    TriggerPriorityConfig priorityConfig;
    ObjectMapper objectMapper;
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
                canvasEntityCache,
                mqTriggerHandler,
                dlqMapper,
                priorityConfig,
                rocketMQTemplate,
                objectMapper,
                cdpUserService
        );
        ReflectionTestUtils.setField(sut, "ctxTtlSec", 86400L);
        ReflectionTestUtils.setField(sut, "globalTimeoutSec", 600L);
        ReflectionTestUtils.setField(sut, "globalMaxConcurrency", 1000);
    }

    @Test
    void invalidateCanvas_delegatesToCanvasEntityCache() {
        sut.invalidateCanvas(20L);

        verify(canvasEntityCache).invalidate(20L);
    }

    @Test
    void triggerPropagatesExecutionInsertFailureBeforeRunningDagAndReleasesDedup() {
        CanvasDO canvas = publishedCanvas(10L, 10);
        when(canvasEntityCache.get(10L)).thenReturn(canvas);
        when(ctxStore.exists(10L, "user-7")).thenReturn(false);
        when(ctxStore.acquireDedup(eq(10L), eq("user-7"), eq("MSG-1"), any()))
                .thenReturn(true);
        when(ctxStore.buildDedupKey(10L, "user-7", "MSG-1")).thenReturn("dedup-key");

        DagGraph graph = graphWithTriggerNode(NodeType.DIRECT_CALL, null);
        when(configCache.get(10L, 101L)).thenReturn(graph);
        when(executionRegistry.tryAcquire(eq(10L), any(), eq(1000), eq(1000)))
                .thenReturn(Optional.of(Disposables.swap()));
        when(dagEngine.execute(eq(graph), eq("trigger"), any()))
                .thenReturn(Mono.just(Map.of("ok", true)));
        when(executionMapper.insert(any(CanvasExecutionDO.class))).thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> sut.trigger(
                10L,
                "user-7",
                TriggerType.DIRECT_CALL,
                NodeType.DIRECT_CALL,
                null,
                Map.of(),
                "MSG-1",
                false).block())
                .hasMessageContaining("db down");

        verify(ctxStore).releaseDedup("dedup-key");
    }

    @Test
    void normalPriorityOverflowQueuesRocketMqRetryWithIncrementedChainRetryCount() throws Exception {
        CanvasDO canvas = publishedCanvas(30L, 10);
        when(canvasEntityCache.get(30L)).thenReturn(canvas);
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
        assertThat(retryMessage.getPayload())
                .containsEntry("orderId", "o-1")
                .doesNotContainKey(OverflowRetryMessage.CHAIN_RETRY_PAYLOAD_KEY);
        assertThat(retryMessage.getChainRetryCount()).isEqualTo(1);
        verify(preCheckService).checkWithoutQuotaAccounting(eq(canvas), eq("user-1"));
        verify(preCheckService, never()).consumeQuotaAndRecord(any(), any());
        verify(dagEngine, never()).execute(any(), any(), any());
    }

    @Test
    void executionRequestOverflowReturnsRetrySignalWithoutRocketMqOverflowMessage() {
        CanvasDO canvas = publishedCanvas(42L, 10);
        when(canvasEntityCache.get(42L)).thenReturn(canvas);
        when(ctxStore.acquireDedup(eq(42L), eq("user-13"), eq("msg-13"), any())).thenReturn(true);
        when(executionRegistry.activeCount(42L)).thenReturn(10);

        Map<String, Object> result = sut.triggerFromExecutionRequest(
                42L,
                "user-13",
                TriggerType.MQ,
                NodeType.MQ_TRIGGER,
                "topic-request",
                Map.of("orderId", "o-13"),
                "msg-13"
        ).block();

        assertThat(result).containsEntry("overflow", "request_retry");
        verify(preCheckService).checkWithoutQuotaAccounting(eq(canvas), eq("user-13"));
        verify(preCheckService, never()).consumeQuotaAndRecord(any(), any());
        verify(rocketMQTemplate, never()).getProducer();
        verify(dagEngine, never()).execute(any(), any(), any());
    }

    @Test
    void overflowRetryDisruptorMetadataUsesNonMutatingCheckThenConsumesQuotaWhenAccepted() {
        CanvasDO canvas = publishedCanvas(34L, 10);
        when(canvasEntityCache.get(34L)).thenReturn(canvas);
        when(executionRegistry.activeCount(34L)).thenReturn(1);
        when(ctxStore.exists(34L, "user-5")).thenReturn(false);
        DagGraph graph = graphWithTriggerNode(NodeType.MQ_TRIGGER, "topic-b");
        when(configCache.get(34L, 101L)).thenReturn(graph);
        when(mqTriggerHandler.resolveTopic(any())).thenReturn("topic-b");
        when(executionRegistry.tryAcquire(eq(34L), any(), eq(10), eq(1000)))
                .thenReturn(Optional.of(Disposables.swap()));
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
        CanvasDO canvas = publishedCanvas(36L, 10);
        when(canvasEntityCache.get(36L)).thenReturn(canvas);
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
        assertThat(retryMessage.getPayload()).doesNotContainKey(OverflowRetryMessage.CHAIN_RETRY_PAYLOAD_KEY);
    }

    @Test
    void forgedOverflowRetryPayloadDoesNotSkipPreCheck() {
        CanvasDO canvas = publishedCanvas(35L, 10);
        when(canvasEntityCache.get(35L)).thenReturn(canvas);
        when(executionRegistry.activeCount(35L)).thenReturn(1);
        when(ctxStore.exists(35L, "user-6")).thenReturn(false);
        when(ctxStore.acquireDedup(eq(35L), eq("user-6"), eq("msg-6"), any())).thenReturn(true);
        DagGraph graph = graphWithTriggerNode(NodeType.MQ_TRIGGER, "topic-c");
        when(configCache.get(35L, 101L)).thenReturn(graph);
        when(mqTriggerHandler.resolveTopic(any())).thenReturn("topic-c");
        when(executionRegistry.tryAcquire(eq(35L), any(), eq(10), eq(1000)))
                .thenReturn(Optional.of(Disposables.swap()));
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
        assertThat(ctxCaptor.getValue().getTriggerPayload())
                .doesNotContainKey(OverflowRetryMessage.CHAIN_RETRY_PAYLOAD_KEY);
    }

    @Test
    void duplicateMessageDoesNotConsumeQuotaOrCooldown() {
        CanvasDO canvas = publishedCanvas(38L, 10);
        when(canvasEntityCache.get(38L)).thenReturn(canvas);
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
        CanvasDO canvas = publishedCanvas(39L, 10);
        when(canvasEntityCache.get(39L)).thenReturn(canvas);
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
        CanvasDO canvas = publishedCanvas(40L, 10);
        when(canvasEntityCache.get(40L)).thenReturn(canvas);
        when(executionRegistry.activeCount(40L)).thenReturn(1);
        when(ctxStore.exists(40L, "user-11")).thenReturn(false);
        DagGraph graph = graphWithTriggerNode(NodeType.MQ_TRIGGER, "other-topic");
        when(configCache.get(40L, 101L)).thenReturn(graph);
        when(mqTriggerHandler.resolveTopic(any())).thenReturn("other-topic");

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
        CanvasDO canvas = publishedCanvas(41L, 10);
        when(canvasEntityCache.get(41L)).thenReturn(canvas);
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
        CanvasDO canvas = publishedCanvas(37L, 10);
        when(canvasEntityCache.get(37L)).thenReturn(canvas);
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
                Map.of("orderId", "o-8", "perfRunId", "perf_20260523_003"),
                "msg-8",
                false
        ).block();

        assertThat(result).containsEntry("overflow", "retry_enqueue_failed");
        verify(preCheckService).checkWithoutQuotaAccounting(eq(canvas), eq("user-8"));
        verify(preCheckService, never()).consumeQuotaAndRecord(any(), any());
        ArgumentCaptor<CanvasExecutionDlqDO> dlqCaptor = ArgumentCaptor.forClass(CanvasExecutionDlqDO.class);
        verify(dlqMapper).insert(dlqCaptor.capture());
        assertThat(dlqCaptor.getValue().getExecutionId()).isEqualTo("msg-8");
        assertThat(dlqCaptor.getValue().getPerfRunId()).isEqualTo("perf_20260523_003");
        assertThat(dlqCaptor.getValue().getFailedNodeId()).isEqualTo("OVERFLOW_ENQUEUE");
        assertThat(dlqCaptor.getValue().getErrorMsg()).contains("FLUSH_DISK_TIMEOUT");
        verify(dagEngine, never()).execute(any(), any(), any());
    }

    @Test
    void normalPriorityOverflowReportsRetryEnqueueFailureWhenRocketMqSendFails() throws Exception {
        CanvasDO canvas = publishedCanvas(33L, 10);
        when(canvasEntityCache.get(33L)).thenReturn(canvas);
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
        verify(dlqMapper).insert(any(CanvasExecutionDlqDO.class));
        verify(dagEngine, never()).execute(any(), any(), any());
    }

    @Test
    void lowPriorityOverflowDropsWithoutRocketMqRetry() {
        CanvasDO canvas = publishedCanvas(31L, 10);
        when(canvasEntityCache.get(31L)).thenReturn(canvas);
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
        CanvasDO canvas = publishedCanvas(32L, 10);
        when(canvasEntityCache.get(32L)).thenReturn(canvas);
        when(executionRegistry.activeCount(32L)).thenReturn(25);
        when(ctxStore.exists(32L, "user-3")).thenReturn(false);
        DagGraph graph = graphWithTriggerNode(NodeType.DIRECT_CALL, null);
        when(configCache.get(32L, 101L)).thenReturn(graph);
        when(executionRegistry.tryAcquire(eq(32L), any(), eq(1000), eq(1000)))
                .thenReturn(Optional.of(Disposables.swap()));
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

    @Test
    void triggerPersistsExecutionPerfRunIdFromTriggerPayload() {
        CanvasDO canvas = publishedCanvas(43L, 10);
        when(canvasEntityCache.get(43L)).thenReturn(canvas);
        when(executionRegistry.activeCount(43L)).thenReturn(1);
        when(ctxStore.exists(43L, "user-14")).thenReturn(false);
        DagGraph graph = graphWithTriggerNode(NodeType.DIRECT_CALL, null);
        when(configCache.get(43L, 101L)).thenReturn(graph);
        when(executionRegistry.tryAcquire(eq(43L), any(), eq(1000), eq(1000)))
                .thenReturn(Optional.of(Disposables.swap()));
        when(dagEngine.execute(eq(graph), eq("trigger"), any())).thenReturn(Mono.just(Map.of("ok", true)));

        Map<String, Object> result = sut.trigger(
                43L,
                "user-14",
                TriggerType.DIRECT_CALL,
                NodeType.DIRECT_CALL,
                null,
                Map.of("perfRunId", "perf_20260523_001", "amount", 88),
                null,
                false
        ).block();

        assertThat(result).containsEntry("ok", true);
        ArgumentCaptor<CanvasExecutionDO> executionCaptor = ArgumentCaptor.forClass(CanvasExecutionDO.class);
        verify(executionMapper).insert(executionCaptor.capture());
        assertThat(executionCaptor.getValue().getPerfRunId()).isEqualTo("perf_20260523_001");
    }

    @Test
    void triggerPersistsPerfDedupKeyForDirectVerification() {
        CanvasDO canvas = publishedCanvas(45L, 10);
        when(canvasEntityCache.get(45L)).thenReturn(canvas);
        when(executionRegistry.activeCount(45L)).thenReturn(1);
        when(ctxStore.exists(45L, "user-16")).thenReturn(false);
        when(ctxStore.acquireDedup(eq(45L), eq("user-16"), eq("perf_20260523_005:direct:1"), any()))
                .thenReturn(true);
        when(ctxStore.buildDedupKey(45L, "user-16", "perf_20260523_005:direct:1"))
                .thenReturn("dedup:45:user-16:perf_20260523_005:direct:1");
        DagGraph graph = graphWithTriggerNode(NodeType.DIRECT_CALL, null);
        when(configCache.get(45L, 101L)).thenReturn(graph);
        when(executionRegistry.tryAcquire(eq(45L), any(), eq(1000), eq(1000)))
                .thenReturn(Optional.of(Disposables.swap()));
        when(dagEngine.execute(eq(graph), eq("trigger"), any())).thenReturn(Mono.just(Map.of("ok", true)));

        Map<String, Object> result = sut.trigger(
                45L,
                "user-16",
                TriggerType.DIRECT_CALL,
                NodeType.DIRECT_CALL,
                null,
                Map.of("perfRunId", "perf_20260523_005", "amount", 88),
                "perf_20260523_005:direct:1",
                false
        ).block();

        assertThat(result).containsEntry("ok", true);
        ArgumentCaptor<CanvasExecutionDO> executionCaptor = ArgumentCaptor.forClass(CanvasExecutionDO.class);
        verify(executionMapper).insert(executionCaptor.capture());
        assertThat(executionCaptor.getValue().getPerfRunId()).isEqualTo("perf_20260523_005");
        assertThat(executionCaptor.getValue().getLastDedupKey())
                .isEqualTo("dedup:45:user-16:perf_20260523_005:direct:1");
    }

    @Test
    void triggerDryRunPersistsExecutionPerfRunIdFromTriggerPayload() {
        CanvasDO canvas = publishedCanvas(44L, 10);
        when(canvasMapper.selectById(44L)).thenReturn(canvas);
        DagGraph graph = graphWithTriggerNode(NodeType.DIRECT_CALL, null);
        when(dagParser.parse("graph-json")).thenReturn(graph);
        when(dagEngine.execute(eq(graph), eq("trigger"), any())).thenReturn(Mono.just(Map.of("ok", true)));

        Map<String, Object> result = sut.triggerDryRun(
                44L,
                "user-15",
                Map.of("perfRunId", "perf_20260523_002", "amount", 99),
                "graph-json"
        ).block();

        assertThat(result).containsEntry("ok", true);
        ArgumentCaptor<CanvasExecutionDO> executionCaptor = ArgumentCaptor.forClass(CanvasExecutionDO.class);
        verify(executionMapper).insert(executionCaptor.capture());
        assertThat(executionCaptor.getValue().getPerfRunId()).isEqualTo("perf_20260523_002");
    }

    @Test
    void dagEngineDlqPersistsPerfRunIdFromExecutionContext() throws Exception {
        CanvasExecutionDlqMapper mapper = mock(CanvasExecutionDlqMapper.class);
        DagEngine engine = new DagEngine(
                mock(HandlerRegistry.class),
                mock(TraceWriteBuffer.class),
                mapper,
                mock(CircuitBreakerRegistry.class),
                mock(CanvasMetrics.class),
                objectMapper,
                mock(ContextPersistenceService.class),
                mock(CanvasExecutionService.class)
        );
        ReflectionTestUtils.setField(engine, "maxRetry", 3);

        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-99");
        ctx.setCanvasId(99L);
        ctx.setUserId("user-99");
        ctx.setPerfRunId("perf_20260523_004");
        ctx.setTriggerType(TriggerType.MQ);
        ctx.setTriggerNodeType(NodeType.MQ_TRIGGER);
        ctx.setMatchKey("topic-dlq");
        ctx.getTriggerPayload().put("orderId", "o-99");

        Method writeDlq = DagEngine.class.getDeclaredMethod(
                "writeDlq", ExecutionContext.class, String.class, String.class, Throwable.class);
        writeDlq.setAccessible(true);
        writeDlq.invoke(engine, ctx, "node-1", NodeType.API_CALL, new RuntimeException("node failed"));

        ArgumentCaptor<CanvasExecutionDlqDO> dlqCaptor = ArgumentCaptor.forClass(CanvasExecutionDlqDO.class);
        verify(mapper, timeout(1_000)).insert(dlqCaptor.capture());
        assertThat(dlqCaptor.getValue().getPerfRunId()).isEqualTo("perf_20260523_004");
    }

    private SendResult sendResult(SendStatus status) {
        SendResult result = new SendResult();
        result.setSendStatus(status);
        return result;
    }

    private CanvasDO publishedCanvas(Long canvasId, Integer maxTotalExecutions) {
        CanvasDO canvas = new CanvasDO();
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

    @Test
    void triggerPersistsContextAndMarksExecutionPausedWhenDagCompletesWithWaitingNode() {
        CanvasDO canvas = new CanvasDO();
        canvas.setId(10L);
        canvas.setStatus(CanvasStatusEnum.PUBLISHED.getCode());
        canvas.setPublishedVersionId(100L);
        when(canvasEntityCache.get(10L)).thenReturn(canvas);
        when(ctxStore.exists(10L, "user-7")).thenReturn(false);

        DagParser.CanvasNode start = new DagParser.CanvasNode();
        start.setId("start");
        start.setType(NodeType.DIRECT_CALL);
        DagGraph graph = new DagGraph(
                Map.of("start", start),
                Map.of("start", List.of()),
                Map.of("start", List.of()),
                Map.of("start", 0));
        when(configCache.get(10L, 100L)).thenReturn(graph);
        when(executionRegistry.tryAcquire(eq(10L), any(), eq(1000), eq(1000)))
                .thenReturn(Optional.of(Disposables.swap()));
        when(dagEngine.execute(eq(graph), eq("start"), any()))
                .thenAnswer(invocation -> {
                    ExecutionContext ctx = invocation.getArgument(2);
                    ctx.setNodeStatus("wait-1", NodeStatus.WAITING);
                    return Mono.just(Map.of());
                });

        Map<String, Object> result = sut.trigger(
                10L,
                "user-7",
                TriggerType.DIRECT_CALL,
                NodeType.DIRECT_CALL,
                null,
                Map.of(),
                null,
                false).block();

        assertThat(result).containsKey("executionId");
        verify(ctxStore).save(any(ExecutionContext.class));
        verify(ctxStore, never()).delete(10L, "user-7");

        var captor = org.mockito.ArgumentCaptor.forClass(CanvasExecutionDO.class);
        verify(executionMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ExecutionStatus.PAUSED.getCode());
    }
}
