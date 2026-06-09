package org.chovy.canvas.domain.monitoring;

import java.util.Map;

/**
 * MarketingMonitorProviderCredentialRevokeCommand 承载 domain.monitoring 场景中的不可变数据快照。
 * @param revokeEndpoint revokeEndpoint 字段。
 * @param tokenTypeHint tokenTypeHint 字段。
 * @param revokeRefreshToken revokeRefreshToken 字段。
 * @param disableAfterRevoke disableAfterRevoke 字段。
 * @param metadata metadata 字段。
 */
public record MarketingMonitorProviderCredentialRevokeCommand(
        String revokeEndpoint,
        String tokenTypeHint,
        Boolean revokeRefreshToken,
        Boolean disableAfterRevoke,
        Map<String, Object> metadata) {
}
