package com.photon.canvas.engine.handlers;

import com.photon.canvas.engine.context.ExecutionContext;
import com.photon.canvas.engine.handler.NodeHandler;
import com.photon.canvas.engine.handler.NodeHandlerType;
import com.photon.canvas.engine.handler.NodeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 代金券节点：HTTP 调用券系统，携带 idempotencyKey 防重。
 * 成功后设 ctx.benefitGranted = true。
 */
@Slf4j
@NodeHandlerType("COUPON")
public class CouponHandler implements NodeHandler {

    private final WebClient webClient;

    public CouponHandler(@Value("${canvas.integration.coupon-service-url}") String url) {
        this.webClient = WebClient.builder().baseUrl(url).build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public NodeResult execute(Map<String, Object> config, ExecutionContext ctx) {
        String couponTypeKey = (String) config.get("couponTypeKey");
        Map<String, Object> params = (Map<String, Object>) config.getOrDefault("params", Map.of());
        String nextNodeId = (String) config.get("nextNodeId");
        String idempotencyKey = ctx.getExecutionId() + ":" + "coupon";

        try {
            Map<String, Object> reqBody = new HashMap<>(params);
            reqBody.put("couponTypeKey", couponTypeKey);
            reqBody.put("userId", ctx.getUserId());
            reqBody.put("idempotencyKey", idempotencyKey);

            @SuppressWarnings("unchecked")
            Map<String, Object> resp = webClient.post()
                    .uri("/issue")
                    .bodyValue(reqBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (resp == null || !"SUCCESS".equals(resp.get("status"))) {
                return NodeResult.fail("发券失败: " + (resp != null ? resp.get("message") : "无响应"));
            }

            Map<String, Object> output = new HashMap<>();
            if (resp.get("couponId") != null)     output.put("couponId",     resp.get("couponId"));
            if (resp.get("couponAmount") != null)  output.put("couponAmount", resp.get("couponAmount"));

            return NodeResult.ok(nextNodeId, output);
        } catch (Exception e) {
            log.warn("[COUPON] 调用失败: {}", e.getMessage());
            return NodeResult.fail("券系统调用异常: " + e.getMessage());
        }
    }

    @Override
    public boolean isBenefitNode() { return true; }
}
