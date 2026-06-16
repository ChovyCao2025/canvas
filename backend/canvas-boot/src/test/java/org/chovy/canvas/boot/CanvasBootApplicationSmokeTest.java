package org.chovy.canvas.boot;

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
import org.chovy.canvas.web.conversation.ConversationController;
import org.apache.ibatis.annotations.Mapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.mybatis.spring.annotation.MapperScan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 验证 Canvas Boot 启动入口的关键 Spring 扫描与会话域装配约束。
 */
class CanvasBootApplicationSmokeTest {

    /**
     * 仅加载会话域相关组件和 Mapper Mock 的轻量级 Spring 上下文运行器。
     */
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ConversationWiringSmokeConfiguration.class, ConversationMapperMockConfiguration.class);

    /**
     * 验证会话控制器到领域端口的 Bean 链路可以在无数据库连接时完成装配。
     */
    @Test
    void conversationControllerFacadeChainStartsWithMapperMocksAndNoDatabase() {
        // 仅用 Mapper Mock 替代数据库访问，避免 smoke test 依赖外部 MySQL。
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

    /**
     * 验证 Boot 入口只扫描显式标注为 MyBatis Mapper 的接口。
     */
    @Test
    void bootMapperScanOnlyIncludesInterfacesAnnotatedAsMybatisMappers() {
        MapperScan mapperScan = CanvasBootApplication.class.getAnnotation(MapperScan.class);

        assertThat(mapperScan).isNotNull();
        assertThat(mapperScan.basePackages()).contains("org.chovy.canvas");
        assertThat(mapperScan.annotationClass()).isEqualTo(Mapper.class);
    }

    /**
     * 会话域装配 smoke test 使用的最小组件扫描配置。
     */
    @Configuration(proxyBeanMethods = false)
    @ComponentScan(basePackages = {
            "org.chovy.canvas.web.conversation",
            "org.chovy.canvas.conversation.application",
            "org.chovy.canvas.conversation.adapter.persistence",
            "org.chovy.canvas.conversation.config"
    })
    static class ConversationWiringSmokeConfiguration {
    }

    /**
     * 为会话持久化适配器提供 Mapper Mock 的测试配置。
     */
    @Configuration(proxyBeanMethods = false)
    static class ConversationMapperMockConfiguration {

        /**
         * 提供会话 Mapper 的 Mock Bean。
         *
         * @return 会话 Mapper Mock
         */
        @Bean
        ConversationSessionMapper conversationSessionMapper() {
            return mock(ConversationSessionMapper.class);
        }

        /**
         * 提供会话消息 Mapper 的 Mock Bean。
         *
         * @return 会话消息 Mapper Mock
         */
        @Bean
        ConversationMessageMapper conversationMessageMapper() {
            return mock(ConversationMessageMapper.class);
        }

        /**
         * 提供联系人画像 Mapper 的 Mock Bean。
         *
         * @return 联系人画像 Mapper Mock
         */
        @Bean
        ConversationContactProfileMapper conversationContactProfileMapper() {
            return mock(ConversationContactProfileMapper.class);
        }

        /**
         * 提供会话工作项 Mapper 的 Mock Bean。
         *
         * @return 会话工作项 Mapper Mock
         */
        @Bean
        ConversationWorkItemMapper conversationWorkItemMapper() {
            return mock(ConversationWorkItemMapper.class);
        }

        /**
         * 提供会话工作项审计 Mapper 的 Mock Bean。
         *
         * @return 会话工作项审计 Mapper Mock
         */
        @Bean
        ConversationWorkItemAuditMapper conversationWorkItemAuditMapper() {
            return mock(ConversationWorkItemAuditMapper.class);
        }

        /**
         * 提供路由坐席 Mapper 的 Mock Bean。
         *
         * @return 路由坐席 Mapper Mock
         */
        @Bean
        ConversationRoutingAgentMapper conversationRoutingAgentMapper() {
            return mock(ConversationRoutingAgentMapper.class);
        }

        /**
         * 提供路由规则 Mapper 的 Mock Bean。
         *
         * @return 路由规则 Mapper Mock
         */
        @Bean
        ConversationRoutingRuleMapper conversationRoutingRuleMapper() {
            return mock(ConversationRoutingRuleMapper.class);
        }

        /**
         * 提供 SLA 违约记录 Mapper 的 Mock Bean。
         *
         * @return SLA 违约记录 Mapper Mock
         */
        @Bean
        ConversationSlaBreachMapper conversationSlaBreachMapper() {
            return mock(ConversationSlaBreachMapper.class);
        }
    }
}
