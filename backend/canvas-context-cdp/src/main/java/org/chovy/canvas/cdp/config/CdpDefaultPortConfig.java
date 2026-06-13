package org.chovy.canvas.cdp.config;

import org.chovy.canvas.cdp.domain.CdpAcceptedEventPublisher;
import org.chovy.canvas.cdp.domain.CdpEventAttributeDiscoveryPort;
import org.chovy.canvas.cdp.domain.CdpEventDefinitionRepository;
import org.chovy.canvas.cdp.domain.CdpPrivacyTombstonePort;
import org.chovy.canvas.cdp.domain.CdpWarehouseEventSinkPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CdpDefaultPortConfig {

    @Bean
    @ConditionalOnMissingBean
    CdpEventDefinitionRepository emptyEventDefinitionRepository() {
        return eventCode -> null;
    }

    @Bean
    @ConditionalOnMissingBean
    CdpEventAttributeDiscoveryPort noopEventAttributeDiscoveryPort() {
        return CdpEventAttributeDiscoveryPort.noop();
    }

    @Bean
    @ConditionalOnMissingBean
    CdpAcceptedEventPublisher noopAcceptedEventPublisher() {
        return CdpAcceptedEventPublisher.noop();
    }

    @Bean
    @ConditionalOnMissingBean
    CdpWarehouseEventSinkPort noopWarehouseEventSinkPort() {
        return CdpWarehouseEventSinkPort.noop();
    }

    @Bean
    @ConditionalOnMissingBean
    CdpPrivacyTombstonePort allowAllPrivacyTombstonePort() {
        return CdpPrivacyTombstonePort.allowAll();
    }
}
