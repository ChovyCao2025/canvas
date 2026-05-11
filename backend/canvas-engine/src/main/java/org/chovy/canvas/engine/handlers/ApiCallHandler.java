package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 接口调用节点：HTTP GET/POST，可选 validateResult 校验返回值。
 */
@Slf4j
@NodeHandlerType("API_CALL")
public class ApiCallHandler implements NodeHandler {

    private final WebClient webClient;

    public ApiCallHandler(@Value("${canvas.integration.api-call-base-url}") String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public NodeResult execute(Map<String, Object> config, ExecutionContext ctx) {
        String bizLineKey   = (String) config.get("bizLineKey");
        String apiKey       = (String) config.get("apiKey");
        List<Map<String, Object>> paramList = (List<Map<String, Object>>) config.getOrDefault("params", List.of());
        Boolean validateResult = (Boolean) config.get("validateResult");
        List<Map<String, Object>> rules = (List<Map<String, Object>>) config.get("validateRules");
        String nextNodeId = (String) config.get("nextNodeId");

        // 构建请求参数
        Map<String, Object> reqParams = new HashMap<>();
        reqParams.put("bizLineKey", bizLineKey);
        reqParams.put("apiKey", apiKey);
        for (Map<String, Object> p : paramList) {
            String key   = (String) p.get("paramKey");
            Object value = "CONTEXT".equals(p.get("valueType"))
                    ? ctx.getContextValue((String) p.get("value"))
                    : p.get("value");
            if (key != null) reqParams.put(key, value);
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = webClient.post()
                    .uri("/call")
                    .bodyValue(reqParams)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            Map<String, Object> output = resp != null ? new HashMap<>(resp) : new HashMap<>();

            // validateResult 校验
            if (Boolean.TRUE.equals(validateResult) && rules != null) {
                // 把 resp 写入临时 ctx 以供规则引用
                ExecutionContext tmpCtx = new ExecutionContext();
                tmpCtx.getTriggerPayload().putAll(ctx.getTriggerPayload());
                tmpCtx.getFlatContext().putAll(ctx.getFlatContext());
                if (resp != null) tmpCtx.getFlatContext().putAll(resp);

                for (Map<String, Object> rule : rules) {
                    if (!IfConditionHandler.evaluate(rule, tmpCtx)) {
                        return NodeResult.fail("接口校验规则不通过: " + rule.get("field"));
                    }
                }
            }

            return NodeResult.ok(nextNodeId, output);
        } catch (Exception e) {
            log.warn("[API_CALL] 调用失败 api={}: {}", apiKey, e.getMessage());
            return NodeResult.fail("接口调用异常: " + e.getMessage());
        }
    }
}
