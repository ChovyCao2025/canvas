package org.chovy.canvas.engine.trigger;

import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.engine.reactive.BackgroundSubscriptionRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    void schedulerClosedLifecycleStateIsAtomic() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/chovy/canvas/engine/trigger/CanvasSchedulerService.java"));

        assertThat(source).contains("AtomicBoolean closed");
        assertThat(source).doesNotContain("private boolean closed");
    }

    @Test
    void resolveUserIdsAsyncDefersTaggerGroupFetchUntilSubscribedAndPaginates() {
        AtomicInteger remoteCalls = new AtomicInteger();
        CanvasSchedulerService service = new CanvasSchedulerService(
                mock(CanvasExecutionService.class),
                mock(org.chovy.canvas.engine.schedule.ScheduleRegistrar.class));
        ReflectionTestUtils.setField(service, "taggerClient", taggerClient(remoteCalls));

        Mono<List<String>> users = service.resolveUserIdsAsync("TAGGER_GROUP", Map.of(
                "tagCode", "vip",
                "limit", 3,
                "pageSize", 2));

        assertThat(remoteCalls).hasValue(0);
        StepVerifier.create(users)
                .expectNext(List.of("u1", "u2", "u3"))
                .verifyComplete();
        assertThat(remoteCalls).hasValue(2);
    }

    @Test
    void dispatchScheduledTriggerTracksExecutionSubscriptionAsBackgroundTask() throws Exception {
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        BackgroundSubscriptionRegistry backgroundSubscriptions = new BackgroundSubscriptionRegistry();
        CountDownLatch subscribed = new CountDownLatch(1);
        when(executionService.trigger(
                eq(1L), eq("user-1"), eq(TriggerType.SCHEDULED),
                eq(NodeType.SCHEDULED_TRIGGER), eq("scheduled-node"),
                any(), any(), eq(false)))
                .thenReturn(Mono.<Map<String, Object>>never()
                        .doOnSubscribe(subscription -> subscribed.countDown()));
        CanvasSchedulerService service = new CanvasSchedulerService(
                executionService,
                mock(org.chovy.canvas.engine.schedule.ScheduleRegistrar.class),
                backgroundSubscriptions);
        CanvasSchedulerService.PendingJitterGroup group =
                service.createPendingJitterGroup("1:scheduled-node");

        service.dispatchScheduledTrigger(group, 1L, "user-1");

        assertThat(subscribed.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat((Integer) ReflectionTestUtils.invokeMethod(backgroundSubscriptions, "activeCount"))
                .isEqualTo(1);
    }

    private static WebClient taggerClient(AtomicInteger remoteCalls) {
        ExchangeFunction exchangeFunction = request -> {
            remoteCalls.incrementAndGet();
            String query = request.url().getQuery();
            String body = query.contains("page=1")
                    ? "{\"userIds\":[\"u1\",\"u2\"]}"
                    : "{\"userIds\":[\"u3\"]}";
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .body(body)
                    .build());
        };
        return WebClient.builder().exchangeFunction(exchangeFunction).build();
    }
}
