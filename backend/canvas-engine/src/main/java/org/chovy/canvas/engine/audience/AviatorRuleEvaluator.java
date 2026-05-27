package org.chovy.canvas.engine.audience;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.engine.rule.RuleAstEvaluator;
import org.chovy.canvas.engine.rule.RuleGroup;
import org.chovy.canvas.engine.rule.RuleParser;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Aviator 兼容入口的人群规则求值器。
 *
 * <p>保留 engineType=AVIATOR 的路由名称，实际求值委托统一规则 AST，避免不同引擎语义漂移。
 */
@Slf4j
@Component("AVIATOR")
@RequiredArgsConstructor
public class AviatorRuleEvaluator implements RuleEvaluator {

    /** 规则 JSON 解析器。 */
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, RuleGroup> ruleCache = new ConcurrentHashMap<>();

    /**
     * 使用 Aviator 执行人群规则 JSON。
     *
     * <p>规则树会先转换成表达式，异常统一降级为未命中，避免单条规则拖垮触发链路。
     *
     * @param ruleJson 人群规则 JSON
     * @param context 用户画像、事件属性等求值上下文
     * @return {@code true} 表示规则命中
     */
    @Override
    public boolean evaluate(String ruleJson, Map<String, Object> context) {
        try {
            return RuleAstEvaluator.matches(parseCached(ruleJson), context::get);
        } catch (Exception e) {
            // 引擎异常统一按“未命中”处理，避免影响整体流程稳定性
            log.error("[AUDIENCE][AVIATOR] evaluate failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 解析并缓存人群规则 AST。
     *
     * <p>同一规则 JSON 在批量判断中会被反复使用，缓存 AST 可避免重复解析。
     *
     * @param ruleJson 人群规则 JSON
     * @return 可直接用于求值的规则分组 AST
     */
    private RuleGroup parseCached(String ruleJson) {
        String cacheKey = ruleJson == null ? "" : ruleJson;
        return ruleCache.computeIfAbsent(cacheKey, key -> {
            try {
                return new RuleParser(objectMapper).parseAudienceJson(key);
            } catch (Exception e) {
                throw new IllegalArgumentException("规则 JSON 解析失败: " + e.getMessage(), e);
            }
        });
    }
}
