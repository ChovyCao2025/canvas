package org.chovy.canvas.infrastructure.observability;

import org.chovy.canvas.config.CorrelationIdWebFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class MdcTaskDecoratorTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void callableUsesCapturedMdcAndRestoresWorkerMdc() throws Exception {
        MDC.put(CorrelationIdWebFilter.MDC_KEY, "request-trace");
        Callable<String> decorated = MdcTaskDecorator.decorate(() -> {
            String traceId = MDC.get(CorrelationIdWebFilter.MDC_KEY);
            MDC.put("temporary", "inside-task");
            return traceId;
        });

        MDC.clear();
        MDC.put(CorrelationIdWebFilter.MDC_KEY, "worker-trace");

        assertThat(decorated.call()).isEqualTo("request-trace");
        assertThat(MDC.get(CorrelationIdWebFilter.MDC_KEY)).isEqualTo("worker-trace");
        assertThat(MDC.get("temporary")).isNull();
    }

    @Test
    void runnableClearsWorkerMdcWhenCallerHadNoMdc() {
        Runnable decorated = MdcTaskDecorator.decorate(() ->
                assertThat(MDC.get(CorrelationIdWebFilter.MDC_KEY)).isNull());
        MDC.put(CorrelationIdWebFilter.MDC_KEY, "worker-trace");

        decorated.run();

        assertThat(MDC.get(CorrelationIdWebFilter.MDC_KEY)).isEqualTo("worker-trace");
    }

    @Test
    void runnablePropagatesCapturedValues() {
        MDC.put(CorrelationIdWebFilter.MDC_KEY, "request-trace");
        AtomicReference<String> observed = new AtomicReference<>();
        Runnable decorated = MdcTaskDecorator.decorate(() ->
                observed.set(MDC.get(CorrelationIdWebFilter.MDC_KEY)));

        MDC.clear();
        decorated.run();

        assertThat(observed.get()).isEqualTo("request-trace");
        assertThat(MDC.get(CorrelationIdWebFilter.MDC_KEY)).isNull();
    }
}
