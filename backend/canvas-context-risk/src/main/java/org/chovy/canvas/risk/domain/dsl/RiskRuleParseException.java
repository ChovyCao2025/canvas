package org.chovy.canvas.risk.domain.dsl;

/**
 * 风控规则 DSL 解析异常，携带错误码和 JSON 路径。
 */
public final class RiskRuleParseException extends RuntimeException {

    /**
     * 保存 code 对应的风控状态或配置。
     */
    private final RiskValidationErrorCode code;

    /**
     * 保存 path 对应的风控状态或配置。
     */
    private final String path;


    /**
     * 创建解析异常。
     */
    public RiskRuleParseException(RiskValidationErrorCode code, String path, String message) {
        super(code + " at " + path + ": " + message);
        this.code = code;
        this.path = path;
    }

    /**
     * 返回解析错误码。
     */
    public RiskValidationErrorCode code() {
        return code;
    }

    /**
     * 返回发生错误的 DSL 路径。
     */
    public String path() {
        return path;
    }
}
