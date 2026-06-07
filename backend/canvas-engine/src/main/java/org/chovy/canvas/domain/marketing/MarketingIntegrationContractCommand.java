package org.chovy.canvas.domain.marketing;

import java.util.Map;

public record MarketingIntegrationContractCommand(
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
        Map<String, Object> metadata) {
}
