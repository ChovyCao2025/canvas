package org.chovy.canvas.common;

import lombok.Data;

/**
 * 通用接口响应包装。
 */
@Data
public class R<T> {

    /** 业务状态码：0=成功，非0=失败。 */
    private int code;

    /** 响应消息。 */
    private String message;

    /** 稳定错误码，成功响应为空。 */
    private String errorCode;

    /** 响应数据。 */
    private T data;

    /** 链路追踪 ID，用于错误排查时关联日志。 */
    private String traceId;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    /** 成功响应（含数据）。 */
    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        // code/message/data 三元组保持稳定，所有控制器都通过这里收敛成功响应格式。
        r.code = 0;
        r.message = "success";
        r.data = data;
        return r;
    }

    /** 成功响应（无数据体）。 */
    public static R<Void> ok() {
        return ok(null);
    }

    /** 失败响应。 */
    public static <T> R<T> fail(String message) {
        return fail(-1, message);
    }

    /** 失败响应（含业务状态码）。 */
    public static <T> R<T> fail(int code, String message) {
        R<T> r = new R<>();
        // 失败统一使用非 0 code；更细粒度错误码目前嵌入 message 前缀中透传。
        r.code = code;
        r.message = message;
        return r;
    }

    /** 失败响应（含稳定错误码和业务状态码）。 */
    public static <T> R<T> fail(String errorCode, int code, String message) {
        R<T> r = fail(code, message);
        r.errorCode = errorCode;
        return r;
    }

    /** 失败响应（含业务状态码和 traceId）。 */
    public static <T> R<T> fail(int code, String message, String traceId) {
        R<T> r = fail(code, message);
        r.traceId = traceId;
        return r;
    }

    /** 失败响应（含稳定错误码、业务状态码和 traceId）。 */
    public static <T> R<T> fail(String errorCode, int code, String message, String traceId) {
        R<T> r = fail(errorCode, code, message);
        r.traceId = traceId;
        return r;
    }
}
