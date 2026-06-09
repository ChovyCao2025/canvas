package org.chovy.canvas.engine.channel;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

/**
 * HttpWhatsAppCloudApiClient 参与 engine.channel 场景的画布执行引擎处理。
 */
@Component
public class HttpWhatsAppCloudApiClient implements WhatsAppCloudApiClient {

    private final WebClient webClient;
    private final String apiVersion;
    private final Duration timeout;

    /**
     * 创建 HttpWhatsAppCloudApiClient 实例并注入 engine.channel 场景依赖。
     * @param webClientBuilder 依赖组件，用于完成数据访问或外部能力调用。
     * @param graphBaseUrl graph base url 参数，用于 HttpWhatsAppCloudApiClient 流程中的校验、计算或对象转换。
     * @param apiVersion api version 参数，用于 HttpWhatsAppCloudApiClient 流程中的校验、计算或对象转换。
     * @param timeoutMs 时间参数，用于计算窗口、过期或审计时间。
     */
    public HttpWhatsAppCloudApiClient(WebClient.Builder webClientBuilder,
                                      @Value("${canvas.conversation.whatsapp.cloud.graph-base-url:https://graph.facebook.com}")
                                      String graphBaseUrl,
                                      @Value("${canvas.conversation.whatsapp.cloud.graph-api-version:}") String apiVersion,
                                      @Value("${canvas.conversation.whatsapp.cloud.timeout-ms:10000}") long timeoutMs) {
        this.webClient = (webClientBuilder == null ? WebClient.builder() : webClientBuilder)
                .baseUrl(blankToDefault(graphBaseUrl, "https://graph.facebook.com"))
                .build();
        this.apiVersion = apiVersion == null ? "" : apiVersion.trim();
        this.timeout = Duration.ofMillis(Math.max(timeoutMs, 1L));
    }

    /**
     * sendMessage 创建或触发 engine.channel 场景的业务处理。
     * @param phoneNumberId 业务对象 ID，用于定位具体记录。
     * @param accessToken 令牌或锁标识，用于鉴权、幂等或并发控制。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 sendMessage 流程生成的业务结果。
     */
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> sendMessage(String phoneNumberId,
                                           String accessToken,
                                           Map<String, Object> payload) {
        if (apiVersion.isBlank()) {
            throw new IllegalStateException("WhatsApp Graph API version is not configured");
        }
        Object response = webClient.post()
                .uri("/{version}/{phoneNumberId}/messages", apiVersion, phoneNumberId)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .bodyValue(payload == null ? Map.of() : payload)
                .retrieve()
                .bodyToMono(Map.class)
                .block(timeout);
        return response instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    /**
     * 将空白配置值替换为默认值。
     *
     * @param value 原始配置值
     * @param fallback 默认值
     * @return 非空配置值
     */
    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
