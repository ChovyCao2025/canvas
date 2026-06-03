package org.chovy.canvas.config;

import org.chovy.canvas.common.R;
import org.chovy.canvas.engine.trigger.TriggerPreCheckService.TriggerRejectedException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

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

    private static final String TRACE_ID_MDC_KEY = "traceId";

    /** 参数校验/业务前置校验失败。 */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleIllegalArgument(IllegalArgumentException e) {
        // 参数和业务前置校验均归一为统一响应体，HTTP 语义由状态码表达。
        return R.fail(e.getMessage());
    }

    /** 业务状态冲突（如乐观锁冲突、重复发布等）。 */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public R<Void> handleIllegalState(IllegalStateException e) {
        // CANVAS_010 乐观锁冲突 → 409
        return R.fail(e.getMessage());
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
        return R.fail(e.getCode() + ": " + e.getMessage());
    }

    /** ResponseStatusException 直接透传其 HTTP 状态码和 reason */
    @ExceptionHandler(ResponseStatusException.class)
    public org.springframework.http.ResponseEntity<R<Void>> handleResponseStatus(
            ResponseStatusException e) {
        // 保留上游显式指定的 HTTP 状态码，只把响应体包装成前端统一格式。
        return org.springframework.http.ResponseEntity
                .status(e.getStatusCode())
                .body(R.fail(e.getReason() != null ? e.getReason() : e.getMessage()));
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
        return R.fail("AUTH_003: 无权限执行此操作");
    }

    /** 兜底异常处理，避免泄露堆栈给调用方。 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleGeneral(Exception e) {
        log.error("Unhandled server exception", e);
        // 兜底不透传内部异常消息，调用方可用 traceId 关联服务端日志排查。
        return R.fail(500, "系统错误", currentTraceId());
    }

    private String currentTraceId() {
        return Optional.ofNullable(MDC.get(TRACE_ID_MDC_KEY))
                .filter(traceId -> !traceId.isBlank())
                .orElseGet(() -> UUID.randomUUID().toString());
    }
}
