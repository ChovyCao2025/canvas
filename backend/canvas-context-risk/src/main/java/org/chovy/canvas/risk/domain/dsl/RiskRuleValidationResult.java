package org.chovy.canvas.risk.domain.dsl;

import java.util.List;
import java.util.Objects;

/**
 * 风控规则校验结果。
 *
 * @param valid 是否通过校验
 * @param errors 校验错误列表
 */
public final class RiskRuleValidationResult {

    /**
     * RiskRuleValidationResult 的 valid 字段。
     */
    private final boolean valid;


    /**
     * RiskRuleValidationResult 的 errors 字段。
     */
    private final List<RiskValidationError> errors;


    /**
     * 创建 RiskRuleValidationResult。
     *
     * @param valid RiskRuleValidationResult 的 valid 字段
     * @param errors RiskRuleValidationResult 的 errors 字段
     */
    public RiskRuleValidationResult(boolean valid, List<RiskValidationError> errors) {
        errors = errors == null ? List.of() : List.copyOf(errors);
        this.valid = valid;
        this.errors = errors;
    }

    /**
     * 返回 RiskRuleValidationResult 的 valid 字段。
     *
     * @return valid 字段值
     */
    public boolean valid() {
        return valid;
    }

    /**
     * 返回 RiskRuleValidationResult 的 errors 字段。
     *
     * @return errors 字段值
     */
    public List<RiskValidationError> errors() {
        return errors;
    }

    /**
     * 比较当前 RiskRuleValidationResult 与其他对象是否相等。
     *
     * @param o 待比较对象
     * @return 相等时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskRuleValidationResult other)) {
            return false;
        }
        return valid == other.valid
                && Objects.equals(errors, other.errors);
    }

    /**
     * 计算 RiskRuleValidationResult 的哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(valid, errors);
    }

    /**
     * 返回 RiskRuleValidationResult 的调试字符串。
     *
     * @return 调试字符串
     */
    @Override
    public String toString() {
        return "RiskRuleValidationResult[valid=" + valid + ", errors=" + errors + "]";
    }
}
