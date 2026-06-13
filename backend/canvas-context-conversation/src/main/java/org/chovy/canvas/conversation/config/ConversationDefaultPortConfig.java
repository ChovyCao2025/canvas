package org.chovy.canvas.conversation.config;

import org.chovy.canvas.conversation.domain.port.ConversationWaitResumePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ConversationDefaultPortConfig {

    @Bean
    @ConditionalOnMissingBean(ConversationWaitResumePort.class)
    ConversationWaitResumePort conversationWaitResumePort() {
        return (eventCode, subject, attributes, eventId) -> 0;
    }
}
