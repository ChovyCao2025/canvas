package org.chovy.canvas.config;

import jakarta.validation.ConstraintViolationException;
import org.chovy.canvas.common.ErrorCode;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.compliance.PiiMaskingService;
import org.chovy.canvas.engine.trigger.TriggerPreCheckService.TriggerRejectedException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 全局异常处理（设计文档第二十二章 22.1 节统一错误响应格式）。
 *
 * 响应结构：{ "code": "CANVAS_001", "message": "...", "traceId": "..." }
 *
 * 设计原则：
 * - HTTP 状态码表达错误类别（4xx/5xx）；
 * - 业务语义放在 code/message，便于前端做稳定分支处理。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String TRACE_ID_MDC_KEY = CorrelationIdWebFilter.MDC_KEY;
    private final PiiMaskingService maskingService;

    public GlobalExceptionHandler() {
        this(new PiiMaskingService());
    }

    @Autowired
    public GlobalExceptionHandler(PiiMaskingService maskingService) {
        this.maskingService = maskingService;
    }

    /** 参数校验/业务前置校验失败。 */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleIllegalArgument(IllegalArgumentException e) {
        // 参数和业务前置校验均归一为统一响应体，HTTP 语义由状态码表达。
        return fail(ErrorCode.API_001, HttpStatus.BAD_REQUEST.value(), e.getMessage());
    }

    /** 业务状态冲突（如乐观锁冲突、重复发布等）。 */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public R<Void> handleIllegalState(IllegalStateException e) {
        // CANVAS_010 乐观锁冲突 → 409
        return fail(ErrorCode.API_004, HttpStatus.CONFLICT.value(), e.getMessage());
    }

    /** Bean Validation 绑定失败。 */
    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleBindException(WebExchangeBindException e) {
        return fail(ErrorCode.API_001, HttpStatus.BAD_REQUEST.value(), validationMessage(e));
    }

    /** 方法参数或 raw-body 程序化校验失败。 */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return fail(ErrorCode.API_001, HttpStatus.BAD_REQUEST.value(), message);
    }

    /** JSON 格式、类型转换等输入解析错误。 */
    @ExceptionHandler(ServerWebInputException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleServerWebInput(ServerWebInputException e) {
        return fail(ErrorCode.API_002, HttpStatus.BAD_REQUEST.value(), "请求体或参数格式不合法");
    }

    /**
     * 执行 handle Trigger Rejected 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param e e 方法执行所需的业务参数
     * @return 接口响应包装结果
     */
    @ExceptionHandler(TriggerRejectedException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public R<Void> handleTriggerRejected(TriggerRejectedException e) {
        // 透传 QUOTA_xxx 错误码，前端可按 code 做文案映射
        return fail(e.getCode(), HttpStatus.TOO_MANY_REQUESTS.value(), e.getCode() + ": " + e.getMessage());
    }

    /** ResponseStatusException 直接透传其 HTTP 状态码和 reason */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<R<Void>> handleResponseStatus(ResponseStatusException e) {
        // 保留上游显式指定的 HTTP 状态码，只把响应体包装成前端统一格式。
        int status = e.getStatusCode().value();
        return ResponseEntity
                .status(e.getStatusCode())
                .body(fail(errorCodeFor(status), status, e.getReason() != null ? e.getReason() : e.getMessage()));
    }

    /**
     * 执行 handle Forbidden 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param e e 方法执行所需的业务参数
     * @return 接口响应包装结果
     */
    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<Void> handleForbidden(SecurityException e) {
        // 统一返回 AUTH_003，避免暴露过多授权细节
        return fail(ErrorCode.AUTH_003, HttpStatus.FORBIDDEN.value(), "无权限执行此操作");
    }

    /** Spring Security 授权拒绝。 */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<Void> handleAccessDenied(AccessDeniedException e) {
        return fail(ErrorCode.AUTH_003, HttpStatus.FORBIDDEN.value(), "无权限执行此操作");
    }

    /** 兜底异常处理，避免泄露堆栈给调用方。 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleGeneral(Exception e) {
        log.error("Unhandled server exception", e);
        // 兜底不透传内部异常消息，调用方可用 traceId 关联服务端日志排查。
        return R.fail(ErrorCode.API_005, HttpStatus.INTERNAL_SERVER_ERROR.value(), "系统错误", currentTraceId());
    }

    private R<Void> fail(String errorCode, int code, String message) {
        String safeMessage = maskingService.maskText(message);
        return optionalTraceId()
                .map(traceId -> R.<Void>fail(errorCode, code, safeMessage, traceId))
                .orElseGet(() -> R.fail(errorCode, code, safeMessage));
    }

    private String errorCodeFor(int status) {
        return switch (status) {
            case 400 -> ErrorCode.API_001;
            case 401 -> ErrorCode.AUTH_002;
            case 403 -> ErrorCode.AUTH_003;
            case 404 -> ErrorCode.API_003;
            case 409 -> ErrorCode.API_004;
            default -> status >= 500 ? ErrorCode.API_005 : ErrorCode.API_001;
        };
    }

    private String validationMessage(WebExchangeBindException e) {
        String message = e.getFieldErrors().stream()
                .map(field -> field.getField() + " " + field.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        if (!message.isBlank()) {
            return message;
        }
        return "请求参数校验失败";
    }

    private Optional<String> optionalTraceId() {
        return Optional.ofNullable(MDC.get(TRACE_ID_MDC_KEY))
                .filter(traceId -> !traceId.isBlank());
    }

    private String currentTraceId() {
        return optionalTraceId()
                .orElseGet(() -> UUID.randomUUID().toString());
    }
}
