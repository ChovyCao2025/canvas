package org.chovy.canvas.domain.risk.dsl;

/**
 * 风控规则解析和校验错误码。
 */
public enum RiskValidationErrorCode {
    /** JSON 结构非法。 */
    INVALID_JSON,
    /** 操作符未知。 */
    UNKNOWN_OPERATOR,
    /** 操作数类型未知。 */
    UNKNOWN_OPERAND_TYPE,
    /** 特征未注册。 */
    UNKNOWN_FEATURE,
    /** 特征仅离线可用。 */
    FEATURE_OFFLINE_ONLY,
    /** 名单未注册。 */
    UNKNOWN_LIST,
    /** 名单主体类型不匹配。 */
    LIST_SUBJECT_TYPE_MISMATCH,
    /** 规则嵌套深度超过限制。 */
    MAX_DEPTH_EXCEEDED,
    /** 条件数量超过限制。 */
    MAX_CONDITIONS_EXCEEDED,
    /** 左右值类型不兼容。 */
    TYPE_MISMATCH,
    /** 表达式不满足安全边界。 */
    UNSAFE_EXPRESSION
}
