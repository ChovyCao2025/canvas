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
 * QLExpress 兼容入口的人群规则求值器。
 *
 * <p>保留 engineType=QL 的路由名称，实际求值委托统一规则 AST，避免不同引擎语义漂移。
 */
@Slf4j
@Component("QL")
@RequiredArgsConstructor
public class QLExpressRuleEvaluator implements RuleEvaluator {

    /** 规则 JSON 解析器。 */
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, RuleGroup> ruleCache = new ConcurrentHashMap<>();

    /**
     * 使用 QLExpress 执行人群规则 JSON。
     *
     * <p>与 Aviator 使用同一套规则结构，异常统一按未命中处理。
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
            // 与 Aviator 保持一致：异常降级为 false
            log.error("[AUDIENCE][QL] evaluate failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 解析并缓存人群规则 AST。
     *
     * <p>QL 兼容入口与 Aviator 共用统一 AST，缓存可减少批量判断时的重复解析。
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
