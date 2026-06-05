package org.chovy.canvas.engine.trigger;

import org.junit.jupiter.api.Test;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.schedule.ScheduleKey;
import org.chovy.canvas.engine.schedule.ScheduleRegistrar;
import org.chovy.canvas.engine.schedule.ScheduleRegistration;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Canvas Scheduler 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class CanvasSchedulerServiceTest {

    @Test
    void calcJitterReturnsZeroWhenDisabled() {
        assertThat(CanvasSchedulerService.calcJitter(0)).isEqualTo(Duration.ZERO);
        assertThat(CanvasSchedulerService.calcJitter(-1)).isEqualTo(Duration.ZERO);
    }

    @Test
    void calcJitterReturnsValueWithinExclusiveUpperBound() {
        for (int i = 0; i < 100; i++) {
            long millis = CanvasSchedulerService.calcJitter(60_000).toMillis();
            assertThat(millis).isBetween(0L, 59_999L);
        }
    }

    @Test
    void extractsNodeIdFromScheduleTaskKey() {
        assertThat(CanvasSchedulerService.nodeIdFromTaskKey("62:scheduled-node")).isEqualTo("scheduled-node");
        assertThat(CanvasSchedulerService.nodeIdFromTaskKey("scheduled-node")).isEqualTo("scheduled-node");
    }

    @Test
    void cancelAllRejectsPendingJitterGroupCreationAfterClose() throws Exception {
        CanvasSchedulerService service = new CanvasSchedulerService(
                mock(CanvasExecutionService.class),
                new NoopScheduleRegistrar(),
                WebClient.builder());
        CanvasSchedulerService.PendingJitterGroup beforeClose =
                service.createPendingJitterGroup("before-close");

        service.cancelAll();

        assertThat(service.isClosed()).isTrue();
        assertThat(beforeClose.isTerminated()).isTrue();
        assertThat(service.hasPendingJitterGroup("before-close")).isFalse();
        assertThat(service.createPendingJitterGroup("late")).isNull();
    }

    @Test
    void closedSchedulerRejectsConcurrentPendingJitterGroupCreation() throws Exception {
        CanvasSchedulerService service = new CanvasSchedulerService(
                mock(CanvasExecutionService.class),
                new NoopScheduleRegistrar(),
                WebClient.builder());
        service.cancelAll();
        int threads = 32;
        AtomicInteger rejected = new AtomicInteger();
        var executor = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            final int index = i;
            executor.submit(() -> {
                if (service.createPendingJitterGroup("task-" + index) == null) {
                    rejected.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        assertThat(rejected.get()).isEqualTo(threads);
    }

    @Test
    void replaceScheduledTriggersClearsOldRegistrationsWithoutClosingScheduler() {
        RecordingScheduleRegistrar registrar = new RecordingScheduleRegistrar();
        CanvasSchedulerService service = new CanvasSchedulerService(
                mock(CanvasExecutionService.class),
                registrar,
                WebClient.builder());
        service.createPendingJitterGroup("old-task");

        int rebuilt = service.replaceScheduledTriggers(Map.of(10L, graphWithScheduledNode("scheduled-1")));

        assertThat(rebuilt).isEqualTo(1);
        assertThat(service.isClosed()).isFalse();
        assertThat(service.hasPendingJitterGroup("old-task")).isFalse();
        assertThat(registrar.unregistered).containsExactly(
                new ScheduleKey("canvas", "old-task"),
                new ScheduleKey("canvas", "10:scheduled-1"));
        assertThat(registrar.registered).extracting(registration -> registration.key().id())
                .containsExactly("10:scheduled-1");
    }

    private DagGraph graphWithScheduledNode(String nodeId) {
        DagParser.CanvasNode node = new DagParser.CanvasNode();
        node.setId(nodeId);
        node.setType(NodeType.SCHEDULED_TRIGGER);
        node.setConfig(Map.of("cronExpression", "0 0/5 * * * *"));
        node.setBizConfig(Map.of());
        return new DagGraph(
                Map.of(nodeId, node),
                Map.of(nodeId, List.of()),
                Map.of(nodeId, List.of()),
                Map.of(nodeId, 0));
    }

    private static final class NoopScheduleRegistrar implements ScheduleRegistrar {
        @Override
        public void register(ScheduleRegistration registration) {
        }

        @Override
        public void unregister(ScheduleKey key) {
        }
    }

    private static final class RecordingScheduleRegistrar implements ScheduleRegistrar {
        private final List<ScheduleRegistration> registered = new ArrayList<>();
        private final List<ScheduleKey> unregistered = new ArrayList<>();

        @Override
        public void register(ScheduleRegistration registration) {
            registered.add(registration);
        }

        @Override
        public void unregister(ScheduleKey key) {
            unregistered.add(key);
        }
    }
}
