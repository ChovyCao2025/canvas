package org.chovy.canvas.infrastructure.http;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * WebClient-backed adapter for outbound JSON integrations.
 */
@Component
public class WebClientExternalHttpClient implements ExternalHttpClient {

    private final WebClient reachPlatformClient;

    /**
     * 创建 WebClientExternalHttpClient 实例并注入 infrastructure.http 场景依赖。
     * @param webClientBuilder 依赖组件，用于完成数据访问或外部能力调用。
     * @param reachPlatformUrl reach platform url 参数，用于 WebClientExternalHttpClient 流程中的校验、计算或对象转换。
     */
    public WebClientExternalHttpClient(
            WebClient.Builder webClientBuilder,
            @Value("${canvas.integration.reach-platform-url}") String reachPlatformUrl
    ) {
        this.reachPlatformClient = webClientBuilder.clone().baseUrl(reachPlatformUrl).build();
    }

    /**
     * postJson 处理 infrastructure.http 场景的业务逻辑。
     * @param integrationName 名称文本，用于展示或唯一性校验。
     * @param path path 参数，用于 postJson 流程中的校验、计算或对象转换。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 postJson 流程生成的业务结果。
     */
    @Override
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> postJson(String integrationName, String path, Map<String, Object> payload) {
        WebClient client = switch (integrationName) {
            case ExternalHttpClient.REACH_PLATFORM -> reachPlatformClient;
            default -> throw new IllegalArgumentException("Unknown external integration: " + integrationName);
        };
        return client.post()
                .uri(path)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .map(map -> (Map<String, Object>) map);
    }
}
