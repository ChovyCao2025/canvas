package org.chovy.canvas.risk.domain.dsl;

import java.util.Objects;

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
    final class FeatureOperand implements RiskRuleOperand {

        /**
         * FeatureOperand 的 key 字段。
         */
        private final String key;


        /**
         * 创建 FeatureOperand。
         *
         * @param key FeatureOperand 的 key 字段
         */
        public FeatureOperand(String key) {
            this.key = key;
        }

        /**
         * 返回 FeatureOperand 的 key 字段。
         *
         * @return key 字段值
         */
        public String key() {
            return key;
        }

        /**
         * 比较当前 FeatureOperand 与其他对象是否相等。
         *
         * @param o 待比较对象
         * @return 相等时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof FeatureOperand other)) {
                return false;
            }
            return Objects.equals(key, other.key);
        }

        /**
         * 计算 FeatureOperand 的哈希值。
         *
         * @return 哈希值
         */
        @Override
        public int hashCode() {
            return Objects.hash(key);
        }

        /**
         * 返回 FeatureOperand 的调试字符串。
         *
         * @return 调试字符串
         */
        @Override
        public String toString() {
            return "FeatureOperand[key=" + key + "]";
        }

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
    final class LiteralOperand implements RiskRuleOperand {

        /**
         * LiteralOperand 的 value 字段。
         */
        private final Object value;


        /**
         * 创建 LiteralOperand。
         *
         * @param value LiteralOperand 的 value 字段
         */
        public LiteralOperand(Object value) {
            this.value = value;
        }

        /**
         * 返回 LiteralOperand 的 value 字段。
         *
         * @return value 字段值
         */
        public Object value() {
            return value;
        }

        /**
         * 比较当前 LiteralOperand 与其他对象是否相等。
         *
         * @param o 待比较对象
         * @return 相等时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof LiteralOperand other)) {
                return false;
            }
            return Objects.equals(value, other.value);
        }

        /**
         * 计算 LiteralOperand 的哈希值。
         *
         * @return 哈希值
         */
        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        /**
         * 返回 LiteralOperand 的调试字符串。
         *
         * @return 调试字符串
         */
        @Override
        public String toString() {
            return "LiteralOperand[value=" + value + "]";
        }

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
    final class ListOperand implements RiskRuleOperand {

        /**
         * ListOperand 的 key 字段。
         */
        private final String key;


        /**
         * 创建 ListOperand。
         *
         * @param key ListOperand 的 key 字段
         */
        public ListOperand(String key) {
            this.key = key;
        }

        /**
         * 返回 ListOperand 的 key 字段。
         *
         * @return key 字段值
         */
        public String key() {
            return key;
        }

        /**
         * 比较当前 ListOperand 与其他对象是否相等。
         *
         * @param o 待比较对象
         * @return 相等时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ListOperand other)) {
                return false;
            }
            return Objects.equals(key, other.key);
        }

        /**
         * 计算 ListOperand 的哈希值。
         *
         * @return 哈希值
         */
        @Override
        public int hashCode() {
            return Objects.hash(key);
        }

        /**
         * 返回 ListOperand 的调试字符串。
         *
         * @return 调试字符串
         */
        @Override
        public String toString() {
            return "ListOperand[key=" + key + "]";
        }

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
    final class ContextOperand implements RiskRuleOperand {

        /**
         * ContextOperand 的 path 字段。
         */
        private final String path;


        /**
         * 创建 ContextOperand。
         *
         * @param path ContextOperand 的 path 字段
         */
        public ContextOperand(String path) {
            this.path = path;
        }

        /**
         * 返回 ContextOperand 的 path 字段。
         *
         * @return path 字段值
         */
        public String path() {
            return path;
        }

        /**
         * 比较当前 ContextOperand 与其他对象是否相等。
         *
         * @param o 待比较对象
         * @return 相等时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ContextOperand other)) {
                return false;
            }
            return Objects.equals(path, other.path);
        }

        /**
         * 计算 ContextOperand 的哈希值。
         *
         * @return 哈希值
         */
        @Override
        public int hashCode() {
            return Objects.hash(path);
        }

        /**
         * 返回 ContextOperand 的调试字符串。
         *
         * @return 调试字符串
         */
        @Override
        public String toString() {
            return "ContextOperand[path=" + path + "]";
        }

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
    final class EventOperand implements RiskRuleOperand {

        /**
         * EventOperand 的 path 字段。
         */
        private final String path;


        /**
         * 创建 EventOperand。
         *
         * @param path EventOperand 的 path 字段
         */
        public EventOperand(String path) {
            this.path = path;
        }

        /**
         * 返回 EventOperand 的 path 字段。
         *
         * @return path 字段值
         */
        public String path() {
            return path;
        }

        /**
         * 比较当前 EventOperand 与其他对象是否相等。
         *
         * @param o 待比较对象
         * @return 相等时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof EventOperand other)) {
                return false;
            }
            return Objects.equals(path, other.path);
        }

        /**
         * 计算 EventOperand 的哈希值。
         *
         * @return 哈希值
         */
        @Override
        public int hashCode() {
            return Objects.hash(path);
        }

        /**
         * 返回 EventOperand 的调试字符串。
         *
         * @return 调试字符串
         */
        @Override
        public String toString() {
            return "EventOperand[path=" + path + "]";
        }

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
    final class SubjectOperand implements RiskRuleOperand {

        /**
         * SubjectOperand 的 path 字段。
         */
        private final String path;


        /**
         * 创建 SubjectOperand。
         *
         * @param path SubjectOperand 的 path 字段
         */
        public SubjectOperand(String path) {
            this.path = path;
        }

        /**
         * 返回 SubjectOperand 的 path 字段。
         *
         * @return path 字段值
         */
        public String path() {
            return path;
        }

        /**
         * 比较当前 SubjectOperand 与其他对象是否相等。
         *
         * @param o 待比较对象
         * @return 相等时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof SubjectOperand other)) {
                return false;
            }
            return Objects.equals(path, other.path);
        }

        /**
         * 计算 SubjectOperand 的哈希值。
         *
         * @return 哈希值
         */
        @Override
        public int hashCode() {
            return Objects.hash(path);
        }

        /**
         * 返回 SubjectOperand 的调试字符串。
         *
         * @return 调试字符串
         */
        @Override
        public String toString() {
            return "SubjectOperand[path=" + path + "]";
        }

        /**
                 * 返回主体操作数类型。
                 */
                @Override
                public RiskOperandType type() {
                    return RiskOperandType.SUBJECT;
                }
    }
}
