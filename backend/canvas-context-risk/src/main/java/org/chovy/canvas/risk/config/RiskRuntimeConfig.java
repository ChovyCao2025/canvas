package org.chovy.canvas.risk.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.risk.adapter.persistence.MybatisRiskActiveStrategyReader;
import org.chovy.canvas.risk.adapter.persistence.MybatisRiskDecisionLedger;
import org.chovy.canvas.risk.adapter.persistence.RiskDecisionRunMapper;
import org.chovy.canvas.risk.adapter.persistence.RiskRuleHitMapper;
import org.chovy.canvas.risk.adapter.persistence.RiskStrategyMapper;
import org.chovy.canvas.risk.adapter.persistence.RiskStrategyVersionMapper;
import org.chovy.canvas.risk.domain.dsl.RiskRuleJsonCodec;
import org.chovy.canvas.risk.domain.dsl.RiskRuleOperand;
import org.chovy.canvas.risk.domain.runtime.RiskActiveStrategyReader;
import org.chovy.canvas.risk.domain.runtime.RiskDecisionLedger;
import org.chovy.canvas.risk.domain.runtime.RiskDecisionMerger;
import org.chovy.canvas.risk.domain.runtime.RiskDecisionMetrics;
import org.chovy.canvas.risk.domain.runtime.RiskDecisionRequest;
import org.chovy.canvas.risk.domain.runtime.RiskDecisionService;
import org.chovy.canvas.risk.domain.runtime.RiskRequestFeatureResolver;
import org.chovy.canvas.risk.domain.runtime.RiskResolvedValue;
import org.chovy.canvas.risk.domain.runtime.RiskRuleEvaluator;
import org.chovy.canvas.risk.domain.runtime.RiskStrategyCompiler;
import org.chovy.canvas.risk.domain.runtime.RiskStrategyRuntimeCache;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class RiskRuntimeConfig {

    @Bean
    @ConditionalOnMissingBean
    Clock riskClock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean
    RiskStrategyCompiler riskStrategyCompiler(RiskRuleJsonCodec jsonCodec) {
        return new RiskStrategyCompiler(jsonCodec);
    }

    @Bean
    @ConditionalOnMissingBean
    RiskStrategyRuntimeCache riskStrategyRuntimeCache(RiskStrategyCompiler compiler) {
        return new RiskStrategyRuntimeCache(compiler);
    }

    @Bean
    @ConditionalOnMissingBean
    RiskActiveStrategyReader riskActiveStrategyReader(RiskStrategyMapper strategyMapper,
                                                       RiskStrategyVersionMapper versionMapper,
                                                       RiskStrategyRuntimeCache runtimeCache,
                                                       ObjectMapper objectMapper) {
        return new MybatisRiskActiveStrategyReader(strategyMapper, versionMapper, runtimeCache, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    RiskDecisionLedger riskDecisionLedger(RiskDecisionRunMapper runMapper,
                                           RiskRuleHitMapper hitMapper,
                                           ObjectMapper objectMapper) {
        return new MybatisRiskDecisionLedger(runMapper, hitMapper, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    RiskRequestFeatureResolver riskRequestFeatureResolver() {
        return this::resolveRequestFeature;
    }

    @Bean
    @ConditionalOnMissingBean
    RiskRuleEvaluator riskRuleEvaluator() {
        return new RiskRuleEvaluator();
    }

    @Bean
    @ConditionalOnMissingBean
    RiskDecisionMerger riskDecisionMerger() {
        return new RiskDecisionMerger();
    }

    @Bean
    @ConditionalOnMissingBean
    RiskDecisionMetrics riskDecisionMetrics() {
        return new NoopRiskDecisionMetrics();
    }

    @Bean
    @ConditionalOnMissingBean
    RiskDecisionService riskDecisionService(RiskActiveStrategyReader strategyReader,
                                             RiskDecisionLedger ledger,
                                             RiskRequestFeatureResolver featureResolver,
                                             RiskRuleEvaluator evaluator,
                                             RiskDecisionMerger merger,
                                             Clock riskClock,
                                             RiskDecisionMetrics metrics) {
        return new RiskDecisionService(strategyReader, ledger, featureResolver, evaluator, merger, riskClock, metrics);
    }

    private RiskResolvedValue resolveRequestFeature(RiskDecisionRequest request, RiskRuleOperand operand) {
        if (request == null || !(operand instanceof RiskRuleOperand.FeatureOperand feature)) {
            return RiskResolvedValue.missing();
        }
        if (request.suppliedFeatures() != null && request.suppliedFeatures().containsKey(feature.key())) {
            return RiskResolvedValue.present(request.suppliedFeatures().get(feature.key()));
        }
        return RiskResolvedValue.missing();
    }

    private static final class NoopRiskDecisionMetrics implements RiskDecisionMetrics {

        @Override
        public void recordRiskDecision(String sceneKey, String action, int latencyMs) {
        }

        @Override
        public void recordRiskRuleHit(String sceneKey, String groupKey, String ruleKey, String action) {
        }

        @Override
        public void recordRiskFeatureMissing(String sceneKey, String featureKey) {
        }

        @Override
        public void recordRiskDecisionFailure(String sceneKey, String errorType) {
        }
    }
}
