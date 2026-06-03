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
import java.util.*;

/**
 * 发券节点处理器（COUPON）。
 *
 * <p>职责：
 * 1) 组装发券请求（券类型、用户、幂等键）；
 * 2) 调用券系统；
 * 3) 把关键结果（couponId/couponAmount）写入上下文输出。
 */
@Slf4j
@Component
@NodeHandlerType("COUPON")
public class CouponHandler implements NodeHandler {

    /** 券系统 HTTP 客户端。 */
    private final WebClient webClient;

    public CouponHandler(WebClient.Builder webClientBuilder,
                         @Value("${canvas.integration.coupon-service-url}") String url) {
        // baseUrl 通过配置注入，便于多环境切换
        this.webClient = webClientBuilder.clone().baseUrl(url).build();
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
    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        // 节点配置：券种、附加参数和下游节点
        String couponTypeKey  = (String) config.get(MapFieldKeys.COUPON_TYPE_KEY);
        if (couponTypeKey == null || couponTypeKey.isBlank()) {
            return Mono.just(NodeResult.fail("COUPON: couponTypeKey 未配置"));
        }
        Map<String, Object> p = (Map<String, Object>) config.getOrDefault(MapFieldKeys.PARAMS, Map.of());
        String nextNodeId     = (String) config.get(MapFieldKeys.NEXT_NODE_ID);

        String nodeId = (String) config.getOrDefault(MapFieldKeys.NODE_ID_INTERNAL, "coupon");
        String idempotencyKey = (String) config.getOrDefault(MapFieldKeys.IDEMPOTENCY_KEY,
                ctx.getExecutionId() + ":" + nodeId);

        // 组装调用体：默认使用 executionId:nodeId 作为幂等键，避免同一画布多个券节点互相冲突
        Map<String, Object> body = new HashMap<>(p);
        body.put(MapFieldKeys.COUPON_TYPE_KEY, couponTypeKey);
        body.put(MapFieldKeys.USER_ID, ctx.getUserId());
        body.put(MapFieldKeys.IDEMPOTENCY_KEY, idempotencyKey);

        return webClient.post().uri("/issue").bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(resp -> {
                    // 约定 status=SUCCESS 才视为业务成功
                    if (!"SUCCESS".equals(resp.get("status")))
                        return NodeResult.fail("发券失败: " + resp.get("message"));
                    Map<String, Object> out = new HashMap<>();
                    // 常用发券结果字段写入上下文，供后续触达/回传节点使用
                    if (resp.get("couponId")    != null) out.put("couponId",    resp.get("couponId"));
                    if (resp.get("couponAmount") != null) out.put("couponAmount", resp.get("couponAmount"));
                    return NodeResult.ok(nextNodeId, out);
                })
                .onErrorResume(e -> {
                    log.warn("[COUPON] 调用失败: {}", e.getMessage());
                    return Mono.just(NodeResult.fail("券系统调用异常: " + e.getMessage()));
                });
    }

    /**
     * 判断 is Benefit Node 相关的业务数据。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @return 判断结果，true 表示校验通过或条件成立
     */
    @Override
    public boolean isBenefitNode() {
        // 供统计侧识别“权益发放”节点
        return true;
    }
}
