package org.chovy.canvas.engine.disruptor;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.chovy.canvas.engine.lane.ExecutionLane;
import org.chovy.canvas.engine.request.CanvasExecutionRequestExecutor;
import org.chovy.canvas.engine.scheduler.CanvasMetrics;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CanvasDisruptorServiceLifecycleTest {

    private CanvasDisruptorService service;

    @AfterEach
    void tearDown() {
        if (service != null) {
            service.shutdown();
        }
    }

    @Test
    void asyncErrorLogKeepsEventIdentifiersAfterRingBufferEventReset() throws Exception {
        CapturingAppender appender = CapturingAppender.attach();
        try {
            CanvasExecutionService executionService = mock(CanvasExecutionService.class);
            when(executionService.triggerFromDisruptor(
                    anyLong(), anyString(), anyString(), anyString(), anyString(), anyMap(),
                    anyString(), nullable(ExecutionLane.class), any(CanvasDisruptorService.DispatchOptions.class)))
                    .thenReturn(Mono.delay(Duration.ofMillis(50))
                            .then(Mono.error(new RuntimeException("boom"))));
            service = new CanvasDisruptorService(
                    executionService, mock(CanvasExecutionRequestExecutor.class), mock(CanvasMetrics.class), 1024, 1);

            service.publish(10L, "u1", "MQ", "MQ_TRIGGER", "topic", Map.of("k", "v"), "msg-1");

            assertThat(appender.awaitError()).isTrue();
            assertThat(appender.errorMessage())
                    .contains("canvasId=10")
                    .contains("userId=u1")
                    .contains("boom");
        } finally {
            appender.detach();
        }
    }

    @Test
    void shutdownWaitsForSubscribedCanvasExecutionToFinish() throws Exception {
        CountDownLatch subscribed = new CountDownLatch(1);
        AtomicReference<MonoSink<Map<String, Object>>> sinkRef = new AtomicReference<>();
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        when(executionService.triggerFromDisruptor(
                anyLong(), anyString(), anyString(), anyString(), anyString(), anyMap(),
                anyString(), nullable(ExecutionLane.class), any(CanvasDisruptorService.DispatchOptions.class)))
                .thenReturn(Mono.<Map<String, Object>>create(sink -> {
                    sinkRef.set(sink);
                    subscribed.countDown();
                }));
        service = new CanvasDisruptorService(
                executionService, mock(CanvasExecutionRequestExecutor.class), mock(CanvasMetrics.class), 1024, 1);

        service.publish(10L, "u1", "MQ", "MQ_TRIGGER", "topic", Map.of(), "msg-1");
        assertThat(subscribed.await(2, TimeUnit.SECONDS)).isTrue();

        CountDownLatch shutdownReturned = new CountDownLatch(1);
        Thread shutdownThread = new Thread(() -> {
            service.shutdown();
            shutdownReturned.countDown();
        });
        shutdownThread.start();

        assertThat(shutdownReturned.await(150, TimeUnit.MILLISECONDS)).isFalse();

        sinkRef.get().success(Map.of("ok", true));

        assertThat(shutdownReturned.await(2, TimeUnit.SECONDS)).isTrue();
        shutdownThread.join();
        service = null;
    }

    private static final class CapturingAppender extends AppenderBase<ILoggingEvent> {
        private final CountDownLatch errorLogged = new CountDownLatch(1);
        private final AtomicReference<String> errorMessage = new AtomicReference<>();
        private final Logger logger;

        private CapturingAppender(Logger logger) {
            this.logger = logger;
        }

        static CapturingAppender attach() {
            Logger logger = (Logger) LoggerFactory.getLogger(CanvasDisruptorService.class);
            CapturingAppender appender = new CapturingAppender(logger);
            appender.start();
            logger.addAppender(appender);
            return appender;
        }

        @Override
        protected void append(ILoggingEvent eventObject) {
            if (eventObject.getLevel().isGreaterOrEqual(Level.ERROR)
                    && eventObject.getFormattedMessage().contains("[DISRUPTOR] 执行失败")) {
                errorMessage.set(eventObject.getFormattedMessage());
                errorLogged.countDown();
            }
        }

        boolean awaitError() throws InterruptedException {
            return errorLogged.await(2, TimeUnit.SECONDS);
        }

        String errorMessage() {
            return errorMessage.get();
        }

        void detach() {
            logger.detachAppender(this);
            stop();
        }
    }
}
