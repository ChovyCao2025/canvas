package org.chovy.canvas.conversation.config;

import org.chovy.canvas.conversation.domain.port.ConversationWaitResumePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 为会话上下文模块提供可被业务侧覆盖的默认端口实现。
 */
@Configuration(proxyBeanMethods = false)
public class ConversationDefaultPortConfig {

    /**
     * 提供默认的等待恢复端口，未接入引擎恢复能力时保持无副作用。
     *
     * @return 默认等待恢复端口
     */
    @Bean
    @ConditionalOnMissingBean(ConversationWaitResumePort.class)
    ConversationWaitResumePort conversationWaitResumePort() {
        return (eventCode, subject, attributes, eventId) -> 0;
    }
}
