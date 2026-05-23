package org.chovy.canvas.engine.audience;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 规则引擎路由器：按 engineType 选择对应求值器。
 *
 * <p>engineType 未匹配时默认回退到 AVIATOR，避免因配置缺失导致整体不可用。
 * 常见 engineType：`AVIATOR`、`QL`。
 */
@Component
@RequiredArgsConstructor
public class RuleEvaluatorRouter {

    /** Spring 注入的求值器映射，key 为组件名（如 AVIATOR / QL）。 */
    private final Map<String, RuleEvaluator> evaluators;

    /** 路由并执行规则求值。 */
    public boolean evaluate(String engineType, String ruleJson, Map<String, Object> context) {
        // engineType 由人群定义配置给出，例如 "AVIATOR" / "QL"
        // 未命中时回退 AVIATOR 作为保守默认值
        RuleEvaluator evaluator = evaluators.getOrDefault(engineType, evaluators.get("AVIATOR"));
        // 统一通过抽象接口求值，调用方不感知具体引擎实现
        return evaluator.evaluate(ruleJson, context);
    }
}
