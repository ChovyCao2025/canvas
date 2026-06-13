package org.chovy.canvas.risk.domain.dsl;

/**
 * 风控规则操作数封闭类型，区分可信字面量、请求字段、上下文字段、特征和名单引用。
 */
public sealed interface RiskRuleOperand
        permits RiskRuleOperand.FeatureOperand,
                RiskRuleOperand.LiteralOperand,
                RiskRuleOperand.ListOperand,
                RiskRuleOperand.ContextOperand,
                RiskRuleOperand.EventOperand,
                RiskRuleOperand.SubjectOperand {

    /**
     * 返回操作数来源类型。
     */
    RiskOperandType type();

    /**
     * 创建特征操作数。
     */
    static FeatureOperand feature(String key) {
        return new FeatureOperand(key);
    }

    /**
     * 创建字面量操作数。
     */
    static LiteralOperand literal(Object value) {
        return new LiteralOperand(value);
    }

    /**
     * 创建名单操作数。
     */
    static ListOperand list(String key) {
        return new ListOperand(key);
    }

    /**
     * 创建上下文路径操作数。
     */
    static ContextOperand context(String path) {
        return new ContextOperand(path);
    }

    /**
     * 创建事件路径操作数。
     */
    static EventOperand event(String path) {
        return new EventOperand(path);
    }

    /**
     * 创建主体路径操作数。
     */
    static SubjectOperand subject(String path) {
        return new SubjectOperand(path);
    }

    /**
     * 特征操作数。
     *
     * @param key 特征业务键
     */
    record FeatureOperand(String key) implements RiskRuleOperand {
        /**
         * 返回特征操作数类型。
         */
        @Override
        public RiskOperandType type() {
            return RiskOperandType.FEATURE;
        }
    }

    /**
     * 字面量操作数。
     *
     * @param value 字面量值
     */
    record LiteralOperand(Object value) implements RiskRuleOperand {
        /**
         * 返回字面量操作数类型。
         */
        @Override
        public RiskOperandType type() {
            return RiskOperandType.LITERAL;
        }
    }

    /**
     * 名单引用操作数。
     *
     * @param key 名单业务键
     */
    record ListOperand(String key) implements RiskRuleOperand {
        /**
         * 返回名单操作数类型。
         */
        @Override
        public RiskOperandType type() {
            return RiskOperandType.LIST;
        }
    }

    /**
     * 上下文路径操作数。
     *
     * @param path 上下文字段路径
     */
    record ContextOperand(String path) implements RiskRuleOperand {
        /**
         * 返回上下文操作数类型。
         */
        @Override
        public RiskOperandType type() {
            return RiskOperandType.CONTEXT;
        }
    }

    /**
     * 事件路径操作数。
     *
     * @param path 事件字段路径
     */
    record EventOperand(String path) implements RiskRuleOperand {
        /**
         * 返回事件操作数类型。
         */
        @Override
        public RiskOperandType type() {
            return RiskOperandType.EVENT;
        }
    }

    /**
     * 主体路径操作数。
     *
     * @param path 主体字段路径
     */
    record SubjectOperand(String path) implements RiskRuleOperand {
        /**
         * 返回主体操作数类型。
         */
        @Override
        public RiskOperandType type() {
            return RiskOperandType.SUBJECT;
        }
    }
}
