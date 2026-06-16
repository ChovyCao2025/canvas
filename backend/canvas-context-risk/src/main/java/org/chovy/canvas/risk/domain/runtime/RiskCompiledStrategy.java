package org.chovy.canvas.risk.domain.runtime;

import org.chovy.canvas.risk.domain.dsl.RiskRuntimeMode;

import java.util.List;
import java.util.Objects;

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
public final class RiskCompiledStrategy {

    /**
     * RiskCompiledStrategy 的 sceneKey 字段。
     */
    private final String sceneKey;


    /**
     * RiskCompiledStrategy 的 strategyKey 字段。
     */
    private final String strategyKey;


    /**
     * RiskCompiledStrategy 的 version 字段。
     */
    private final int version;


    /**
     * RiskCompiledStrategy 的 mode 字段。
     */
    private final RiskRuntimeMode mode;


    /**
     * RiskCompiledStrategy 的 failPolicy 字段。
     */
    private final RiskFailPolicy failPolicy;


    /**
     * RiskCompiledStrategy 的 requiredFeatures 字段。
     */
    private final List<String> requiredFeatures;


    /**
     * RiskCompiledStrategy 的 rules 字段。
     */
    private final List<RiskCompiledRule> rules;


    /**
     * RiskCompiledStrategy 的 compiledHash 字段。
     */
    private final String compiledHash;


    /**
     * 创建 RiskCompiledStrategy。
     *
     * @param sceneKey RiskCompiledStrategy 的 sceneKey 字段
     * @param strategyKey RiskCompiledStrategy 的 strategyKey 字段
     * @param version RiskCompiledStrategy 的 version 字段
     * @param mode RiskCompiledStrategy 的 mode 字段
     * @param failPolicy RiskCompiledStrategy 的 failPolicy 字段
     * @param requiredFeatures RiskCompiledStrategy 的 requiredFeatures 字段
     * @param rules RiskCompiledStrategy 的 rules 字段
     * @param compiledHash RiskCompiledStrategy 的 compiledHash 字段
     */
    public RiskCompiledStrategy(String sceneKey, String strategyKey, int version, RiskRuntimeMode mode, RiskFailPolicy failPolicy, List<String> requiredFeatures, List<RiskCompiledRule> rules, String compiledHash) {
        requiredFeatures = requiredFeatures == null ? List.of() : List.copyOf(requiredFeatures);
                rules = rules == null ? List.of() : List.copyOf(rules);
                compiledHash = compiledHash == null ? "" : compiledHash;
        this.sceneKey = sceneKey;
        this.strategyKey = strategyKey;
        this.version = version;
        this.mode = mode;
        this.failPolicy = failPolicy;
        this.requiredFeatures = requiredFeatures;
        this.rules = rules;
        this.compiledHash = compiledHash;
    }

    /**
     * 返回 RiskCompiledStrategy 的 sceneKey 字段。
     *
     * @return sceneKey 字段值
     */
    public String sceneKey() {
        return sceneKey;
    }

    /**
     * 返回 RiskCompiledStrategy 的 strategyKey 字段。
     *
     * @return strategyKey 字段值
     */
    public String strategyKey() {
        return strategyKey;
    }

    /**
     * 返回 RiskCompiledStrategy 的 version 字段。
     *
     * @return version 字段值
     */
    public int version() {
        return version;
    }

    /**
     * 返回 RiskCompiledStrategy 的 mode 字段。
     *
     * @return mode 字段值
     */
    public RiskRuntimeMode mode() {
        return mode;
    }

    /**
     * 返回 RiskCompiledStrategy 的 failPolicy 字段。
     *
     * @return failPolicy 字段值
     */
    public RiskFailPolicy failPolicy() {
        return failPolicy;
    }

    /**
     * 返回 RiskCompiledStrategy 的 requiredFeatures 字段。
     *
     * @return requiredFeatures 字段值
     */
    public List<String> requiredFeatures() {
        return requiredFeatures;
    }

    /**
     * 返回 RiskCompiledStrategy 的 rules 字段。
     *
     * @return rules 字段值
     */
    public List<RiskCompiledRule> rules() {
        return rules;
    }

    /**
     * 返回 RiskCompiledStrategy 的 compiledHash 字段。
     *
     * @return compiledHash 字段值
     */
    public String compiledHash() {
        return compiledHash;
    }

    /**
     * 比较当前 RiskCompiledStrategy 与其他对象是否相等。
     *
     * @param o 待比较对象
     * @return 相等时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskCompiledStrategy other)) {
            return false;
        }
        return Objects.equals(sceneKey, other.sceneKey)
                && Objects.equals(strategyKey, other.strategyKey)
                && version == other.version
                && Objects.equals(mode, other.mode)
                && Objects.equals(failPolicy, other.failPolicy)
                && Objects.equals(requiredFeatures, other.requiredFeatures)
                && Objects.equals(rules, other.rules)
                && Objects.equals(compiledHash, other.compiledHash);
    }

    /**
     * 计算 RiskCompiledStrategy 的哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(sceneKey, strategyKey, version, mode, failPolicy, requiredFeatures, rules, compiledHash);
    }

    /**
     * 返回 RiskCompiledStrategy 的调试字符串。
     *
     * @return 调试字符串
     */
    @Override
    public String toString() {
        return "RiskCompiledStrategy[sceneKey=" + sceneKey + ", strategyKey=" + strategyKey + ", version=" + version + ", mode=" + mode + ", failPolicy=" + failPolicy + ", requiredFeatures=" + requiredFeatures + ", rules=" + rules + ", compiledHash=" + compiledHash + "]";
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
