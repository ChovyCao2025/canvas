package org.chovy.canvas.domain.monitoring;

import java.util.List;
import java.util.Map;

/**
 * MarketingMonitorAlertChannelCommand 承载 domain.monitoring 场景中的不可变数据快照。
 * @param channelKey channelKey 字段。
 * @param channelType channelType 字段。
 * @param displayName displayName 字段。
 * @param endpointUrl endpointUrl 字段。
 * @param enabled enabled 字段。
 * @param minSeverity minSeverity 字段。
 * @param alertTypes alertTypes 字段。
 * @param signingMode signingMode 字段。
 * @param secret secret 字段。
 * @param metadata metadata 字段。
 * @param maxAttempts maxAttempts 字段。
 */
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
