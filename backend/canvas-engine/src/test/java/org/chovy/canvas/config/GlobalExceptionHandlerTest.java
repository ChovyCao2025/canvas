package org.chovy.canvas.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import org.chovy.canvas.common.ErrorCode;
import org.chovy.canvas.common.R;
import org.chovy.canvas.dto.CanvasCreateReq;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;

import java.util.Set;
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
        assertThat(response.getErrorCode()).isEqualTo(ErrorCode.API_005);
        assertThat(response.getMessage()).isEqualTo("系统错误");
        assertThat(response.getTraceId()).isEqualTo("trace-123");
    }

    @Test
    void generalExceptionGeneratesTraceIdWhenMdcIsEmpty() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        R<Void> response = handler.handleGeneral(new RuntimeException("boom"));

        assertThat(response.getCode()).isEqualTo(500);
        assertThat(response.getErrorCode()).isEqualTo(ErrorCode.API_005);
        assertThat(response.getMessage()).isEqualTo("系统错误");
        assertThat(response.getTraceId()).isNotBlank();
        assertThatCodeIsUuid(response.getTraceId());
    }

    @Test
    void constraintViolationUsesStableValidationErrorCode() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        Set<ConstraintViolation<CanvasCreateReq>> violations = Validation
                .buildDefaultValidatorFactory()
                .getValidator()
                .validate(new CanvasCreateReq());

        R<Void> response = handler.handleConstraintViolation(new ConstraintViolationException(violations));

        assertThat(response.getCode()).isEqualTo(400);
        assertThat(response.getErrorCode()).isEqualTo(ErrorCode.API_001);
        assertThat(response.getMessage()).contains("name");
    }

    @Test
    void responseStatusUnauthorizedUsesAuthErrorCode() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        var response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "公开触发缺少签名头"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(401);
        assertThat(response.getBody().getErrorCode()).isEqualTo(ErrorCode.AUTH_002);
        assertThat(response.getBody().getMessage()).isEqualTo("公开触发缺少签名头");
    }

    @Test
    void malformedInputUsesStableMalformedJsonCode() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        R<Void> response = handler.handleServerWebInput(new ServerWebInputException("bad body"));

        assertThat(response.getCode()).isEqualTo(400);
        assertThat(response.getErrorCode()).isEqualTo(ErrorCode.API_002);
        assertThat(response.getMessage()).isEqualTo("请求体或参数格式不合法");
    }

    @Test
    void clientErrorMessagesMaskSensitiveValues() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        R<Void> response = handler.handleIllegalArgument(new IllegalArgumentException(
                "phone=13812345678 email=alice@example.com token=secret-token-123456"));

        assertThat(response.getMessage()).contains("138****5678");
        assertThat(response.getMessage()).contains("a***e@example.com");
        assertThat(response.getMessage()).contains("****3456");
        assertThat(response.getMessage()).doesNotContain("13812345678");
        assertThat(response.getMessage()).doesNotContain("alice@example.com");
        assertThat(response.getMessage()).doesNotContain("secret-token-123456");
    }

    private void assertThatCodeIsUuid(String traceId) {
        assertThat(UUID.fromString(traceId).toString()).isEqualTo(traceId);
    }
}
