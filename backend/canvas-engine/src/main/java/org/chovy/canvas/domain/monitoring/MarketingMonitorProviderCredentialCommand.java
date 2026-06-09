package org.chovy.canvas.domain.monitoring;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * MarketingMonitorProviderCredentialCommand 承载 domain.monitoring 场景中的不可变数据快照。
 * @param credentialKey credentialKey 字段。
 * @param providerType providerType 字段。
 * @param authType authType 字段。
 * @param displayName displayName 字段。
 * @param enabled enabled 字段。
 * @param accessToken accessToken 字段。
 * @param refreshToken refreshToken 字段。
 * @param apiKey apiKey 字段。
 * @param tokenType tokenType 字段。
 * @param scopes scopes 字段。
 * @param expiresAt expiresAt 字段。
 * @param refreshTokenExpiresAt refreshTokenExpiresAt 字段。
 * @param refreshEndpoint refreshEndpoint 字段。
 * @param revokeEndpoint revokeEndpoint 字段。
 * @param clientId clientId 字段。
 * @param clientSecret clientSecret 字段。
 * @param metadata metadata 字段。
 */
public record MarketingMonitorProviderCredentialCommand(
        String credentialKey,
        String providerType,
        String authType,
        String displayName,
        Boolean enabled,
        String accessToken,
        String refreshToken,
        String apiKey,
        String tokenType,
        List<String> scopes,
        LocalDateTime expiresAt,
        LocalDateTime refreshTokenExpiresAt,
        String refreshEndpoint,
        String revokeEndpoint,
        String clientId,
        String clientSecret,
        Map<String, Object> metadata) {
}
