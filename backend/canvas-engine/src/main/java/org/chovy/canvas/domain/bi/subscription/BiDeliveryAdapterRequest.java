package org.chovy.canvas.domain.bi.subscription;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * BiDeliveryAdapterRequest 承载 domain.bi.subscription 场景中的不可变数据快照。
 * @param tenantId tenantId 字段。
 * @param workspaceId workspaceId 字段。
 * @param jobType jobType 字段。
 * @param jobId jobId 字段。
 * @param jobKey jobKey 字段。
 * @param resourceType resourceType 字段。
 * @param resourceId resourceId 字段。
 * @param channel channel 字段。
 * @param receiver receiver 字段。
 * @param payload payload 字段。
 * @param metricValue metricValue 字段。
 * @param triggeredBy triggeredBy 字段。
 * @param attachments attachments 字段。
 */
public record BiDeliveryAdapterRequest(
        Long tenantId,
        Long workspaceId,
        String jobType,
        Long jobId,
        String jobKey,
        String resourceType,
        Long resourceId,
        String channel,
        Map<String, Object> receiver,
        Map<String, Object> payload,
        BigDecimal metricValue,
        String triggeredBy,
        List<BiEmailAttachment> attachments
) {
    /**
     * 创建 BiDeliveryAdapterRequest 实例并注入 domain.bi.subscription 场景依赖。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param jobType 类型标识，用于选择对应处理分支。
     * @param jobId 业务对象 ID，用于定位具体记录。
     * @param jobKey 业务键，用于在同一租户下定位资源。
     * @param resourceType 类型标识，用于选择对应处理分支。
     * @param resourceId 业务对象 ID，用于定位具体记录。
     * @param channel channel 参数，用于 BiDeliveryAdapterRequest 流程中的校验、计算或对象转换。
     * @param receiver receiver 参数，用于 BiDeliveryAdapterRequest 流程中的校验、计算或对象转换。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @param metricValue 待处理值，用于规则计算或转换。
     * @param triggeredBy triggered by 参数，用于 BiDeliveryAdapterRequest 流程中的校验、计算或对象转换。
     */
    public BiDeliveryAdapterRequest(Long tenantId,
                                    Long workspaceId,
                                    String jobType,
                                    Long jobId,
                                    String jobKey,
                                    String resourceType,
                                    Long resourceId,
                                    String channel,
                                    Map<String, Object> receiver,
                                    Map<String, Object> payload,
                                    BigDecimal metricValue,
                                    String triggeredBy) {
        this(tenantId,
                workspaceId,
                jobType,
                jobId,
                jobKey,
                resourceType,
                resourceId,
                channel,
                receiver,
                payload,
                metricValue,
                triggeredBy,
                List.of());
    }

    public BiDeliveryAdapterRequest {
        receiver = receiver == null ? Map.of() : Map.copyOf(receiver);
        payload = payload == null ? Map.of() : Map.copyOf(payload);
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
    }
}
