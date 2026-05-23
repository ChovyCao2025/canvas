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

    /** 响应数据。 */
    private T data;

    /** 成功响应（含数据）。 */
    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
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
        R<T> r = new R<>();
        r.code = -1;
        r.message = message;
        return r;
    }
}
