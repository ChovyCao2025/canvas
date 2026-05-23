package org.chovy.canvas.config;

import org.chovy.canvas.common.R;
import org.chovy.canvas.engine.trigger.TriggerPreCheckService.TriggerRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * 全局异常处理（设计文档第二十二章 22.1 节统一错误响应格式）。
 *
 * 响应结构：{ "code": "CANVAS_001", "message": "...", "traceId": "..." }
 *
 * 设计原则：
 * - HTTP 状态码表达错误类别（4xx/5xx）；
 * - 业务语义放在 code/message，便于前端做稳定分支处理。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 参数校验/业务前置校验失败。 */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleIllegalArgument(IllegalArgumentException e) {
        return R.fail(e.getMessage());
    }

    /** 业务状态冲突（如乐观锁冲突、重复发布等）。 */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public R<Void> handleIllegalState(IllegalStateException e) {
        // CANVAS_010 乐观锁冲突 → 409
        return R.fail(e.getMessage());
    }

    @ExceptionHandler(TriggerRejectedException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public R<Void> handleTriggerRejected(TriggerRejectedException e) {
        // 透传 QUOTA_xxx 错误码，前端可按 code 做文案映射
        return R.fail(e.getCode() + ": " + e.getMessage());
    }

    /** ResponseStatusException 直接透传其 HTTP 状态码和 reason */
    @ExceptionHandler(ResponseStatusException.class)
    public org.springframework.http.ResponseEntity<R<Void>> handleResponseStatus(
            ResponseStatusException e) {
        return org.springframework.http.ResponseEntity
                .status(e.getStatusCode())
                .body(R.fail(e.getReason() != null ? e.getReason() : e.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<Void> handleForbidden(SecurityException e) {
        // 统一返回 AUTH_003，避免暴露过多授权细节
        return R.fail("AUTH_003: 无权限执行此操作");
    }

    /** 兜底异常处理，避免泄露堆栈给调用方。 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleGeneral(Exception e) {
        return R.fail("系统错误: " + e.getMessage());
    }
}
