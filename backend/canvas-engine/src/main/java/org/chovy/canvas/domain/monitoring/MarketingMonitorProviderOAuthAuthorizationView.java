package org.chovy.canvas.domain.monitoring;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * MarketingMonitorProviderOAuthAuthorizationView 承载 domain.monitoring 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param authState authState 字段。
 * @param credentialKey credentialKey 字段。
 * @param providerType providerType 字段。
 * @param authType authType 字段。
 * @param displayName displayName 字段。
 * @param status status 字段。
 * @param authorizationUrl authorizationUrl 字段。
 * @param authorizeEndpoint authorizeEndpoint 字段。
 * @param tokenEndpoint tokenEndpoint 字段。
 * @param redirectUri redirectUri 字段。
 * @param scopes scopes 字段。
 * @param codeChallengeMethod codeChallengeMethod 字段。
 * @param credentialId credentialId 字段。
 * @param providerError providerError 字段。
 * @param providerErrorDescription providerErrorDescription 字段。
 * @param lastHttpStatus lastHttpStatus 字段。
 * @param lastErrorMessage lastErrorMessage 字段。
 * @param expiresAt expiresAt 字段。
 * @param completedAt completedAt 字段。
 * @param metadata metadata 字段。
 * @param createdBy createdBy 字段。
 * @param updatedBy updatedBy 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
public record MarketingMonitorProviderOAuthAuthorizationView(
        Long id,
        Long tenantId,
        String authState,
        String credentialKey,
        String providerType,
        String authType,
        String displayName,
        String status,
        String authorizationUrl,
        String authorizeEndpoint,
        String tokenEndpoint,
        String redirectUri,
        List<String> scopes,
        String codeChallengeMethod,
        Long credentialId,
        String providerError,
        String providerErrorDescription,
        Integer lastHttpStatus,
        String lastErrorMessage,
        LocalDateTime expiresAt,
        LocalDateTime completedAt,
        Map<String, Object> metadata,
        String createdBy,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
