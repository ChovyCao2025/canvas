package org.chovy.canvas.domain.marketing;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * MarketingIntegrationContractView 承载 domain.marketing 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param contractKey contractKey 字段。
 * @param displayName displayName 字段。
 * @param providerFamily providerFamily 字段。
 * @param sourceCapabilityKey sourceCapabilityKey 字段。
 * @param targetCapabilityKey targetCapabilityKey 字段。
 * @param assetKey assetKey 字段。
 * @param direction direction 字段。
 * @param environment environment 字段。
 * @param authMode authMode 字段。
 * @param credentialDependency credentialDependency 字段。
 * @param apiRoot apiRoot 字段。
 * @param ownerTeam ownerTeam 字段。
 * @param status status 字段。
 * @param slaTier slaTier 字段。
 * @param timeoutMs timeoutMs 字段。
 * @param retryPolicy retryPolicy 字段。
 * @param schemaContract schemaContract 字段。
 * @param metadata metadata 字段。
 * @param createdBy createdBy 字段。
 * @param updatedBy updatedBy 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
public record MarketingIntegrationContractView(
        Long id,
        Long tenantId,
        String contractKey,
        String displayName,
        String providerFamily,
        String sourceCapabilityKey,
        String targetCapabilityKey,
        String assetKey,
        String direction,
        String environment,
        String authMode,
        String credentialDependency,
        String apiRoot,
        String ownerTeam,
        String status,
        String slaTier,
        Integer timeoutMs,
        Map<String, Object> retryPolicy,
        Map<String, Object> schemaContract,
        Map<String, Object> metadata,
        String createdBy,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
