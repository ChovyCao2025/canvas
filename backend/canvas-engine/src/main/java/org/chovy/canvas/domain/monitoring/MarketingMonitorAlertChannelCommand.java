package org.chovy.canvas.domain.monitoring;

import java.util.List;
import java.util.Map;

public record MarketingMonitorAlertChannelCommand(String channelKey,
                                                  String channelType,
                                                  String displayName,
                                                  String endpointUrl,
                                                  Boolean enabled,
                                                  String minSeverity,
                                                  List<String> alertTypes,
                                                  String signingMode,
                                                  String secret,
                                                  Map<String, Object> metadata,
                                                  Integer maxAttempts) {

    public MarketingMonitorAlertChannelCommand {
        alertTypes = alertTypes == null ? List.of() : List.copyOf(alertTypes);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
