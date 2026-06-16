package org.chovy.canvas.boot;

import org.chovy.canvas.cdp.api.CdpEventIngestionFacade;
import org.chovy.canvas.cdp.application.CdpEventIngestionApplicationService;
import org.chovy.canvas.cdp.config.CdpDefaultPortConfig;
import org.chovy.canvas.cdp.domain.CdpAcceptedEventPublisher;
import org.chovy.canvas.cdp.domain.CdpEventAttributeDiscoveryPort;
import org.chovy.canvas.cdp.domain.CdpEventRepository;
import org.chovy.canvas.cdp.domain.CdpPrivacyTombstonePort;
import org.chovy.canvas.cdp.domain.CdpWarehouseEventSinkPort;
import org.chovy.canvas.cdp.domain.CustomerProfileRepository;
import org.chovy.canvas.conversation.adapter.persistence.ConversationContactProfileMapper;
import org.chovy.canvas.conversation.adapter.persistence.ConversationMessageMapper;
import org.chovy.canvas.conversation.adapter.persistence.ConversationRoutingAgentMapper;
import org.chovy.canvas.conversation.adapter.persistence.ConversationRoutingRuleMapper;
import org.chovy.canvas.conversation.adapter.persistence.ConversationSessionMapper;
import org.chovy.canvas.conversation.adapter.persistence.ConversationSlaBreachMapper;
import org.chovy.canvas.conversation.adapter.persistence.ConversationWorkItemAuditMapper;
import org.chovy.canvas.conversation.adapter.persistence.ConversationWorkItemMapper;
import org.chovy.canvas.conversation.api.ConversationFacade;
import org.chovy.canvas.conversation.domain.port.ConversationContactProfileRepository;
import org.chovy.canvas.conversation.domain.port.ConversationMessageRepository;
import org.chovy.canvas.conversation.domain.port.ConversationRoutingAgentRepository;
import org.chovy.canvas.conversation.domain.port.ConversationRoutingRuleRepository;
import org.chovy.canvas.conversation.domain.port.ConversationSessionRepository;
import org.chovy.canvas.conversation.domain.port.ConversationSlaBreachRepository;
import org.chovy.canvas.conversation.domain.port.ConversationWaitResumePort;
import org.chovy.canvas.conversation.domain.port.ConversationWorkItemAuditRepository;
import org.chovy.canvas.conversation.domain.port.ConversationWorkItemRepository;
import org.chovy.canvas.risk.adapter.persistence.RiskDecisionRunMapper;
import org.chovy.canvas.risk.adapter.persistence.RiskRuleHitMapper;
import org.chovy.canvas.risk.adapter.persistence.RiskStrategyMapper;
import org.chovy.canvas.risk.adapter.persistence.RiskStrategyVersionMapper;
import org.chovy.canvas.risk.api.RiskDecisionFacade;
import org.chovy.canvas.risk.adapter.external.JacksonRiskRuleJsonCodec;
import org.chovy.canvas.risk.application.RiskDecisionApplicationService;
import org.chovy.canvas.risk.config.RiskRuntimeConfig;
import org.chovy.canvas.risk.domain.runtime.RiskActiveStrategyReader;
import org.chovy.canvas.risk.domain.runtime.RiskDecisionLedger;
import org.chovy.canvas.risk.domain.runtime.RiskDecisionService;
import org.chovy.canvas.risk.domain.runtime.RiskRequestFeatureResolver;
import org.chovy.canvas.web.conversation.ConversationController;
import org.apache.ibatis.annotations.Mapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.mybatis.spring.annotation.MapperScan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CanvasBootApplicationSmokeTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ConversationWiringSmokeConfiguration.class, ConversationMapperMockConfiguration.class);

    @Test
    void conversationControllerFacadeChainStartsWithMapperMocksAndNoDatabase() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ConversationController.class);
            assertThat(context).hasSingleBean(ConversationFacade.class);
            assertThat(context).hasSingleBean(ConversationSessionRepository.class);
            assertThat(context).hasSingleBean(ConversationMessageRepository.class);
            assertThat(context).hasSingleBean(ConversationContactProfileRepository.class);
            assertThat(context).hasSingleBean(ConversationWorkItemRepository.class);
            assertThat(context).hasSingleBean(ConversationWorkItemAuditRepository.class);
            assertThat(context).hasSingleBean(ConversationRoutingAgentRepository.class);
            assertThat(context).hasSingleBean(ConversationRoutingRuleRepository.class);
            assertThat(context).hasSingleBean(ConversationSlaBreachRepository.class);
            assertThat(context).hasSingleBean(ConversationWaitResumePort.class);
        });
    }

    @Test
    void bootMapperScanOnlyIncludesInterfacesAnnotatedAsMybatisMappers() {
        MapperScan mapperScan = CanvasBootApplication.class.getAnnotation(MapperScan.class);

        assertThat(mapperScan).isNotNull();
        assertThat(mapperScan.basePackages()).contains("org.chovy.canvas");
        assertThat(mapperScan.annotationClass()).isEqualTo(Mapper.class);
    }

    @Test
    void cdpIngestionFacadeStartsWithDefaultPortsAndNoExternalPublisher() {
        new ApplicationContextRunner()
                .withUserConfiguration(CdpIngestionWiringSmokeConfiguration.class, CdpRepositoryMockConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(CdpEventIngestionFacade.class);
                    assertThat(context).hasSingleBean(CdpEventIngestionApplicationService.class);
                    assertThat(context).hasSingleBean(CdpAcceptedEventPublisher.class);
                    assertThat(context).hasSingleBean(CdpEventAttributeDiscoveryPort.class);
                    assertThat(context).hasSingleBean(CdpWarehouseEventSinkPort.class);
                    assertThat(context).hasSingleBean(CdpPrivacyTombstonePort.class);
                });
    }

    @Test
    void riskDecisionFacadeStartsWithRuntimePersistencePorts() {
        new ApplicationContextRunner()
                .withBean(com.fasterxml.jackson.databind.ObjectMapper.class)
                .withUserConfiguration(RiskDecisionWiringSmokeConfiguration.class, RiskMapperMockConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(RiskDecisionFacade.class);
                    assertThat(context).hasSingleBean(RiskDecisionApplicationService.class);
                    assertThat(context).hasSingleBean(RiskDecisionService.class);
                    assertThat(context).hasSingleBean(RiskActiveStrategyReader.class);
                    assertThat(context).hasSingleBean(RiskDecisionLedger.class);
                    assertThat(context).hasSingleBean(RiskRequestFeatureResolver.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @ComponentScan(basePackages = {
            "org.chovy.canvas.web.conversation",
            "org.chovy.canvas.conversation.application",
            "org.chovy.canvas.conversation.adapter.persistence",
            "org.chovy.canvas.conversation.config"
    })
    static class ConversationWiringSmokeConfiguration {
    }

    @Configuration(proxyBeanMethods = false)
    @Import({CdpEventIngestionApplicationService.class, CdpDefaultPortConfig.class})
    static class CdpIngestionWiringSmokeConfiguration {
    }

    @Configuration(proxyBeanMethods = false)
    @Import({RiskDecisionApplicationService.class, RiskRuntimeConfig.class, JacksonRiskRuleJsonCodec.class})
    static class RiskDecisionWiringSmokeConfiguration {
    }

    @Configuration(proxyBeanMethods = false)
    static class ConversationMapperMockConfiguration {

        @Bean
        ConversationSessionMapper conversationSessionMapper() {
            return mock(ConversationSessionMapper.class);
        }

        @Bean
        ConversationMessageMapper conversationMessageMapper() {
            return mock(ConversationMessageMapper.class);
        }

        @Bean
        ConversationContactProfileMapper conversationContactProfileMapper() {
            return mock(ConversationContactProfileMapper.class);
        }

        @Bean
        ConversationWorkItemMapper conversationWorkItemMapper() {
            return mock(ConversationWorkItemMapper.class);
        }

        @Bean
        ConversationWorkItemAuditMapper conversationWorkItemAuditMapper() {
            return mock(ConversationWorkItemAuditMapper.class);
        }

        @Bean
        ConversationRoutingAgentMapper conversationRoutingAgentMapper() {
            return mock(ConversationRoutingAgentMapper.class);
        }

        @Bean
        ConversationRoutingRuleMapper conversationRoutingRuleMapper() {
            return mock(ConversationRoutingRuleMapper.class);
        }

        @Bean
        ConversationSlaBreachMapper conversationSlaBreachMapper() {
            return mock(ConversationSlaBreachMapper.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CdpRepositoryMockConfiguration {

        @Bean
        CdpEventRepository cdpEventRepository() {
            return mock(CdpEventRepository.class);
        }

        @Bean
        CustomerProfileRepository customerProfileRepository() {
            return mock(CustomerProfileRepository.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class RiskMapperMockConfiguration {

        @Bean
        RiskStrategyMapper riskStrategyMapper() {
            return mock(RiskStrategyMapper.class);
        }

        @Bean
        RiskStrategyVersionMapper riskStrategyVersionMapper() {
            return mock(RiskStrategyVersionMapper.class);
        }

        @Bean
        RiskDecisionRunMapper riskDecisionRunMapper() {
            return mock(RiskDecisionRunMapper.class);
        }

        @Bean
        RiskRuleHitMapper riskRuleHitMapper() {
            return mock(RiskRuleHitMapper.class);
        }
    }
}
