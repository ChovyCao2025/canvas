package org.chovy.canvas.domain.cdp;

import java.util.Map;

/**
 * WebhookDeliveryPayload 承载 domain.cdp 场景中的不可变数据快照。
 * @param schemaVersion schemaVersion 字段。
 * @param eventType eventType 字段。
 * @param deliveryId deliveryId 字段。
 * @param data data 字段。
 */
public record WebhookDeliveryPayload(
        String schemaVersion,
        String eventType,
        String deliveryId,
        Map<String, Object> data
) {
    /**
     * of 处理 domain.cdp 场景的业务逻辑。
     * @param eventType 类型标识，用于选择对应处理分支。
     * @param deliveryId 业务对象 ID，用于定位具体记录。
     * @param data data 参数，用于 of 流程中的校验、计算或对象转换。
     * @return 返回 of 流程生成的业务结果。
     */
    public static WebhookDeliveryPayload of(String eventType, String deliveryId, Map<String, Object> data) {
        return new WebhookDeliveryPayload("2026-06-03", eventType, deliveryId, data);
    }
}
