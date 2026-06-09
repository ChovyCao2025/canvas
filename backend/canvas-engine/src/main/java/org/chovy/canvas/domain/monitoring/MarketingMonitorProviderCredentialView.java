package org.chovy.canvas.domain.monitoring;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * MarketingMonitorProviderCredentialView 承载 domain.monitoring 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param credentialKey credentialKey 字段。
 * @param providerType providerType 字段。
 * @param authType authType 字段。
 * @param displayName displayName 字段。
 * @param status status 字段。
 * @param tokenType tokenType 字段。
 * @param scopes scopes 字段。
 * @param accessTokenPrefix accessTokenPrefix 字段。
 * @param refreshTokenPrefix refreshTokenPrefix 字段。
 * @param apiKeyPrefix apiKeyPrefix 字段。
 * @param refreshEndpoint refreshEndpoint 字段。
 * @param revokeEndpoint revokeEndpoint 字段。
 * @param expiresAt expiresAt 字段。
 * @param refreshTokenExpiresAt refreshTokenExpiresAt 字段。
 * @param revokedAt revokedAt 字段。
 * @param lastRefreshedAt lastRefreshedAt 字段。
 * @param refreshAttemptCount refreshAttemptCount 字段。
 * @param lastRefreshStatus lastRefreshStatus 字段。
 * @param lastRefreshError lastRefreshError 字段。
 * @param lastRevokeStatus lastRevokeStatus 字段。
 * @param lastRevokeError lastRevokeError 字段。
 * @param metadata metadata 字段。
 * @param createdBy createdBy 字段。
 * @param updatedBy updatedBy 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
public record MarketingMonitorProviderCredentialView(
        Long id,
        Long tenantId,
        String credentialKey,
        String providerType,
        String authType,
        String displayName,
        String status,
        String tokenType,
        List<String> scopes,
        String accessTokenPrefix,
        String refreshTokenPrefix,
        String apiKeyPrefix,
        String refreshEndpoint,
        String revokeEndpoint,
        LocalDateTime expiresAt,
        LocalDateTime refreshTokenExpiresAt,
        LocalDateTime revokedAt,
        LocalDateTime lastRefreshedAt,
        int refreshAttemptCount,
        String lastRefreshStatus,
        String lastRefreshError,
        String lastRevokeStatus,
        String lastRevokeError,
        Map<String, Object> metadata,
        String createdBy,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
