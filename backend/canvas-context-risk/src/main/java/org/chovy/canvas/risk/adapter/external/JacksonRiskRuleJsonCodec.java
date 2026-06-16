package org.chovy.canvas.risk.adapter.external;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.chovy.canvas.risk.domain.dsl.RiskRuleJsonCodec;
import org.chovy.canvas.risk.domain.dsl.RiskRuleJsonNode;
import org.chovy.canvas.risk.domain.dsl.RiskRuleParseException;
import org.chovy.canvas.risk.domain.dsl.RiskValidationErrorCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 定义 JacksonRiskRuleJsonCodec 的风控模块职责和数据契约。
 */
@Component
public class JacksonRiskRuleJsonCodec implements RiskRuleJsonCodec {

    /**
     * 保存 objectMapper 对应的风控状态或配置。
     */
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public JacksonRiskRuleJsonCodec() {
        this(new com.fasterxml.jackson.databind.ObjectMapper()
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true));
    }

    JacksonRiskRuleJsonCodec(com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 执行 readTree 相关的风控处理逻辑。
     */
    @Override
    public RiskRuleJsonNode readTree(String json) {
        try {
            return wrap(objectMapper.readTree(json));
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            throw new RiskRuleParseException(RiskValidationErrorCode.INVALID_JSON, "$", "invalid rule JSON");
        }
    }

    /**
     * 执行 writeCanonical 相关的风控处理逻辑。
     */
    @Override
    public String writeCanonical(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("risk JSON could not be canonicalized", ex);
        }
    }

    /**
     * 执行 wrap 相关的风控处理逻辑。
     */
    private static RiskRuleJsonNode wrap(JsonNode node) {
        return new JacksonRiskRuleJsonNode(node == null ? MissingNode.getInstance() : node);
    }

    /**
     * 定义 JacksonRiskRuleJsonNode 的风控模块职责和数据契约。
     */
    private static final class JacksonRiskRuleJsonNode implements RiskRuleJsonNode {

        /**
         * JacksonRiskRuleJsonNode 的 delegate 字段。
         */
        private final JsonNode delegate;


        /**
         * 创建 JacksonRiskRuleJsonNode。
         *
         * @param delegate JacksonRiskRuleJsonNode 的 delegate 字段
         */
        public JacksonRiskRuleJsonNode(JsonNode delegate) {
            this.delegate = delegate;
        }

        /**
         * 返回 JacksonRiskRuleJsonNode 的 delegate 字段。
         *
         * @return delegate 字段值
         */
        public JsonNode delegate() {
            return delegate;
        }

        /**
         * 比较当前 JacksonRiskRuleJsonNode 与其他对象是否相等。
         *
         * @param o 待比较对象
         * @return 相等时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof JacksonRiskRuleJsonNode other)) {
                return false;
            }
            return Objects.equals(delegate, other.delegate);
        }

        /**
         * 计算 JacksonRiskRuleJsonNode 的哈希值。
         *
         * @return 哈希值
         */
        @Override
        public int hashCode() {
            return Objects.hash(delegate);
        }

        /**
         * 返回 JacksonRiskRuleJsonNode 的调试字符串。
         *
         * @return 调试字符串
         */
        @Override
        public String toString() {
            return "JacksonRiskRuleJsonNode[delegate=" + delegate + "]";
        }

                /**
                 * 执行 isMissing 相关的风控处理逻辑。
                 */
        @Override
                public boolean isMissing() {
                    return delegate.isMissingNode();
                }

                /**
                 * 执行 isNull 相关的风控处理逻辑。
                 */
                @Override
                public boolean isNull() {
                    return delegate.isNull();
                }

                /**
                 * 执行 isObject 相关的风控处理逻辑。
                 */
                @Override
                public boolean isObject() {
                    return delegate.isObject();
                }

                /**
                 * 执行 isArray 相关的风控处理逻辑。
                 */
                @Override
                public boolean isArray() {
                    return delegate.isArray();
                }

                /**
                 * 执行 isValue 相关的风控处理逻辑。
                 */
                @Override
                public boolean isValue() {
                    return delegate.isValueNode();
                }

                /**
                 * 执行 isBoolean 相关的风控处理逻辑。
                 */
                @Override
                public boolean isBoolean() {
                    return delegate.isBoolean();
                }

                /**
                 * 执行 isIntegralNumber 相关的风控处理逻辑。
                 */
                @Override
                public boolean isIntegralNumber() {
                    return delegate.isIntegralNumber();
                }

                /**
                 * 执行 isDecimalNumber 相关的风控处理逻辑。
                 */
                @Override
                public boolean isDecimalNumber() {
                    return delegate.isFloatingPointNumber() || delegate.isBigDecimal();
                }

                /**
                 * 执行 isText 相关的风控处理逻辑。
                 */
                @Override
                public boolean isText() {
                    return delegate.isTextual();
                }

                /**
                 * 执行 booleanValue 相关的风控处理逻辑。
                 */
                @Override
                public boolean booleanValue() {
                    return delegate.booleanValue();
                }

                /**
                 * 执行 longValue 相关的风控处理逻辑。
                 */
                @Override
                public long longValue() {
                    return delegate.longValue();
                }

                /**
                 * 执行 decimalValue 相关的风控处理逻辑。
                 */
                @Override
                public BigDecimal decimalValue() {
                    return delegate.decimalValue();
                }

                /**
                 * 执行 textValue 相关的风控处理逻辑。
                 */
                @Override
                public String textValue() {
                    return delegate.textValue();
                }

                /**
                 * 执行 value 相关的风控处理逻辑。
                 */
                @Override
                public Object value() {
                    if (delegate.isNull() || delegate.isMissingNode()) {
                        return null;
                    }
                    if (delegate.isBoolean()) {
                        return delegate.booleanValue();
                    }
                    if (delegate.isIntegralNumber()) {
                        return delegate.longValue();
                    }
                    if (delegate.isFloatingPointNumber() || delegate.isBigDecimal()) {
                        return delegate.decimalValue();
                    }
                    if (delegate.isTextual()) {
                        return delegate.textValue();
                    }
                    throw new IllegalStateException("JSON node is not a scalar value");
                }

                /**
                 * 执行 get 相关的风控处理逻辑。
                 */
                @Override
                public RiskRuleJsonNode get(String field) {
                    return wrap(delegate.get(field));
                }

                /**
                 * 执行 path 相关的风控处理逻辑。
                 */
                @Override
                public RiskRuleJsonNode path(String field) {
                    return wrap(delegate.path(field));
                }

                /**
                 * 执行 get 相关的风控处理逻辑。
                 */
                @Override
                public RiskRuleJsonNode get(int index) {
                    return wrap(delegate.get(index));
                }

                /**
                 * 执行 size 相关的风控处理逻辑。
                 */
                @Override
                public int size() {
                    return delegate.size();
                }

                /**
                 * 执行 elements 相关的风控处理逻辑。
                 */
                @Override
                public List<RiskRuleJsonNode> elements() {
                    List<RiskRuleJsonNode> elements = new ArrayList<>();
                    delegate.elements().forEachRemaining(item -> elements.add(wrap(item)));
                    return List.copyOf(elements);
                }

                /**
                 * 执行 fieldNames 相关的风控处理逻辑。
                 */
                @Override
                public List<String> fieldNames() {
                    List<String> fields = new ArrayList<>();
                    delegate.fieldNames().forEachRemaining(fields::add);
                    return List.copyOf(fields);
                }
    }
}
