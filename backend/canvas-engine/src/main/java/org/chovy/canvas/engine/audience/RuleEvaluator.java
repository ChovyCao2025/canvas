package org.chovy.canvas.engine.audience;

import java.util.Map;

/**
 * 人群规则求值器抽象。
 *
 * <p>不同实现可基于不同表达式引擎（如 Aviator、QLExpress）。
 * 调用方只关心布尔结果，不依赖具体引擎实现细节。
 * 任何引擎异常都建议在实现层做降级，避免向上抛出中断批任务。
 */
public interface RuleEvaluator {

    /**
     * 对规则进行求值。
     *
     * @param ruleJson 规则 JSON
     * @param context  运行时上下文
     * @return 是否命中
     */
    boolean evaluate(String ruleJson, Map<String, Object> context);

    // 统一返回 boolean，避免不同引擎暴露不一致的中间结果结构。
    // 如需返回命中明细，建议另建扩展接口而非改动现有契约。
    // 调用方（如 RuleEvaluatorRouter）只关心 true/false，不关心引擎内部 AST/脚本细节。
    // 失败降级策略也由实现类内部处理，不向上层传播引擎实现异常细节。
}
