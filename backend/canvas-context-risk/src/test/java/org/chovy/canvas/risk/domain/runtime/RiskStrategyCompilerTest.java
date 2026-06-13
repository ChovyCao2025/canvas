package org.chovy.canvas.risk.domain.runtime;

import org.chovy.canvas.risk.adapter.external.JacksonRiskRuleJsonCodec;
import org.chovy.canvas.risk.domain.dsl.RiskRuntimeMode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class RiskStrategyCompilerTest {

    private final JacksonRiskRuleJsonCodec jsonCodec = new JacksonRiskRuleJsonCodec();
    private final RiskStrategyCompiler compiler = new RiskStrategyCompiler(jsonCodec);

    @Test
    void compilesStrategyIntoStableOrderedSnapshot() {
        RiskStrategySnapshot snapshot = snapshot(List.of(
                group("z-late", 20, true, List.of(rule("r2", 1, "ALLOW", featureRule("risk.low")))),
                group("a-early", 10, true, List.of(
                        rule("b-rule", 10, "BLOCK", featureRule("risk.high")),
                        rule("a-rule", 10, "REVIEW", featureRule("risk.medium")))),
                group("disabled", 5, false, List.of(rule("skip", 99, "BLOCK", featureRule("risk.skip"))))));

        RiskCompiledStrategy compiled = compiler.compile(snapshot);

        assertThat(compiled.sceneKey()).isEqualTo("payment");
        assertThat(compiled.strategyKey()).isEqualTo("payment-risk");
        assertThat(compiled.version()).isEqualTo(12);
        assertThat(compiled.compiledHash()).startsWith("sha256:");
        assertThat(compiled.rules()).extracting(RiskCompiledRule::ruleKey)
                .containsExactly("a-rule", "b-rule", "r2");
    }

    @Test
    void recordsOnlyDeclaredRequiredFeatures() {
        RiskCompiledStrategy compiled = compiler.compile(snapshot(List.of(
                group("mixed", 1, true, List.of(rule("mixed-rule", 1, "BLOCK", """
                        {
                          "logic":"AND",
                          "conditions":[
                            {"left":{"type":"FEATURE","key":"risk.score"},"op":">=","right":{"type":"LITERAL","value":85}},
                            {"left":{"type":"CONTEXT","path":"caller"},"op":"==","right":{"type":"LITERAL","value":"CANVAS"}},
                            {"left":{"type":"EVENT","path":"amount"},"op":">","right":{"type":"LITERAL","value":100}},
                            {"left":{"type":"SUBJECT","path":"userId"},"op":"IN","right":{"type":"LIST","key":"blacklist.user"}}
                          ],
                          "groups":[]
                        }
                        """))))));

        assertThat(compiled.requiredFeatures()).containsExactly("risk.score");
    }

    @Test
    void compiledHashIsStableForCanonicalEquivalentSnapshots() {
        RiskStrategySnapshot left = snapshot(List.of(group("g", 1, true,
                List.of(rule("r", 1, "BLOCK", featureRule("risk.score"))))));
        RiskStrategySnapshot right = snapshot(List.of(group("g", 1, true,
                List.of(rule("r", 1, "BLOCK", """
                        {"groups":[],"conditions":[{"right":{"value":85,"type":"LITERAL"},"op":">=","left":{"key":"risk.score","type":"FEATURE"}}],"logic":"AND"}
                        """)))));

        assertThat(compiler.compile(left).compiledHash()).isEqualTo(compiler.compile(right).compiledHash());
    }

    @Test
    void rejectsUnknownActionWithJsonPath() {
        RiskStrategySnapshot snapshot = snapshot(List.of(group("g", 1, true,
                List.of(rule("r", 1, "ESCALATE", featureRule("risk.score"))))));

        assertThatExceptionOfType(RiskStrategyCompileException.class)
                .isThrownBy(() -> compiler.compile(snapshot))
                .satisfies(error -> {
                    assertThat(error.code()).isEqualTo(RiskStrategyCompileErrorCode.UNKNOWN_ACTION);
                    assertThat(error.path()).isEqualTo("$.groups[0].rules[0].action");
                });
    }

    @Test
    void rejectsCompileLimits() {
        RiskStrategyCompiler limited = new RiskStrategyCompiler(jsonCodec,
                new RiskStrategyCompileLimits(1, 1, 1, 0, 1024));
        RiskStrategySnapshot tooManyGroups = snapshot(List.of(
                group("g1", 1, true, List.of(rule("r1", 1, "BLOCK", featureRule("risk.one")))),
                group("g2", 2, true, List.of(rule("r2", 1, "BLOCK", featureRule("risk.two"))))));

        assertThatExceptionOfType(RiskStrategyCompileException.class)
                .isThrownBy(() -> limited.compile(tooManyGroups))
                .satisfies(error -> assertThat(error.code()).isEqualTo(RiskStrategyCompileErrorCode.GROUP_LIMIT_EXCEEDED));

        RiskStrategySnapshot tooManyRules = snapshot(List.of(group("g", 1, true, List.of(
                rule("r1", 1, "BLOCK", featureRule("risk.one")),
                rule("r2", 2, "BLOCK", featureRule("risk.two"))))));
        assertThatExceptionOfType(RiskStrategyCompileException.class)
                .isThrownBy(() -> limited.compile(tooManyRules))
                .satisfies(error -> assertThat(error.code()).isEqualTo(RiskStrategyCompileErrorCode.RULE_LIMIT_EXCEEDED));

        RiskStrategySnapshot tooManyFeatures = snapshot(List.of(group("g", 1, true, List.of(
                rule("r", 1, "BLOCK", """
                        {"logic":"AND","conditions":[
                          {"left":{"type":"FEATURE","key":"risk.one"},"op":">=","right":{"type":"LITERAL","value":1}},
                          {"left":{"type":"FEATURE","key":"risk.two"},"op":">=","right":{"type":"LITERAL","value":1}}
                        ],"groups":[]}
                        """)))));
        assertThatExceptionOfType(RiskStrategyCompileException.class)
                .isThrownBy(() -> limited.compile(tooManyFeatures))
                .satisfies(error -> assertThat(error.code()).isEqualTo(RiskStrategyCompileErrorCode.FEATURE_LIMIT_EXCEEDED));
    }

    @Test
    void rejectsSafeExpressionUntilGovernedCompilerExists() {
        RiskStrategySnapshot snapshot = snapshot(List.of(group("g", 1, true,
                List.of(rule("r", 1, "BLOCK", """
                        {"logic":"AND","conditions":[{"left":{"type":"SCRIPT","body":"x"},"op":"==","right":{"type":"LITERAL","value":true}}],"groups":[]}
                        """)))));

        assertThatExceptionOfType(RiskStrategyCompileException.class)
                .isThrownBy(() -> compiler.compile(snapshot))
                .satisfies(error -> assertThat(error.code()).isEqualTo(RiskStrategyCompileErrorCode.SAFE_EXPRESSION_LIMIT_EXCEEDED));
    }

    @Test
    void cacheReturnsSameCompiledSnapshotUntilInvalidated() {
        RiskStrategyRuntimeCache cache = new RiskStrategyRuntimeCache(compiler);
        RiskStrategySnapshot snapshot = snapshot(List.of(group("g", 1, true,
                List.of(rule("r", 1, "BLOCK", featureRule("risk.score"))))));

        RiskCompiledStrategy first = cache.getOrCompile(snapshot);
        RiskCompiledStrategy second = cache.getOrCompile(snapshot);

        assertThat(second).isSameAs(first);
        cache.invalidate(7L, "payment", "payment-risk", 12);
        assertThat(cache.getOrCompile(snapshot)).isNotSameAs(first);
    }

    @Test
    void cacheInvalidationRemovesOneStrategyVersionOnly() {
        RiskStrategyRuntimeCache cache = new RiskStrategyRuntimeCache(compiler);
        RiskStrategySnapshot version11 = snapshot(11, List.of(group("g", 1, true,
                List.of(rule("r", 1, "BLOCK", featureRule("risk.score"))))));
        RiskStrategySnapshot version12 = snapshot(12, List.of(group("g", 1, true,
                List.of(rule("r", 1, "BLOCK", featureRule("risk.score"))))));

        RiskCompiledStrategy compiled11 = cache.getOrCompile(version11);
        RiskCompiledStrategy compiled12 = cache.getOrCompile(version12);

        cache.invalidate(7L, "payment", "payment-risk", 12);

        assertThat(cache.getOrCompile(version11)).isSameAs(compiled11);
        assertThat(cache.getOrCompile(version12)).isNotSameAs(compiled12);
    }

    private RiskStrategySnapshot snapshot(List<RiskStrategyRuleGroupDefinition> groups) {
        return snapshot(12, groups);
    }

    private RiskStrategySnapshot snapshot(int version, List<RiskStrategyRuleGroupDefinition> groups) {
        return new RiskStrategySnapshot(
                7L,
                "payment",
                "payment-risk",
                version,
                RiskRuntimeMode.ENFORCE,
                100,
                RiskFailPolicy.FAIL_REVIEW,
                50,
                groups,
                Map.of());
    }

    private RiskStrategyRuleGroupDefinition group(String groupKey, int executionOrder, boolean enabled,
                                                  List<RiskStrategyRuleDefinition> rules) {
        return new RiskStrategyRuleGroupDefinition(groupKey, "HARD_RULE", executionOrder, "ANY_MATCHED", enabled, rules);
    }

    private RiskStrategyRuleDefinition rule(String ruleKey, int priority, String action, String dslJson) {
        return new RiskStrategyRuleDefinition(ruleKey, priority, RiskRuntimeMode.ENFORCE, dslJson, action,
                10, ruleKey, List.of());
    }

    private String featureRule(String featureKey) {
        return """
                {"logic":"AND","conditions":[{"left":{"type":"FEATURE","key":"%s"},"op":">=","right":{"type":"LITERAL","value":85}}],"groups":[]}
                """.formatted(featureKey);
    }
}
