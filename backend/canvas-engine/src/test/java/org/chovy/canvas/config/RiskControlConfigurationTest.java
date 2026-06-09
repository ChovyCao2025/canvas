package org.chovy.canvas.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.dal.mapper.RiskDecisionRunMapper;
import org.chovy.canvas.dal.mapper.RiskListEntryMapper;
import org.chovy.canvas.dal.mapper.RiskListMapper;
import org.chovy.canvas.dal.mapper.RiskRuleHitMapper;
import org.chovy.canvas.dal.mapper.RiskSceneMapper;
import org.chovy.canvas.dal.mapper.RiskSimulationRunMapper;
import org.chovy.canvas.dal.mapper.RiskStrategyMapper;
import org.chovy.canvas.dal.mapper.RiskStrategyVersionMapper;
import org.chovy.canvas.domain.risk.feature.RedisRiskFeatureStore;
import org.chovy.canvas.domain.risk.feature.RiskFeatureStore;
import org.chovy.canvas.domain.risk.governance.RiskListService;
import org.chovy.canvas.domain.risk.governance.RiskSceneService;
import org.chovy.canvas.domain.risk.governance.RiskStrategyService;
import org.chovy.canvas.domain.risk.lab.JdbcRiskSimulationSampleRepository;
import org.chovy.canvas.domain.risk.lab.JdbcRiskSimulationRunRepository;
import org.chovy.canvas.domain.risk.lab.RiskSimulationRunRepository;
import org.chovy.canvas.domain.risk.lab.RiskSimulationSampleRepository;
import org.chovy.canvas.domain.risk.lab.RiskSimulationService;
import org.chovy.canvas.domain.risk.runtime.JdbcRiskDecisionLedger;
import org.chovy.canvas.domain.risk.runtime.JdbcRiskDecisionTraceReader;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionLedger;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionService;
import org.chovy.canvas.web.risk.RiskDecisionTraceReader;
import org.chovy.canvas.web.risk.RiskDecisionController;
import org.chovy.canvas.web.risk.RiskLabController;
import org.chovy.canvas.web.risk.RiskListController;
import org.chovy.canvas.web.risk.RiskSceneController;
import org.chovy.canvas.web.risk.RiskStrategyController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RiskControlConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(RiskControlConfiguration.class,
                    RiskDecisionController.class,
                    RiskStrategyController.class,
                    RiskListController.class,
                    RiskSceneController.class,
                    RiskLabController.class)
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(CanvasRuntimeMetrics.class, () -> new CanvasRuntimeMetrics(new SimpleMeterRegistry()))
            .withBean(TenantContextResolver.class, () -> mock(TenantContextResolver.class))
            .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
            .withBean(RiskDecisionRunMapper.class, () -> mock(RiskDecisionRunMapper.class))
            .withBean(RiskRuleHitMapper.class, () -> mock(RiskRuleHitMapper.class))
            .withBean(RiskSceneMapper.class, () -> mock(RiskSceneMapper.class))
            .withBean(RiskSimulationRunMapper.class, () -> mock(RiskSimulationRunMapper.class))
            .withBean(RiskListMapper.class, () -> mock(RiskListMapper.class))
            .withBean(RiskListEntryMapper.class, () -> mock(RiskListEntryMapper.class))
            .withBean(RiskStrategyMapper.class, () -> mock(RiskStrategyMapper.class))
            .withBean(RiskStrategyVersionMapper.class, () -> mock(RiskStrategyVersionMapper.class));

    @Test
    void productionRiskApiBeansAreComposable() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(RiskDecisionController.class);
            assertThat(context).hasSingleBean(RiskStrategyController.class);
            assertThat(context).hasSingleBean(RiskListController.class);
            assertThat(context).hasSingleBean(RiskSceneController.class);
            assertThat(context).hasSingleBean(RiskLabController.class);
            assertThat(context).hasSingleBean(RiskDecisionService.class);
            assertThat(context).hasSingleBean(RiskStrategyService.class);
            assertThat(context).hasSingleBean(RiskListService.class);
            assertThat(context).hasSingleBean(RiskSceneService.class);
            assertThat(context).hasSingleBean(RiskSimulationService.class);
            assertThat(context).hasSingleBean(RiskSimulationSampleRepository.class);
            assertThat(context.getBean(RiskSimulationSampleRepository.class))
                    .isInstanceOf(JdbcRiskSimulationSampleRepository.class);
            assertThat(context).hasSingleBean(RiskSimulationRunRepository.class);
            assertThat(context.getBean(RiskSimulationRunRepository.class))
                    .isInstanceOf(JdbcRiskSimulationRunRepository.class);
            assertThat(context).hasSingleBean(RiskDecisionLedger.class);
            assertThat(context.getBean(RiskDecisionLedger.class)).isInstanceOf(JdbcRiskDecisionLedger.class);
            assertThat(context).hasSingleBean(RiskDecisionTraceReader.class);
            assertThat(context.getBean(RiskDecisionTraceReader.class)).isInstanceOf(JdbcRiskDecisionTraceReader.class);
            assertThat(context).hasSingleBean(RiskFeatureStore.class);
            assertThat(context.getBean(RiskFeatureStore.class)).isInstanceOf(RedisRiskFeatureStore.class);
        });
    }
}
