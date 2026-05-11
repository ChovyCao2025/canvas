package com.photon.canvas.config;

import com.photon.canvas.common.R;
import com.photon.canvas.engine.trigger.TriggerPreCheckService.TriggerRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理（设计文档第二十二章 22.1 节统一错误响应格式）。
 *
 * 响应结构：{ "code": "CANVAS_001", "message": "...", "traceId": "..." }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleIllegalArgument(IllegalArgumentException e) {
        return R.fail(e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public R<Void> handleIllegalState(IllegalStateException e) {
        // CANVAS_010 乐观锁冲突 → 409
        return R.fail(e.getMessage());
    }

    @ExceptionHandler(TriggerRejectedException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public R<Void> handleTriggerRejected(TriggerRejectedException e) {
        return R.fail(e.getCode() + ": " + e.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<Void> handleForbidden(SecurityException e) {
        return R.fail("AUTH_003: 无权限执行此操作");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleGeneral(Exception e) {
        return R.fail("系统错误: " + e.getMessage());
    }
}
