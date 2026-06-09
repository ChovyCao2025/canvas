package org.chovy.canvas.domain.risk.runtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.chovy.canvas.domain.risk.dsl.RiskRuleConditionNode;
import org.chovy.canvas.domain.risk.dsl.RiskRuleGroupNode;
import org.chovy.canvas.domain.risk.dsl.RiskRuleOperand;
import org.chovy.canvas.domain.risk.dsl.RiskRuleParseException;
import org.chovy.canvas.domain.risk.dsl.RiskRuleParser;

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
import java.util.Set;

/**
 * 风控策略编译器，将持久化策略快照转换为确定性的运行时可执行规则计划。
 */
public class RiskStrategyCompiler {

    private final RiskStrategyCompileLimits limits;
    private final RiskRuleParser parser;
    private final ObjectMapper objectMapper;

    /**
     * 使用默认编译限制构造编译器。
     */
    public RiskStrategyCompiler() {
        this(RiskStrategyCompileLimits.defaults());
    }

    /**
     * 使用指定编译限制构造编译器，空值时回退到默认限制。
     */
    public RiskStrategyCompiler(RiskStrategyCompileLimits limits) {
        this.limits = limits == null ? RiskStrategyCompileLimits.defaults() : limits;
        this.objectMapper = new ObjectMapper().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        this.parser = new RiskRuleParser(objectMapper);
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
            JsonNode root = objectMapper.readTree(dslJson);
            if (containsScriptOperand(root)) {
                // 安全表达式沙箱接入前禁止 SCRIPT 操作数进入运行时。
                throw new RiskStrategyCompileException(RiskStrategyCompileErrorCode.SAFE_EXPRESSION_LIMIT_EXCEEDED,
                        path, "safe expression compiler is not enabled");
            }
            return parser.parse(dslJson);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RiskStrategyCompileException ex) {
            throw ex;
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RiskRuleParseException ex) {
            throw new RiskStrategyCompileException(RiskStrategyCompileErrorCode.INVALID_DSL,
                    path, ex.getMessage());
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            throw new RiskStrategyCompileException(RiskStrategyCompileErrorCode.INVALID_DSL,
                    path, "invalid rule JSON");
        }
    }

    /**
     * 递归检查 DSL JSON 是否包含被禁用的 SCRIPT 操作数。
     */
    private boolean containsScriptOperand(JsonNode node) {
        if (node == null || node.isNull() || node.isValueNode()) {
            return false;
        }
        if (node.isObject()) {
            // 遍历任意嵌套 JSON，防止被禁用操作数隐藏在子组或数组里。
            JsonNode type = node.get("type");
            if (type != null && type.isTextual() && "SCRIPT".equalsIgnoreCase(type.textValue())) {
                return true;
            }
            List<String> fields = new ArrayList<>();
            node.fieldNames().forEachRemaining(fields::add);
            return fields.stream().anyMatch(field -> containsScriptOperand(node.get(field)));
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
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
            String json = objectMapper.writeValueAsString(canonical);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return "sha256:" + HexFormat.of().formatHex(digest.digest(json.getBytes(StandardCharsets.UTF_8)));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException | NoSuchAlgorithmException ex) {
            throw new IllegalStateException("risk strategy hash could not be computed", ex);
        }
    }

    /**
     * 解析并规范化 DSL JSON，使字段顺序不影响编译哈希。
     */
    private JsonNode canonicalDsl(String dslJson) throws JsonProcessingException {
        return sortNode(objectMapper.readTree(dslJson));
    }

    /**
     * 递归排序 JSON 对象字段，数组顺序保持不变以保留规则语义。
     */
    private JsonNode sortNode(JsonNode node) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (node == null || node.isNull() || node.isValueNode()) {
            return node;
        }
        if (node.isArray()) {
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            ArrayNode array = objectMapper.createArrayNode();
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            node.forEach(item -> array.add(sortNode(item)));
            return array;
        }
        ObjectNode object = objectMapper.createObjectNode();
        List<String> fields = new ArrayList<>();
        node.fieldNames().forEachRemaining(fields::add);
        fields.stream().sorted().forEach(field -> object.set(field, sortNode(node.get(field))));
        return object;
    }

    /**
     * 保留原始顺序的规则组索引。
     *
     * @param index 规则组在定义中的原始位置
     * @param group 规则组定义
     * @param rules 规则组内带原始位置的规则列表
     */
    private record IndexedGroup(int index, RiskStrategyRuleGroupDefinition group, List<IndexedRule> rules) {
    }

    /**
     * 保留原始顺序的规则索引。
     *
     * @param index 规则在规则组中的原始位置
     * @param rule 规则定义
     */
    private record IndexedRule(int index, RiskStrategyRuleDefinition rule) {
    }
}
