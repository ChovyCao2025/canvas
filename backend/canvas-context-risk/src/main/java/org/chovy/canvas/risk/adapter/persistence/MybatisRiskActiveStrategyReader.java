package org.chovy.canvas.risk.adapter.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.risk.domain.dsl.RiskRuntimeMode;
import org.chovy.canvas.risk.domain.runtime.RiskActiveStrategyReader;
import org.chovy.canvas.risk.domain.runtime.RiskCompiledStrategy;
import org.chovy.canvas.risk.domain.runtime.RiskFailPolicy;
import org.chovy.canvas.risk.domain.runtime.RiskStrategyRuntimeCache;
import org.chovy.canvas.risk.domain.runtime.RiskStrategyRuleDefinition;
import org.chovy.canvas.risk.domain.runtime.RiskStrategyRuleGroupDefinition;
import org.chovy.canvas.risk.domain.runtime.RiskStrategySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * MyBatis-backed active strategy reader for the online risk decision runtime.
 */
public class MybatisRiskActiveStrategyReader implements RiskActiveStrategyReader {

    private final RiskStrategyMapper strategyMapper;
    private final RiskStrategyVersionMapper versionMapper;
    private final RiskStrategyRuntimeCache runtimeCache;
    private final ObjectMapper objectMapper;

    public MybatisRiskActiveStrategyReader(RiskStrategyMapper strategyMapper,
                                           RiskStrategyVersionMapper versionMapper,
                                           RiskStrategyRuntimeCache runtimeCache,
                                           ObjectMapper objectMapper) {
        this.strategyMapper = strategyMapper;
        this.versionMapper = versionMapper;
        this.runtimeCache = runtimeCache;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    @Override
    public RiskCompiledStrategy findActiveStrategy(Long tenantId, String sceneKey) {
        RiskStrategyDO strategy = strategyMapper.selectList(new LambdaQueryWrapper<RiskStrategyDO>()
                        .eq(RiskStrategyDO::getTenantId, tenantId)
                        .eq(RiskStrategyDO::getSceneKey, sceneKey)
                        .eq(RiskStrategyDO::getStatus, "ACTIVE")
                        .isNotNull(RiskStrategyDO::getActiveVersion)
                        .orderByAsc(RiskStrategyDO::getStrategyKey))
                .stream()
                .findFirst()
                .orElse(null);
        if (strategy == null) {
            return null;
        }
        RiskStrategyVersionDO version = versionMapper.selectOne(new LambdaQueryWrapper<RiskStrategyVersionDO>()
                .eq(RiskStrategyVersionDO::getTenantId, tenantId)
                .eq(RiskStrategyVersionDO::getStrategyKey, strategy.getStrategyKey())
                .eq(RiskStrategyVersionDO::getVersion, strategy.getActiveVersion())
                .last("LIMIT 1"));
        if (version == null) {
            return null;
        }
        return runtimeCache.getOrCompile(snapshot(strategy, version));
    }

    private RiskStrategySnapshot snapshot(RiskStrategyDO strategy, RiskStrategyVersionDO version) {
        try {
            JsonNode root = objectMapper.readTree(version.getDefinitionJson());
            return new RiskStrategySnapshot(
                    strategy.getTenantId(),
                    strategy.getSceneKey(),
                    strategy.getStrategyKey(),
                    version.getVersion(),
                    enumValue(RiskRuntimeMode.class, text(root, "mode", defaultText(version.getMode(), "ENFORCE"))),
                    integer(root, "trafficPercent", version.getTrafficPercent() == null
                            ? 100
                            : version.getTrafficPercent().intValue()),
                    enumValue(RiskFailPolicy.class, text(root, "failPolicy", "FAIL_REVIEW")),
                    integer(root, "latencyBudgetMs", 50),
                    groups(root),
                    Map.of("riskLevel", Objects.toString(strategy.getRiskLevel(), "")));
        } catch (Exception error) {
            throw new IllegalStateException("active risk strategy cannot be compiled: "
                    + strategy.getStrategyKey(), error);
        }
    }

    private List<RiskStrategyRuleGroupDefinition> groups(JsonNode root) {
        JsonNode groupNodes = root.path("groups");
        if (!groupNodes.isArray()) {
            groupNodes = objectMapper.createArrayNode().add(objectMapper.createObjectNode()
                    .put("groupKey", "default")
                    .put("groupType", "HARD_RULE")
                    .put("executionOrder", 0)
                    .put("matchPolicy", "ANY_MATCHED")
                    .put("enabled", true)
                    .set("rules", root.path("rules")));
        }
        List<RiskStrategyRuleGroupDefinition> result = new ArrayList<>();
        for (int i = 0; i < groupNodes.size(); i++) {
            JsonNode group = groupNodes.get(i);
            result.add(new RiskStrategyRuleGroupDefinition(
                    text(group, "groupKey", "group-" + i),
                    text(group, "groupType", "HARD_RULE"),
                    integer(group, "executionOrder", i),
                    text(group, "matchPolicy", "ANY_MATCHED"),
                    !group.has("enabled") || group.path("enabled").asBoolean(),
                    rules(group.path("rules"))));
        }
        return result;
    }

    private List<RiskStrategyRuleDefinition> rules(JsonNode ruleNodes) {
        if (!ruleNodes.isArray()) {
            return List.of();
        }
        List<RiskStrategyRuleDefinition> result = new ArrayList<>();
        for (int i = 0; i < ruleNodes.size(); i++) {
            JsonNode rule = ruleNodes.get(i);
            result.add(new RiskStrategyRuleDefinition(
                    text(rule, "ruleKey", "rule-" + i),
                    integer(rule, "priority", 0),
                    enumValue(RiskRuntimeMode.class, text(rule, "mode", "ENFORCE")),
                    dslJson(rule),
                    text(rule, "action", "REVIEW"),
                    integer(rule, "scoreDelta", 0),
                    text(rule, "reasonCode", text(rule, "ruleKey", "rule-" + i)),
                    labels(rule.path("labels"))));
        }
        return result;
    }

    private String dslJson(JsonNode rule) {
        JsonNode dslJson = rule.get("dslJson");
        if (dslJson != null && dslJson.isTextual()) {
            return dslJson.asText();
        }
        JsonNode dsl = rule.get("dsl");
        try {
            return dsl == null || dsl.isMissingNode() || dsl.isNull()
                    ? "{\"logic\":\"AND\",\"conditions\":[],\"groups\":[]}"
                    : objectMapper.writeValueAsString(dsl);
        } catch (Exception error) {
            throw new IllegalArgumentException("rule DSL cannot be serialized", error);
        }
    }

    private List<String> labels(JsonNode labels) {
        if (!labels.isArray()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        labels.forEach(label -> result.add(label.asText()));
        return result;
    }

    private String text(JsonNode node, String field, String fallback) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() || value.asText().isBlank() ? fallback : value.asText();
    }

    private int integer(JsonNode node, String field, int fallback) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || !value.canConvertToInt() ? fallback : value.asInt();
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private <T extends Enum<T>> T enumValue(Class<T> type, String value) {
        return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
    }
}
