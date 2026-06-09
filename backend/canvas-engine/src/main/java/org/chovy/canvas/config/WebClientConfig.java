package org.chovy.canvas.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Web Client Spring 配置类。
 *
 * <p>负责注册后端运行所需的 Bean、过滤器或基础设施参数，集中管理框架层装配逻辑。
 * <p>业务代码不应直接依赖配置细节，而应通过注入后的组件使用对应能力。
 */
@Configuration
public class WebClientConfig {

    /**
     * webClientBuilder 处理 config 场景的业务逻辑。
     * @param maxConnections max connections 参数，用于 webClientBuilder 流程中的校验、计算或对象转换。
     * @param pendingAcquireMaxCount pending acquire max count 参数，用于 webClientBuilder 流程中的校验、计算或对象转换。
     * @param pendingAcquireTimeoutMs 时间参数，用于计算窗口、过期或审计时间。
     * @param connectTimeoutMs 时间参数，用于计算窗口、过期或审计时间。
     * @param responseTimeoutMs 时间参数，用于计算窗口、过期或审计时间。
     * @param writeTimeoutMs 时间参数，用于计算窗口、过期或审计时间。
     * @param maxInMemoryBytes max in memory bytes 参数，用于 webClientBuilder 流程中的校验、计算或对象转换。
     * @param maxIdleSeconds max idle seconds 参数，用于 webClientBuilder 流程中的校验、计算或对象转换。
     * @param maxLifeMinutes max life minutes 参数，用于 webClientBuilder 流程中的校验、计算或对象转换。
     * @return 返回 webClientBuilder 流程生成的业务结果。
     */
    @Bean
    public WebClient.Builder webClientBuilder(
            @Value("${canvas.http-client.max-connections:500}") int maxConnections,
            @Value("${canvas.http-client.pending-acquire-max-count:2000}") int pendingAcquireMaxCount,
            @Value("${canvas.http-client.pending-acquire-timeout-ms:1000}") long pendingAcquireTimeoutMs,
            @Value("${canvas.http-client.connect-timeout-ms:1000}") int connectTimeoutMs,
            @Value("${canvas.http-client.response-timeout-ms:3000}") long responseTimeoutMs,
            @Value("${canvas.http-client.write-timeout-ms:3000}") long writeTimeoutMs,
            @Value("${canvas.http-client.max-in-memory-bytes:1048576}") int maxInMemoryBytes,
            @Value("${canvas.http-client.max-idle-sec:30}") long maxIdleSeconds,
            @Value("${canvas.http-client.max-life-min:5}") long maxLifeMinutes) {

        // 连接池参数集中在配置项，避免出站 API 节点高并发时无界建连。
        ConnectionProvider provider = ConnectionProvider.builder("canvas-http")
                .maxConnections(maxConnections)
                .pendingAcquireMaxCount(pendingAcquireMaxCount)
                .pendingAcquireTimeout(Duration.ofMillis(pendingAcquireTimeoutMs))
                .maxIdleTime(Duration.ofSeconds(maxIdleSeconds))
                .maxLifeTime(Duration.ofMinutes(maxLifeMinutes))
                .evictInBackground(Duration.ofSeconds(30))
                .build();

        // 同时设置连接、响应读取和写入超时，防止下游卡死拖住执行链路。
        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                .responseTimeout(Duration.ofMillis(responseTimeoutMs))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(responseTimeoutMs, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(writeTimeoutMs, TimeUnit.MILLISECONDS)));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(maxInMemoryBytes));
    }
}
