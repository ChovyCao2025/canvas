package org.chovy.canvas.execution.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import org.junit.jupiter.api.Test;

/**
 * 定义 BackgroundTaskExecutorTest 的执行上下文数据结构或业务契约。
 */
class BackgroundTaskExecutorTest {

    /**
     * 执行 enforcesMaxConcurrentTasksAndReleasesPermitsWhenDone 对应的业务处理。
     */
    @Test
    void enforcesMaxConcurrentTasksAndReleasesPermitsWhenDone() throws Exception {
        BackgroundTaskExecutor executor = new BackgroundTaskExecutor(
                1,
                Executors.newFixedThreadPool(1),
                Duration.ofMillis(200));
        CountDownLatch release = new CountDownLatch(1);

        executor.submit("first", () -> {
            release.await();
            return null;
        });

        assertThatThrownBy(() -> executor.submit("second", () -> null))
                .isInstanceOf(RejectedExecutionException.class)
                .hasMessageContaining("Too many background tasks");

        release.countDown();
        Thread.sleep(50);

        assertThat(executor.submit("third", () -> "ok").get()).isEqualTo("ok");
        executor.shutdown();
    }
}
