package org.chovy.canvas.domain.monitoring;

import java.util.List;

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
