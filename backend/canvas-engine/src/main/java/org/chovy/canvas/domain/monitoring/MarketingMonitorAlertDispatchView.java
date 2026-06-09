package org.chovy.canvas.domain.monitoring;

import java.util.List;

/**
 * MarketingMonitorAlertDispatchView 承载 domain.monitoring 场景中的不可变数据快照。
 * @param tenantId tenantId 字段。
 * @param alertId alertId 字段。
 * @param attempted attempted 字段。
 * @param delivered delivered 字段。
 * @param failed failed 字段。
 * @param deliveries deliveries 字段。
 */
public record MarketingMonitorAlertDispatchView(Long tenantId,
                                                Long alertId,
                                                int attempted,
                                                int delivered,
                                                int failed,
                                                List<MarketingMonitorAlertDeliveryView> deliveries) {

    public MarketingMonitorAlertDispatchView {
        deliveries = deliveries == null ? List.of() : List.copyOf(deliveries);
    }
}
