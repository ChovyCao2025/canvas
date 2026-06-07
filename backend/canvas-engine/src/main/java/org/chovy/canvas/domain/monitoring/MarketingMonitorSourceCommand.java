package org.chovy.canvas.domain.monitoring;

import java.util.Map;

public record MarketingMonitorSourceCommand(String sourceKey,
                                            String sourceType,
                                            String displayName,
                                            Boolean enabled,
                                            Map<String, Object> metadata) {
}
