package org.chovy.canvas.risk.domain.runtime;

import org.chovy.canvas.risk.domain.dsl.RiskRuleConditionNode;
import org.chovy.canvas.risk.domain.dsl.RiskRuleGroupNode;
import org.chovy.canvas.risk.domain.dsl.RiskRuleJsonCodec;
import org.chovy.canvas.risk.domain.dsl.RiskRuleJsonNode;
import org.chovy.canvas.risk.domain.dsl.RiskRuleOperand;
import org.chovy.canvas.risk.domain.dsl.RiskRuleParseException;
import org.chovy.canvas.risk.domain.dsl.RiskRuleParser;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 风控策略编译器，将持久化策略快照转换为确定性的运行时可执行规则计划。
 */
public class RiskStrategyCompiler {

    /**
     * 保存 limits 对应的风控状态或配置。
     */
    private final RiskStrategyCompileLimits limits;

    /**
     * 保存 parser 对应的风控状态或配置。
     */
    private final RiskRuleParser parser;

    /**
     * 保存 jsonCodec 对应的风控状态或配置。
     */
    private final RiskRuleJsonCodec jsonCodec;


    /**
     * 使用默认编译限制构造编译器。
     */
    public RiskStrategyCompiler(RiskRuleJsonCodec jsonCodec) {
        this(jsonCodec, RiskStrategyCompileLimits.defaults());
    }

    /**
     * 使用指定编译限制构造编译器，空值时回退到默认限制。
     */
    public RiskStrategyCompiler(RiskRuleJsonCodec jsonCodec, RiskStrategyCompileLimits limits) {
        this.jsonCodec = Objects.requireNonNull(jsonCodec, "jsonCodec");
        this.limits = limits == null ? RiskStrategyCompileLimits.defaults() : limits;
        this.parser = new RiskRuleParser(jsonCodec);
    }

    /**
     * 编译策略快照，输出排序后的规则、必需特征集合和编译哈希。
     */
    public RiskCompiledStrategy compile(RiskStrategySnapshot snapshot) {
        validateGroupLimit(snapshot);
        List<IndexedGroup> groups = indexedGroups(snapshot);
        validateRuleLimit(groups);
        List<RiskCompiledRule> rules = new ArrayList<>();
        Set<String> requiredFeatures = new LinkedHashSet<>();
        // 在编译阶段规范化规则组顺序和规则优先级，运行时只需线性遍历编译结果。
        for (IndexedGroup group : sortedEnabledGroups(groups)) {
            validateGroupPolicy(group);
            List<IndexedRule> sortedRules = group.rules().stream()
                    .sorted(Comparator.comparingInt((IndexedRule rule) -> rule.rule().priority()).reversed()
                            .thenComparing(rule -> rule.rule().ruleKey()))
                    .toList();
            for (IndexedRule indexedRule : sortedRules) {
                RiskStrategyRuleDefinition rule = indexedRule.rule();
                RiskDecisionAction action = parseAction(rule.action(), "$.groups[%d].rules[%d].action"
                        .formatted(group.index(), indexedRule.index()));
                RiskRuleGroupNode parsed = parseDsl(rule.dslJson(), "$.groups[%d].rules[%d].dslJson"
                        .formatted(group.index(), indexedRule.index()));
                // 收集规则树里引用的特征，避免运行时每条规则重复解析同一特征。
                collectFeatures(parsed, requiredFeatures);
                rules.add(new RiskCompiledRule(group.group().groupKey(), rule.ruleKey(), parsed, action,
                        rule.scoreDelta(), rule.reasonCode(), false));
            }
        }
        if (requiredFeatures.size() > limits.maxRequiredFeatures()) {
            throw new RiskStrategyCompileException(RiskStrategyCompileErrorCode.FEATURE_LIMIT_EXCEEDED,
                    "$.requiredFeatures", "required feature limit exceeded");
        }
        String compiledHash = compiledHash(snapshot);
        return new RiskCompiledStrategy(snapshot.sceneKey(), snapshot.strategyKey(), snapshot.version(),
                snapshot.mode(), snapshot.failPolicy(), List.copyOf(requiredFeatures), rules, compiledHash);
    }

    /**
     * 校验策略规则组数量是否超过编译上限。
     */
    private void validateGroupLimit(RiskStrategySnapshot snapshot) {
        if (snapshot.groups().size() > limits.maxGroups()) {
            throw new RiskStrategyCompileException(RiskStrategyCompileErrorCode.GROUP_LIMIT_EXCEEDED,
                    "$.groups", "group limit exceeded");
        }
    }

    /**
     * 校验所有规则组内的规则总数是否超过编译上限。
     */
    private void validateRuleLimit(List<IndexedGroup> groups) {
        int ruleCount = groups.stream().mapToInt(group -> group.group().rules().size()).sum();
        if (ruleCount > limits.maxRules()) {
            throw new RiskStrategyCompileException(RiskStrategyCompileErrorCode.RULE_LIMIT_EXCEEDED,
                    "$.groups[*].rules", "rule limit exceeded");
        }
    }

    /**
     * 为规则组和组内规则保留原始数组下标，便于错误路径定位。
     */
    private List<IndexedGroup> indexedGroups(RiskStrategySnapshot snapshot) {
        List<IndexedGroup> groups = new ArrayList<>();
        for (int i = 0; i < snapshot.groups().size(); i++) {
            RiskStrategyRuleGroupDefinition group = snapshot.groups().get(i);
            List<IndexedRule> rules = new ArrayList<>();
            for (int j = 0; j < group.rules().size(); j++) {
                rules.add(new IndexedRule(j, group.rules().get(j)));
            }
            groups.add(new IndexedGroup(i, group, rules));
        }
        return groups;
    }

    /**
     * 过滤禁用规则组，并按执行顺序和业务键稳定排序。
     */
    private List<IndexedGroup> sortedEnabledGroups(List<IndexedGroup> groups) {
        return groups.stream()
                .filter(group -> group.group().enabled())
                .sorted(Comparator.comparingInt((IndexedGroup group) -> group.group().executionOrder())
                        .thenComparing(group -> group.group().groupKey()))
                .toList();
    }

    /**
     * 校验规则组类型和匹配策略是否为受支持枚举值。
     */
    private void validateGroupPolicy(IndexedGroup group) {
        try {
            RiskRuleGroupType.valueOf(group.group().groupType());
            RiskRuleGroupMatchPolicy.valueOf(group.group().matchPolicy());
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (IllegalArgumentException ex) {
            throw new RiskStrategyCompileException(RiskStrategyCompileErrorCode.UNKNOWN_GROUP_POLICY,
                    "$.groups[%d].matchPolicy".formatted(group.index()), "unknown group type or match policy");
        }
    }

    /**
     * 将规则动作字符串解析为运行时决策动作。
     */
    private RiskDecisionAction parseAction(String value, String path) {
        try {
            return RiskDecisionAction.valueOf(value);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (IllegalArgumentException ex) {
            throw new RiskStrategyCompileException(RiskStrategyCompileErrorCode.UNKNOWN_ACTION,
                    path, "unknown action: " + value);
        }
    }

    /**
     * 解析规则 DSL JSON，并在解析前拦截尚未支持的脚本操作数。
     */
    private RiskRuleGroupNode parseDsl(String dslJson, String path) {
        try {
            RiskRuleJsonNode root = jsonCodec.readTree(dslJson);
            if (containsScriptOperand(root)) {
                // 安全表达式沙箱接入前禁止 SCRIPT 操作数进入运行时。
                throw new RiskStrategyCompileException(RiskStrategyCompileErrorCode.SAFE_EXPRESSION_LIMIT_EXCEEDED,
                        path, "safe expression compiler is not enabled");
            }
            return parser.parse(root);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RiskStrategyCompileException ex) {
            throw ex;
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RiskRuleParseException ex) {
            throw new RiskStrategyCompileException(RiskStrategyCompileErrorCode.INVALID_DSL,
                    path, ex.getMessage());
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (IllegalArgumentException ex) {
            throw new RiskStrategyCompileException(RiskStrategyCompileErrorCode.INVALID_DSL,
                    path, "invalid rule JSON");
        }
    }

    /**
     * 递归检查 DSL JSON 是否包含被禁用的 SCRIPT 操作数。
     */
    private boolean containsScriptOperand(RiskRuleJsonNode node) {
        if (node == null || node.isNull() || node.isValue()) {
            return false;
        }
        if (node.isObject()) {
            // 遍历任意嵌套 JSON，防止被禁用操作数隐藏在子组或数组里。
            RiskRuleJsonNode type = node.get("type");
            if (type != null && type.isText() && "SCRIPT".equalsIgnoreCase(type.textValue())) {
                return true;
            }
            return node.fieldNames().stream().anyMatch(field -> containsScriptOperand(node.get(field)));
        }
        if (node.isArray()) {
            for (RiskRuleJsonNode item : node.elements()) {
                if (containsScriptOperand(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 从规则组树中收集所有 FEATURE 操作数引用的特征键。
     */
    private void collectFeatures(RiskRuleGroupNode group, Set<String> features) {
        // 特征收集按结构遍历并去重，用于运行时一次性解析特征快照。
        for (RiskRuleConditionNode condition : group.conditions()) {
            collectFeature(condition.left(), features);
            collectFeature(condition.right(), features);
        }
        group.groups().forEach(child -> collectFeatures(child, features));
    }

    /**
     * 若操作数是 FEATURE，则把特征键加入必需特征集合。
     */
    private void collectFeature(RiskRuleOperand operand, Set<String> features) {
        if (operand instanceof RiskRuleOperand.FeatureOperand feature) {
            features.add(feature.key());
        }
    }

    /**
     * 计算编译产物哈希，表示实际可执行的策略计划。
     */
    private String compiledHash(RiskStrategySnapshot snapshot) {
        try {
            // 哈希排除禁用规则组，并排序对象键，确保代表真实可执行计划。
            Map<String, Object> canonical = new LinkedHashMap<>();
            canonical.put("tenantId", snapshot.tenantId());
            canonical.put("sceneKey", snapshot.sceneKey());
            canonical.put("strategyKey", snapshot.strategyKey());
            canonical.put("version", snapshot.version());
            canonical.put("mode", snapshot.mode());
            canonical.put("trafficPercent", snapshot.trafficPercent());
            canonical.put("failPolicy", snapshot.failPolicy());
            canonical.put("latencyBudgetMs", snapshot.latencyBudgetMs());
            List<Map<String, Object>> groups = new ArrayList<>();
            for (IndexedGroup group : sortedEnabledGroups(indexedGroups(snapshot))) {
                Map<String, Object> groupMap = new LinkedHashMap<>();
                groupMap.put("groupKey", group.group().groupKey());
                groupMap.put("groupType", group.group().groupType());
                groupMap.put("executionOrder", group.group().executionOrder());
                groupMap.put("matchPolicy", group.group().matchPolicy());
                groupMap.put("enabled", group.group().enabled());
                List<Map<String, Object>> rules = new ArrayList<>();
                for (IndexedRule rule : group.rules().stream()
                        .sorted(Comparator.comparingInt((IndexedRule item) -> item.rule().priority()).reversed()
                                .thenComparing(item -> item.rule().ruleKey()))
                        .toList()) {
                    Map<String, Object> ruleMap = new LinkedHashMap<>();
                    ruleMap.put("ruleKey", rule.rule().ruleKey());
                    ruleMap.put("priority", rule.rule().priority());
                    ruleMap.put("mode", rule.rule().mode());
                    ruleMap.put("dsl", canonicalDsl(rule.rule().dslJson()));
                    ruleMap.put("action", rule.rule().action());
                    ruleMap.put("scoreDelta", rule.rule().scoreDelta());
                    ruleMap.put("reasonCode", rule.rule().reasonCode());
                    ruleMap.put("labels", rule.rule().labels());
                    rules.add(ruleMap);
                }
                groupMap.put("rules", rules);
                groups.add(groupMap);
            }
            canonical.put("groups", groups);
            String json = jsonCodec.writeCanonical(canonical);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return "sha256:" + HexFormat.of().formatHex(digest.digest(json.getBytes(StandardCharsets.UTF_8)));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("risk strategy hash could not be computed", ex);
        }
    }

    /**
     * 解析并规范化 DSL JSON，使字段顺序不影响编译哈希。
     */
    private Object canonicalDsl(String dslJson) {
        return sortNode(jsonCodec.readTree(dslJson));
    }

    /**
     * 递归排序 JSON 对象字段，数组顺序保持不变以保留规则语义。
     */
    private Object sortNode(RiskRuleJsonNode node) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (node == null || node.isMissing() || node.isNull()) {
            return null;
        }
        if (node.isValue()) {
            return node.value();
        }
        if (node.isArray()) {
            List<Object> array = new ArrayList<>();
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            node.elements().forEach(item -> array.add(sortNode(item)));
            return List.copyOf(array);
        }
        Map<String, Object> object = new LinkedHashMap<>();
        node.fieldNames().stream().sorted().forEach(field -> object.put(field, sortNode(node.get(field))));
        return object;
    }

    /**
     * 保留原始顺序的规则组索引。
     *
     * @param index 规则组在定义中的原始位置
     * @param group 规则组定义
     * @param rules 规则组内带原始位置的规则列表
     */
    private static final class IndexedGroup {

        /**
         * IndexedGroup 的 index 字段。
         */
        private final int index;


        /**
         * IndexedGroup 的 group 字段。
         */
        private final RiskStrategyRuleGroupDefinition group;


        /**
         * IndexedGroup 的 rules 字段。
         */
        private final List<IndexedRule> rules;


        /**
         * 创建 IndexedGroup。
         *
         * @param index IndexedGroup 的 index 字段
         * @param group IndexedGroup 的 group 字段
         * @param rules IndexedGroup 的 rules 字段
         */
        public IndexedGroup(int index, RiskStrategyRuleGroupDefinition group, List<IndexedRule> rules) {
            this.index = index;
            this.group = group;
            this.rules = rules;
        }

        /**
         * 返回 IndexedGroup 的 index 字段。
         *
         * @return index 字段值
         */
        public int index() {
            return index;
        }

        /**
         * 返回 IndexedGroup 的 group 字段。
         *
         * @return group 字段值
         */
        public RiskStrategyRuleGroupDefinition group() {
            return group;
        }

        /**
         * 返回 IndexedGroup 的 rules 字段。
         *
         * @return rules 字段值
         */
        public List<IndexedRule> rules() {
            return rules;
        }

        /**
         * 比较当前 IndexedGroup 与其他对象是否相等。
         *
         * @param o 待比较对象
         * @return 相等时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof IndexedGroup other)) {
                return false;
            }
            return index == other.index
                    && Objects.equals(group, other.group)
                    && Objects.equals(rules, other.rules);
        }

        /**
         * 计算 IndexedGroup 的哈希值。
         *
         * @return 哈希值
         */
        @Override
        public int hashCode() {
            return Objects.hash(index, group, rules);
        }

        /**
         * 返回 IndexedGroup 的调试字符串。
         *
         * @return 调试字符串
         */
        @Override
        public String toString() {
            return "IndexedGroup[index=" + index + ", group=" + group + ", rules=" + rules + "]";
        }
    }

    /**
     * 保留原始顺序的规则索引。
     *
     * @param index 规则在规则组中的原始位置
     * @param rule 规则定义
     */
    private static final class IndexedRule {

        /**
         * IndexedRule 的 index 字段。
         */
        private final int index;


        /**
         * IndexedRule 的 rule 字段。
         */
        private final RiskStrategyRuleDefinition rule;


        /**
         * 创建 IndexedRule。
         *
         * @param index IndexedRule 的 index 字段
         * @param rule IndexedRule 的 rule 字段
         */
        public IndexedRule(int index, RiskStrategyRuleDefinition rule) {
            this.index = index;
            this.rule = rule;
        }

        /**
         * 返回 IndexedRule 的 index 字段。
         *
         * @return index 字段值
         */
        public int index() {
            return index;
        }

        /**
         * 返回 IndexedRule 的 rule 字段。
         *
         * @return rule 字段值
         */
        public RiskStrategyRuleDefinition rule() {
            return rule;
        }

        /**
         * 比较当前 IndexedRule 与其他对象是否相等。
         *
         * @param o 待比较对象
         * @return 相等时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof IndexedRule other)) {
                return false;
            }
            return index == other.index
                    && Objects.equals(rule, other.rule);
        }

        /**
         * 计算 IndexedRule 的哈希值。
         *
         * @return 哈希值
         */
        @Override
        public int hashCode() {
            return Objects.hash(index, rule);
        }

        /**
         * 返回 IndexedRule 的调试字符串。
         *
         * @return 调试字符串
         */
        @Override
        public String toString() {
            return "IndexedRule[index=" + index + ", rule=" + rule + "]";
        }
    }
}
