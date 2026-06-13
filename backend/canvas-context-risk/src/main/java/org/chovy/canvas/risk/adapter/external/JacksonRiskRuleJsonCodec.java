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

@Component
public class JacksonRiskRuleJsonCodec implements RiskRuleJsonCodec {

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public JacksonRiskRuleJsonCodec() {
        this(new com.fasterxml.jackson.databind.ObjectMapper()
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true));
    }

    JacksonRiskRuleJsonCodec(com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public RiskRuleJsonNode readTree(String json) {
        try {
            return wrap(objectMapper.readTree(json));
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            throw new RiskRuleParseException(RiskValidationErrorCode.INVALID_JSON, "$", "invalid rule JSON");
        }
    }

    @Override
    public String writeCanonical(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("risk JSON could not be canonicalized", ex);
        }
    }

    private static RiskRuleJsonNode wrap(JsonNode node) {
        return new JacksonRiskRuleJsonNode(node == null ? MissingNode.getInstance() : node);
    }

    private record JacksonRiskRuleJsonNode(JsonNode delegate) implements RiskRuleJsonNode {

        @Override
        public boolean isMissing() {
            return delegate.isMissingNode();
        }

        @Override
        public boolean isNull() {
            return delegate.isNull();
        }

        @Override
        public boolean isObject() {
            return delegate.isObject();
        }

        @Override
        public boolean isArray() {
            return delegate.isArray();
        }

        @Override
        public boolean isValue() {
            return delegate.isValueNode();
        }

        @Override
        public boolean isBoolean() {
            return delegate.isBoolean();
        }

        @Override
        public boolean isIntegralNumber() {
            return delegate.isIntegralNumber();
        }

        @Override
        public boolean isDecimalNumber() {
            return delegate.isFloatingPointNumber() || delegate.isBigDecimal();
        }

        @Override
        public boolean isText() {
            return delegate.isTextual();
        }

        @Override
        public boolean booleanValue() {
            return delegate.booleanValue();
        }

        @Override
        public long longValue() {
            return delegate.longValue();
        }

        @Override
        public BigDecimal decimalValue() {
            return delegate.decimalValue();
        }

        @Override
        public String textValue() {
            return delegate.textValue();
        }

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

        @Override
        public RiskRuleJsonNode get(String field) {
            return wrap(delegate.get(field));
        }

        @Override
        public RiskRuleJsonNode path(String field) {
            return wrap(delegate.path(field));
        }

        @Override
        public RiskRuleJsonNode get(int index) {
            return wrap(delegate.get(index));
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public List<RiskRuleJsonNode> elements() {
            List<RiskRuleJsonNode> elements = new ArrayList<>();
            delegate.elements().forEachRemaining(item -> elements.add(wrap(item)));
            return List.copyOf(elements);
        }

        @Override
        public List<String> fieldNames() {
            List<String> fields = new ArrayList<>();
            delegate.fieldNames().forEachRemaining(fields::add);
            return List.copyOf(fields);
        }
    }
}
