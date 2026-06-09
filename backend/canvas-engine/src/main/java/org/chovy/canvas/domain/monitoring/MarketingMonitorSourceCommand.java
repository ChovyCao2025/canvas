package org.chovy.canvas.domain.monitoring;

import java.util.Map;

/**
 * MarketingMonitorSourceCommand 承载 domain.monitoring 场景中的不可变数据快照。
 * @param sourceKey sourceKey 字段。
 * @param sourceType sourceType 字段。
 * @param displayName displayName 字段。
 * @param enabled enabled 字段。
 * @param metadata metadata 字段。
 */
public record MarketingMonitorSourceCommand(String sourceKey,
                                            String sourceType,
                                            String displayName,
                                            Boolean enabled,
                                            Map<String, Object> metadata) {
}
