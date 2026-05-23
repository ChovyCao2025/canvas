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

    public CouponHandler(@Value("${canvas.integration.coupon-service-url}") String url) {
        // baseUrl 通过配置注入，便于多环境切换
        this.webClient = WebClient.builder().baseUrl(url).build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        // 节点配置：券种、附加参数和下游节点
        String couponTypeKey  = (String) config.get(MapFieldKeys.COUPON_TYPE_KEY);
        Map<String, Object> p = (Map<String, Object>) config.getOrDefault(MapFieldKeys.PARAMS, Map.of());
        String nextNodeId     = (String) config.get(MapFieldKeys.NEXT_NODE_ID);

        // 组装调用体：executionId:coupon 作为幂等键，避免重试重复发券
        Map<String, Object> body = new HashMap<>(p);
        body.put(MapFieldKeys.COUPON_TYPE_KEY, couponTypeKey);
        body.put(MapFieldKeys.USER_ID, ctx.getUserId());
        body.put(MapFieldKeys.IDEMPOTENCY_KEY, ctx.getExecutionId() + ":coupon");

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

    @Override
    public boolean isBenefitNode() {
        // 供统计侧识别“权益发放”节点
        return true;
    }
}
