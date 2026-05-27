package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.Map;

/**
 * Tagger 离线标签节点（TAGGER_OFFLINE）。
 *
 * <p>调用标签系统离线查询接口，根据返回标签值决定是否放行：
 * - 为空：视为不满足，拦截流程；
 * - 配置了 expectedVal 且不相等：拦截流程；
 * - 其余情况：继续执行并输出 tagValue。
 * 该节点用于“离线标签判定”，不消费实时标签流。
 */
@Slf4j @Component @NodeHandlerType("TAGGER_OFFLINE")
public class TaggerOfflineHandler implements NodeHandler {

    /** Tagger 离线查询客户端。 */
    private final WebClient webClient;
    public TaggerOfflineHandler(@Value("${canvas.integration.tagger-service-url}") String url) {
        // 与实时标签服务共用同一基础地址时，可通过路由路径区分接口
        this.webClient = WebClient.builder().baseUrl(url).build();
    }

    /**
     * 执行当前节点或服务的核心处理流程。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @Override @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        // tagCodeKey 表示标签编码，params.tagValue 可作为期望值做二次校验
        String tagCodeKey   = (String) config.get("tagCodeKey");
        Map<String, Object> params = (Map<String, Object>) config.getOrDefault("params", Map.of());
        String nextNodeId   = (String) config.get(MapFieldKeys.NEXT_NODE_ID);
        String expectedVal  = params.get("tagValue") != null ? params.get("tagValue").toString() : null;

        return webClient.get()
                .uri(u -> u.path("/offline").queryParam("tagCode", tagCodeKey).queryParam("userId", ctx.getUserId()).build())
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(resp -> {
                    Object tagValue = resp.get("tagValue");
                    // 离线标签不存在时按未命中处理，直接失败终止
                    if (tagValue == null || tagValue.toString().isBlank())
                        return Mono.just(NodeResult.fail("Tagger 离线标签为空，拦截流程"));
                    // 配置了期望值则必须严格匹配
                    if (expectedVal != null && !expectedVal.equals(tagValue.toString()))
                        return Mono.just(NodeResult.fail("Tagger 离线标签值不匹配"));
                    // 把命中的标签值写回上下文，供下游条件节点复用
                    return Mono.just(NodeResult.ok(nextNodeId, Map.of(MapFieldKeys.TAG_VALUE, tagValue)));
                })
                .onErrorResume(e -> {
                    log.warn("[TAGGER_OFFLINE] 调用失败: {}", e.getMessage());
                    return Mono.just(NodeResult.fail("Tagger 查询异常: " + e.getMessage()));
                });
    }
}
