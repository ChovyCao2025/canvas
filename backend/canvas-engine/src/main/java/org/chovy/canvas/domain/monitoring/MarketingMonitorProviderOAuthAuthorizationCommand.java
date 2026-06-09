package org.chovy.canvas.domain.monitoring;

import java.util.List;
import java.util.Map;

/**
 * MarketingMonitorProviderOAuthAuthorizationCommand 承载 domain.monitoring 场景中的不可变数据快照。
 * @param credentialKey credentialKey 字段。
 * @param providerType providerType 字段。
 * @param authType authType 字段。
 * @param displayName displayName 字段。
 * @param authorizeEndpoint authorizeEndpoint 字段。
 * @param tokenEndpoint tokenEndpoint 字段。
 * @param revokeEndpoint revokeEndpoint 字段。
 * @param redirectUri redirectUri 字段。
 * @param clientId clientId 字段。
 * @param clientSecret clientSecret 字段。
 * @param scopes scopes 字段。
 * @param authorizeParams authorizeParams 字段。
 * @param expiresInMinutes expiresInMinutes 字段。
 * @param metadata metadata 字段。
 */
public record MarketingMonitorProviderOAuthAuthorizationCommand(
        String credentialKey,
        String providerType,
        String authType,
        String displayName,
        String authorizeEndpoint,
        String tokenEndpoint,
        String revokeEndpoint,
        String redirectUri,
        String clientId,
        String clientSecret,
        List<String> scopes,
        Map<String, Object> authorizeParams,
        Integer expiresInMinutes,
        Map<String, Object> metadata) {
}
