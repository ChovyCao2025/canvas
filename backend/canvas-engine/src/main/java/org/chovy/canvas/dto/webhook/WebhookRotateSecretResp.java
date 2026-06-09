package org.chovy.canvas.dto.webhook;

/**
 * WebhookRotateSecretResp 承载 dto.webhook 场景中的不可变数据快照。
 * @param subscriptionId subscriptionId 字段。
 * @param secret secret 字段。
 * @param secretPrefix secretPrefix 字段。
 */
public record WebhookRotateSecretResp(
        Long subscriptionId,
        String secret,
        String secretPrefix
) {
}
