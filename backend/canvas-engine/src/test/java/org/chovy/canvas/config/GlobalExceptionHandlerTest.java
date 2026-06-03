package org.chovy.canvas.config;

import org.chovy.canvas.common.R;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void generalExceptionUsesMdcTraceIdAndDoesNotLeakExceptionMessage() {
        MDC.put("traceId", "trace-123");
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        R<Void> response = handler.handleGeneral(
                new RuntimeException("database password=secret-token"));

        assertThat(response.getCode()).isEqualTo(500);
        assertThat(response.getMessage()).isEqualTo("系统错误");
        assertThat(response.getTraceId()).isEqualTo("trace-123");
    }

    @Test
    void generalExceptionGeneratesTraceIdWhenMdcIsEmpty() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        R<Void> response = handler.handleGeneral(new RuntimeException("boom"));

        assertThat(response.getCode()).isEqualTo(500);
        assertThat(response.getMessage()).isEqualTo("系统错误");
        assertThat(response.getTraceId()).isNotBlank();
        assertThatCodeIsUuid(response.getTraceId());
    }

    private void assertThatCodeIsUuid(String traceId) {
        assertThat(UUID.fromString(traceId).toString()).isEqualTo(traceId);
    }
}
