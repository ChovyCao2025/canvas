package org.chovy.canvas.engine.channel;

import java.util.Map;

/**
 * WhatsAppCloudApiClient 定义 engine.channel 场景中的扩展契约。
 */
public interface WhatsAppCloudApiClient {

    /**
     * 调用 WhatsApp Cloud API 发送消息。
     *
     * @param phoneNumberId WhatsApp 号码 ID
     * @param accessToken Graph API 访问令牌
     * @param payload 供应商发送载荷
     * @return 供应商响应映射
     */
    Map<String, Object> sendMessage(String phoneNumberId,
                                    String accessToken,
                                    Map<String, Object> payload);
}
