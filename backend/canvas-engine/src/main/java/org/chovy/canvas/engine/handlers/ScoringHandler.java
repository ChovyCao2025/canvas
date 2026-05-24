package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
@NodeHandlerType(NodeType.SCORING)
public class ScoringHandler implements NodeHandler {
    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        int score = 0;
        List<Map<String, Object>> rules = (List<Map<String, Object>>) config.getOrDefault("rules", List.of());
        for (Map<String, Object> rule : rules) {
            if (matches(rule, ctx)) {
                score += number(rule.get("score"), 0);
            }
        }

        List<Map<String, Object>> bands = (List<Map<String, Object>>) config.getOrDefault("bands", List.of());
        Map<String, Object> selected = null;
        for (Map<String, Object> band : bands) {
            int min = number(band.get("min"), Integer.MIN_VALUE);
            int max = number(band.get("max"), Integer.MAX_VALUE);
            if (score >= min && score <= max) {
                selected = band;
                break;
            }
        }
        if (selected == null) {
            return Mono.just(NodeResult.ok(string(config, "nextNodeId", null), Map.of(MapFieldKeys.SCORE, score)));
        }
        String bandId = string(selected, "bandId", string(selected, "id", "band"));
        return Mono.just(NodeResult.routed(bandId, string(selected, "nextNodeId", null),
                Map.of(MapFieldKeys.SCORE, score, MapFieldKeys.SCORE_BAND, bandId)));
    }

    private boolean matches(Map<String, Object> rule, ExecutionContext ctx) {
        Object actualValue = ctx.getContextValue(string(rule, "field", ""));
        if (actualValue == null) return false;
        String operator = string(rule, "operator", "EQUALS");
        Object expectedValue = rule.get("value");
        String actual = String.valueOf(actualValue);
        String expected = String.valueOf(expectedValue);
        return switch (operator) {
            case "GREATER_THAN", "GT" -> decimal(actual).compareTo(decimal(expected)) > 0;
            case "LESS_THAN", "LT" -> decimal(actual).compareTo(decimal(expected)) < 0;
            case "CONTAINS" -> actual.contains(expected);
            case "IN" -> expected.contains(actual);
            default -> actual.equals(expected);
        };
    }

    private BigDecimal decimal(String value) {
        try {
            return new BigDecimal(value);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private int number(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private String string(Map<String, Object> config, String key, String fallback) {
        Object value = config.get(key);
        return value == null ? fallback : value.toString();
    }
}
