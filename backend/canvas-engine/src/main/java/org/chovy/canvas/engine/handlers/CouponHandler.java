package org.chovy.canvas.engine.handlers;

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

@Slf4j @Component @NodeHandlerType("COUPON")
public class CouponHandler implements NodeHandler {

    private final WebClient webClient;
    public CouponHandler(@Value("${canvas.integration.coupon-service-url}") String url) {
        this.webClient = WebClient.builder().baseUrl(url).build();
    }

    @Override @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String couponTypeKey  = (String) config.get("couponTypeKey");
        Map<String, Object> p = (Map<String, Object>) config.getOrDefault("params", Map.of());
        String nextNodeId     = (String) config.get("nextNodeId");

        Map<String, Object> body = new HashMap<>(p);
        body.put("couponTypeKey",  couponTypeKey);
        body.put("userId",         ctx.getUserId());
        body.put("idempotencyKey", ctx.getExecutionId() + ":coupon");

        return webClient.post().uri("/issue").bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(resp -> {
                    if (!"SUCCESS".equals(resp.get("status")))
                        return NodeResult.fail("发券失败: " + resp.get("message"));
                    Map<String, Object> out = new HashMap<>();
                    if (resp.get("couponId")    != null) out.put("couponId",    resp.get("couponId"));
                    if (resp.get("couponAmount") != null) out.put("couponAmount", resp.get("couponAmount"));
                    return NodeResult.ok(nextNodeId, out);
                })
                .onErrorResume(e -> {
                    log.warn("[COUPON] 调用失败: {}", e.getMessage());
                    return Mono.just(NodeResult.fail("券系统调用异常: " + e.getMessage()));
                });
    }
    @Override public boolean isBenefitNode() { return true; }
}
