package org.chovy.canvas.config;

import org.chovy.canvas.common.ErrorCode;
import org.chovy.canvas.common.R;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTraceTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void genericErrorIncludesPublicTraceIdAndHidesExceptionMessage() {
        MDC.put(CorrelationIdWebFilter.MDC_KEY, "trace-from-filter");
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        R<Void> response = handler.handleGeneral(
                new RuntimeException("secret database password leaked"));

        assertThat(response.getCode()).isEqualTo(500);
        assertThat(response.getErrorCode()).isEqualTo(ErrorCode.API_005);
        assertThat(response.getMessage()).isEqualTo("系统错误");
        assertThat(response.getTraceId()).isEqualTo("trace-from-filter");
    }

    @Test
    void responseStatusErrorIncludesCurrentTraceId() {
        MDC.put(CorrelationIdWebFilter.MDC_KEY, "status-trace");
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        var response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "canvas not found"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo(ErrorCode.API_003);
        assertThat(response.getBody().getTraceId()).isEqualTo("status-trace");
    }
}
