package org.chovy.canvas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Redis 相关 Bean 配置。
 *
 * <p>区分 reactive 与 blocking 模板，避免在响应式链路中误用阻塞 API。
 * 该类仅做 Bean 装配，不承载业务缓存 key 规则。
 *
 * 读代码提示：
 * - key 命名规则在各业务服务中定义（如 trigger/quota/context）；
 * - 本类只解决“用什么客户端实例访问 Redis”。
 */
@Configuration
public class RedisConfig {

    /**
     * ReactiveStringRedisTemplate：用于 Pub/Sub 缓存失效广播（12.7节）。
     * StringRedisTemplate（blocking）用于普通 K/V 操作。
     */
    @Bean
    public ReactiveStringRedisTemplate reactiveStringRedisTemplate(
            ReactiveRedisConnectionFactory factory) {
        // 用于 Pub/Sub 与轻量 reactive KV 访问
        // 业务上仍可按需注入 StringRedisTemplate 做阻塞查询
        return new ReactiveStringRedisTemplate(factory);
    }

    /**
     * redisMessageListenerContainer 处理 config 场景的业务逻辑。
     * @param factory 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @return 返回 redis message listener container 汇总后的集合、分页或映射视图。
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory factory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        return container;
    }
}
