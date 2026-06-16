package org.chovy.canvas.risk.domain.dsl;

import java.util.Objects;

/**
 * 风控规则校验错误。
 *
 * @param path 错误所在 DSL 路径
 * @param code 错误码
 * @param message 错误说明
 */
public final class RiskValidationError {

    /**
     * RiskValidationError 的 path 字段。
     */
    private final String path;


    /**
     * RiskValidationError 的 code 字段。
     */
    private final RiskValidationErrorCode code;


    /**
     * RiskValidationError 的 message 字段。
     */
    private final String message;


    /**
     * 创建 RiskValidationError。
     *
     * @param path RiskValidationError 的 path 字段
     * @param code RiskValidationError 的 code 字段
     * @param message RiskValidationError 的 message 字段
     */
    public RiskValidationError(String path, RiskValidationErrorCode code, String message) {
        this.path = path;
        this.code = code;
        this.message = message;
    }

    /**
     * 返回 RiskValidationError 的 path 字段。
     *
     * @return path 字段值
     */
    public String path() {
        return path;
    }

    /**
     * 返回 RiskValidationError 的 code 字段。
     *
     * @return code 字段值
     */
    public RiskValidationErrorCode code() {
        return code;
    }

    /**
     * 返回 RiskValidationError 的 message 字段。
     *
     * @return message 字段值
     */
    public String message() {
        return message;
    }

    /**
     * 比较当前 RiskValidationError 与其他对象是否相等。
     *
     * @param o 待比较对象
     * @return 相等时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskValidationError other)) {
            return false;
        }
        return Objects.equals(path, other.path)
                && Objects.equals(code, other.code)
                && Objects.equals(message, other.message);
    }

    /**
     * 计算 RiskValidationError 的哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(path, code, message);
    }

    /**
     * 返回 RiskValidationError 的调试字符串。
     *
     * @return 调试字符串
     */
    @Override
    public String toString() {
        return "RiskValidationError[path=" + path + ", code=" + code + ", message=" + message + "]";
    }
}
