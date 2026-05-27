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
        R<T> r = new R<>();
        // 失败统一使用非 0 code；更细粒度错误码目前嵌入 message 前缀中透传。
        r.code = -1;
        r.message = message;
        return r;
    }
}
