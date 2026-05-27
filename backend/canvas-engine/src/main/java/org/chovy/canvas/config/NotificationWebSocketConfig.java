package org.chovy.canvas.config;

import org.chovy.canvas.domain.notification.NotificationWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.Map;

/**
 * 通知消息 Web Socket Spring 配置类。
 *
 * <p>负责注册后端运行所需的 Bean、过滤器或基础设施参数，集中管理框架层装配逻辑。
 * <p>业务代码不应直接依赖配置细节，而应通过注入后的组件使用对应能力。
 */
@Configuration
public class NotificationWebSocketConfig {

    /**
     * 创建并注册 notification Web Socket Mapping 相关的 Spring Bean。
     *
     * <p>该方法在应用启动时由 Spring 容器调用，用于装配运行依赖。
     *
     * @param handler handler 方法执行所需的业务参数
     * @return 方法执行后的业务结果
     */
    @Bean
    public HandlerMapping notificationWebSocketMapping(NotificationWebSocketHandler handler) {
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setOrder(-1);
        mapping.setUrlMap(Map.of("/canvas/ws/notifications", handler));
        return mapping;
    }

    /**
     * 创建并注册 web Socket Handler Adapter 相关的 Spring Bean。
     *
     * <p>该方法在应用启动时由 Spring 容器调用，用于装配运行依赖。
     *
     * @return 方法执行后的业务结果
     */
    @Bean
    public WebSocketHandlerAdapter webSocketHandlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
