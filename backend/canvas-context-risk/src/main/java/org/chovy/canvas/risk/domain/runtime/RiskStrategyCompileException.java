package org.chovy.canvas.risk.domain.runtime;

/**
 * 风控策略编译异常，携带错误码和定义路径。
 */
public class RiskStrategyCompileException extends RuntimeException {

    /**
     * 保存 code 对应的风控状态或配置。
     */
    private final RiskStrategyCompileErrorCode code;

    /**
     * 保存 path 对应的风控状态或配置。
     */
    private final String path;


    /**
     * 创建策略编译异常。
     */
    public RiskStrategyCompileException(RiskStrategyCompileErrorCode code, String path, String message) {
        super(code + " at " + path + ": " + message);
        this.code = code;
        this.path = path;
    }

    /**
     * 返回编译错误码。
     */
    public RiskStrategyCompileErrorCode code() {
        return code;
    }

    /**
     * 返回发生错误的策略定义路径。
     */
    public String path() {
        return path;
    }
}
