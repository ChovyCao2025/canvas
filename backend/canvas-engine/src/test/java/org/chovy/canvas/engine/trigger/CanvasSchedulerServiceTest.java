package org.chovy.canvas.engine.trigger;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.Map;
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
