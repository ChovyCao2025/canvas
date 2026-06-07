package org.chovy.canvas.domain.marketing;

import java.time.LocalDateTime;
import java.util.Map;

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
