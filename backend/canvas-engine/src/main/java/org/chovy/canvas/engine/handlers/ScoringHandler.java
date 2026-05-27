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

/**
 * 评分节点处理器。
 *
 * <p>由 DAG 执行器在运行画布节点时调用，读取节点 config 与执行上下文，产出 NodeResult 决定后续路由。
 * <p>处理器应保持单节点职责，跨节点编排、重试和状态持久化由执行引擎统一管理。
 */
@Component
@NodeHandlerType(NodeType.SCORING)
public class ScoringHandler implements NodeHandler {
    /**
     * 执行当前节点或服务的核心处理流程。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        int score = 0;
        List<Map<String, Object>> rules = (List<Map<String, Object>>) config.getOrDefault("rules", List.of());
        for (Map<String, Object> rule : rules) {
            if (matches(rule, ctx)) {
                // 命中的规则累加分值，未命中规则不影响总分。
                score += number(rule.get("score"), 0);
            }
        }

        List<Map<String, Object>> bands = (List<Map<String, Object>>) config.getOrDefault("bands", List.of());
        Map<String, Object> selected = null;
        for (Map<String, Object> band : bands) {
            int min = number(band.get("min"), Integer.MIN_VALUE);
            int max = number(band.get("max"), Integer.MAX_VALUE);
            if (score >= min && score <= max) {
                // bands 按配置顺序命中第一档，允许运营用顺序处理重叠区间。
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

    /**
     * 判断 matches 相关的业务数据。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param rule rule 方法执行所需的业务参数
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 判断结果，true 表示校验通过或条件成立
     */
    private boolean matches(Map<String, Object> rule, ExecutionContext ctx) {
        Object actualValue = ctx.getContextValue(string(rule, "field", ""));
        if (actualValue == null) return false;
        String operator = string(rule, "operator", "EQUALS");
        Object expectedValue = rule.get("value");
        String actual = String.valueOf(actualValue);
        String expected = String.valueOf(expectedValue);
        // 数值比较失败会在 decimal 中降级为 0，避免单条脏规则抛异常中断评分。
        return switch (operator) {
            case "GREATER_THAN", "GT" -> decimal(actual).compareTo(decimal(expected)) > 0;
            case "LESS_THAN", "LT" -> decimal(actual).compareTo(decimal(expected)) < 0;
            case "CONTAINS" -> actual.contains(expected);
            case "IN" -> expected.contains(actual);
            default -> actual.equals(expected);
        };
    }

    /**
     * 执行 decimal 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 方法执行后的业务结果
     */
    private BigDecimal decimal(String value) {
        try {
            return new BigDecimal(value);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 执行 number 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param value value 待写入、比较或转换的业务值
     * @param fallback fallback 方法执行所需的业务参数
     * @return 计算得到的数值结果
     */
    private int number(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    /**
     * 执行 string 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @param key key 对应的缓存键、配置键或业务键
     * @param fallback fallback 方法执行所需的业务参数
     * @return 转换或查询得到的字符串结果
     */
    private String string(Map<String, Object> config, String key, String fallback) {
        Object value = config.get(key);
        return value == null ? fallback : value.toString();
    }
}
