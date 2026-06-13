package org.chovy.canvas.risk.domain.runtime;

import org.chovy.canvas.risk.domain.dsl.RiskRuntimeMode;

import java.util.List;

/**
 * 不可变的风控可执行策略，包含规范化规则和预计算特征依赖。
 *
 * @param sceneKey 场景业务键
 * @param strategyKey 策略业务键
 * @param version 策略版本号
 * @param mode 运行模式
 * @param failPolicy 运行时失败策略
 * @param requiredFeatures 策略执行所需特征键
 * @param rules 已编译规则列表
 * @param compiledHash 编译后稳定哈希
 */
public record RiskCompiledStrategy(
        String sceneKey,
        String strategyKey,
        int version,
        RiskRuntimeMode mode,
        RiskFailPolicy failPolicy,
        List<String> requiredFeatures,
        List<RiskCompiledRule> rules,
        String compiledHash
) {

    public RiskCompiledStrategy {
        requiredFeatures = requiredFeatures == null ? List.of() : List.copyOf(requiredFeatures);
        rules = rules == null ? List.of() : List.copyOf(rules);
        compiledHash = compiledHash == null ? "" : compiledHash;
    }

    /**
     * 创建不带编译哈希的兼容策略对象。
     */
    public RiskCompiledStrategy(String sceneKey,
                                String strategyKey,
                                int version,
                                RiskRuntimeMode mode,
                                RiskFailPolicy failPolicy,
                                List<String> requiredFeatures,
                                List<RiskCompiledRule> rules) {
        this(sceneKey, strategyKey, version, mode, failPolicy, requiredFeatures, rules, "");
    }

    /**
     * 返回替换必需特征后的策略副本。
     */
    public RiskCompiledStrategy withRequiredFeatures(List<String> features) {
        return new RiskCompiledStrategy(sceneKey, strategyKey, version, mode, failPolicy, features, rules, compiledHash);
    }

    /**
     * 返回替换失败策略后的策略副本，空值按人工复核处理。
     */
    public RiskCompiledStrategy withFailPolicy(RiskFailPolicy policy) {
        return new RiskCompiledStrategy(sceneKey, strategyKey, version, mode,
                policy == null ? RiskFailPolicy.FAIL_REVIEW : policy,
                requiredFeatures, rules, compiledHash);
    }
}
