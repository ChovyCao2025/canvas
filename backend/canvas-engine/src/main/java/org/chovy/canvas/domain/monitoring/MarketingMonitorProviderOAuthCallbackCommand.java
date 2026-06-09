package org.chovy.canvas.domain.monitoring;

import java.util.Map;

/**
 * MarketingMonitorProviderOAuthCallbackCommand 承载 domain.monitoring 场景中的不可变数据快照。
 * @param state state 字段。
 * @param code code 字段。
 * @param error error 字段。
 * @param errorDescription errorDescription 字段。
 * @param metadata metadata 字段。
 */
public record MarketingMonitorProviderOAuthCallbackCommand(
        String state,
        String code,
        String error,
        String errorDescription,
        Map<String, Object> metadata) {
}
