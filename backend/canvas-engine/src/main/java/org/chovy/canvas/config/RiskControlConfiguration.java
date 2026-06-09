package org.chovy.canvas.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.mapper.RiskDecisionRunMapper;
import org.chovy.canvas.dal.mapper.RiskListEntryMapper;
import org.chovy.canvas.dal.mapper.RiskListMapper;
import org.chovy.canvas.dal.mapper.RiskRuleHitMapper;
import org.chovy.canvas.dal.mapper.RiskSceneMapper;
import org.chovy.canvas.dal.mapper.RiskSimulationRunMapper;
import org.chovy.canvas.dal.mapper.RiskStrategyMapper;
import org.chovy.canvas.dal.mapper.RiskStrategyVersionMapper;
import org.chovy.canvas.domain.risk.feature.RedisRiskFeatureStore;
import org.chovy.canvas.domain.risk.feature.RiskFeatureCatalogService;
import org.chovy.canvas.domain.risk.feature.RiskFeatureStore;
import org.chovy.canvas.domain.risk.governance.JdbcRiskSceneStore;
import org.chovy.canvas.domain.risk.governance.JdbcRiskStrategyStateStore;
import org.chovy.canvas.domain.risk.governance.JdbcRiskListStore;
import org.chovy.canvas.domain.risk.governance.RiskListService;
import org.chovy.canvas.domain.risk.governance.RiskSceneService;
import org.chovy.canvas.domain.risk.governance.RiskStrategyService;
import org.chovy.canvas.domain.risk.lab.JdbcRiskSimulationSampleRepository;
import org.chovy.canvas.domain.risk.lab.JdbcRiskSimulationRunRepository;
import org.chovy.canvas.domain.risk.lab.RiskSimulationActivationGuard;
import org.chovy.canvas.domain.risk.lab.RiskSimulationRunRepository;
import org.chovy.canvas.domain.risk.lab.RiskSimulationSampleRepository;
import org.chovy.canvas.domain.risk.lab.RiskSimulationService;
import org.chovy.canvas.domain.risk.runtime.JdbcRiskDecisionLedger;
import org.chovy.canvas.domain.risk.runtime.JdbcRiskDecisionTraceReader;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionLedger;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionMerger;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionService;
import org.chovy.canvas.domain.risk.runtime.RiskRuleEvaluator;
import org.chovy.canvas.domain.risk.runtime.RiskSubjectHashing;
import org.chovy.canvas.web.risk.RiskListAuditSink;
import org.chovy.canvas.web.risk.RiskDecisionTraceReader;
import org.chovy.canvas.web.risk.RiskStrategyAuditSink;
import org.chovy.canvas.web.risk.RiskStrategyRuntimeCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Clock;
import java.time.Duration;

@Configuration(proxyBeanMethods = false)
/**
 * 风控规则引擎 Spring Bean 配置，装配决策、特征、治理、名单和仿真服务。
 */
public class RiskControlConfiguration {

    /**
     * 提供风控运行统一时钟。
     *
     * @return UTC 系统时钟
     */
    @Bean
    public Clock riskClock() {
        return Clock.systemUTC();
    }

    /**
     * 创建风控决策账本。
     *
     * @return JDBC 决策账本
     */
    @Bean
    public RiskDecisionLedger riskDecisionLedger(RiskDecisionRunMapper runMapper,
                                                 RiskRuleHitMapper hitMapper,
                                                 ObjectMapper objectMapper) {
        return new JdbcRiskDecisionLedger(runMapper, hitMapper, objectMapper);
    }

    /**
     * 创建风控决策追踪读取器。
     *
     * @return JDBC 决策追踪读取器
     */
    @Bean
    public RiskDecisionTraceReader riskDecisionTraceReader(RiskDecisionRunMapper runMapper,
                                                           RiskRuleHitMapper hitMapper) {
        return new JdbcRiskDecisionTraceReader(runMapper, hitMapper);
    }

    /**
     * 创建风控特征目录服务。
     *
     * @return 特征目录服务
     */
    @Bean
    public RiskFeatureCatalogService riskFeatureCatalogService() {
        return new RiskFeatureCatalogService();
    }

    /**
     * 创建 Redis 在线特征存储。
     *
     * @return 风控特征存储
     */
    @Bean
    public RiskFeatureStore riskFeatureStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
        return new RedisRiskFeatureStore(redis, objectMapper);
    }

    /**
     * 创建风控场景目录服务。
     *
     * @return 风控场景服务
     */
    @Bean
    public RiskSceneService riskSceneService(RiskSceneMapper sceneMapper,
                                             Clock riskClock) {
        return new RiskSceneService(new JdbcRiskSceneStore(sceneMapper, riskClock));
    }

    /**
     * 创建风控特征解析器。
     *
     * @return 带本地缓存和主体哈希的特征解析器
     */
    @Bean
    public org.chovy.canvas.domain.risk.feature.RiskFeatureResolver riskFeatureResolver(
            RiskFeatureCatalogService catalog,
            RiskFeatureStore featureStore) {
        return new org.chovy.canvas.domain.risk.feature.RiskFeatureResolver(
                catalog, featureStore, Duration.ofMinutes(5), RiskSubjectHashing::sha256);
    }

    /**
     * 创建风控在线决策服务。
     *
     * @return 风控决策服务
     */
    @Bean
    public RiskDecisionService riskDecisionService(RiskStrategyService strategyReader,
                                                   RiskDecisionLedger ledger,
                                                   org.chovy.canvas.domain.risk.feature.RiskFeatureResolver featureResolver,
                                                   CanvasRuntimeMetrics metrics,
                                                   Clock riskClock) {
        return new RiskDecisionService(
                strategyReader,
                ledger,
                featureResolver,
                new RiskRuleEvaluator(),
                new RiskDecisionMerger(),
                riskClock,
                metrics);
    }

    /**
     * 创建策略治理审计写入器。
     *
     * @return 当前实现为空审计写入器
     */
    @Bean
    public RiskStrategyAuditSink riskStrategyAuditSink() {
        return (tenantId, eventType, strategyKey, version, actor) -> {
        };
    }

    /**
     * 创建 Web 层策略运行时缓存失效适配器。
     *
     * @return 当前实现为空缓存失效适配器
     */
    @Bean
    public RiskStrategyRuntimeCache riskStrategyRuntimeCache() {
        return (tenantId, strategyKey) -> {
        };
    }

    /**
     * 创建风控策略治理服务。
     *
     * @return 策略治理服务
     */
    @Bean
    public RiskStrategyService riskStrategyService(RiskStrategyAuditSink auditSink,
                                                   RiskStrategyRuntimeCache runtimeCache,
                                                   CanvasRuntimeMetrics metrics,
                                                   RiskStrategyMapper strategyMapper,
                                                   RiskStrategyVersionMapper strategyVersionMapper,
                                                   ObjectMapper objectMapper,
                                                   Clock riskClock) {
        return new RiskStrategyService(auditSink, runtimeCache, metrics,
                new JdbcRiskStrategyStateStore(strategyMapper, strategyVersionMapper, objectMapper, riskClock));
    }

    /**
     * 创建名单治理审计写入器。
     *
     * @return 返回审计编号的名单审计写入器
     */
    @Bean
    public RiskListAuditSink riskListAuditSink() {
        return (tenantId, eventType, listKey, subjectHash, actor) -> "risk-list-audit:" + eventType;
    }

    /**
     * 创建风控名单治理服务。
     *
     * @return 名单治理服务
     */
    @Bean
    public RiskListService riskListService(RiskListAuditSink auditSink,
                                           CanvasRuntimeMetrics metrics,
                                           Clock riskClock,
                                           RiskListMapper listMapper,
                                           RiskListEntryMapper listEntryMapper) {
        return new RiskListService(auditSink, RiskSubjectHashing::sha256, riskClock, metrics,
                new JdbcRiskListStore(listMapper, listEntryMapper, riskClock));
    }

    /**
     * 创建风控仿真样本仓储。
     *
     * @return JDBC 仿真样本仓储
     */
    @Bean
    public RiskSimulationSampleRepository riskSimulationSampleRepository(RiskDecisionRunMapper runMapper,
                                                                         ObjectMapper objectMapper) {
        return new JdbcRiskSimulationSampleRepository(runMapper, objectMapper);
    }

    /**
     * 创建风控仿真运行仓储。
     *
     * @return JDBC 仿真运行仓储
     */
    @Bean
    public RiskSimulationRunRepository riskSimulationRunRepository(RiskSimulationRunMapper mapper,
                                                                   ObjectMapper objectMapper,
                                                                   Clock riskClock) {
        return new JdbcRiskSimulationRunRepository(mapper, objectMapper, riskClock);
    }

    /**
     * 创建风控仿真激活保护器。
     *
     * @return 当前实现为空操作，确保仿真不直接激活策略
     */
    @Bean
    public RiskSimulationActivationGuard riskSimulationActivationGuard() {
        return (tenantId, strategyKey, version) -> {
        };
    }

    /**
     * 创建风控仿真服务。
     *
     * @return 风控仿真服务
     */
    @Bean
    public RiskSimulationService riskSimulationService(RiskSimulationSampleRepository sampleRepository,
                                                       RiskSimulationActivationGuard activationGuard,
                                                       CanvasRuntimeMetrics metrics,
                                                       RiskSimulationRunRepository runRepository) {
        return new RiskSimulationService(sampleRepository, activationGuard, metrics, runRepository);
    }

}
