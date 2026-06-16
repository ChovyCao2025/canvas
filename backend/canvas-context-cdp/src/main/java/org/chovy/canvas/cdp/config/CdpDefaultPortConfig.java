package org.chovy.canvas.cdp.config;

import org.chovy.canvas.cdp.domain.CdpAcceptedEventPublisher;
import org.chovy.canvas.cdp.domain.CdpEventAttributeDiscoveryPort;
import org.chovy.canvas.cdp.domain.CdpEventDefinitionRepository;
import org.chovy.canvas.cdp.domain.CdpPrivacyTombstonePort;
import org.chovy.canvas.cdp.domain.CdpWarehouseEventSinkPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置 CdpDefaultPort 相关默认端口实现。
 */
@Configuration
public class CdpDefaultPortConfig {

    /**
     * 执行 emptyEventDefinitionRepository 对应的 CDP 业务操作。
     */
    @Bean
    @ConditionalOnMissingBean
    CdpEventDefinitionRepository emptyEventDefinitionRepository() {
        return eventCode -> null;
    }

    /**
     * 执行 noopEventAttributeDiscoveryPort 对应的 CDP 业务操作。
     */
    @Bean
    @ConditionalOnMissingBean
    CdpEventAttributeDiscoveryPort noopEventAttributeDiscoveryPort() {
        return CdpEventAttributeDiscoveryPort.noop();
    }

    /**
     * 执行 noopAcceptedEventPublisher 对应的 CDP 业务操作。
     */
    @Bean
    @ConditionalOnMissingBean
    CdpAcceptedEventPublisher noopAcceptedEventPublisher() {
        return CdpAcceptedEventPublisher.noop();
    }

    /**
     * 执行 noopWarehouseEventSinkPort 对应的 CDP 业务操作。
     */
    @Bean
    @ConditionalOnMissingBean
    CdpWarehouseEventSinkPort noopWarehouseEventSinkPort() {
        return CdpWarehouseEventSinkPort.noop();
    }

    /**
     * 执行 allowAllPrivacyTombstonePort 对应的 CDP 业务操作。
     */
    @Bean
    @ConditionalOnMissingBean
    CdpPrivacyTombstonePort allowAllPrivacyTombstonePort() {
        return CdpPrivacyTombstonePort.allowAll();
    }
}
