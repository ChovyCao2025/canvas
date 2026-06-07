package org.chovy.canvas.domain.content;

import java.time.Duration;
import java.util.Map;

public record MarketingAssetUploadHandoffRequest(
        Long tenantId,
        String assetKey,
        String assetType,
        String provider,
        String mimeType,
        String fileName,
        Long sizeBytes,
        String intentKey,
        String uploadToken,
        String objectKey,
        Duration ttl,
        Map<String, String> requiredHeaders
) {

    public MarketingAssetUploadHandoffRequest {
        requiredHeaders = requiredHeaders == null ? Map.of() : Map.copyOf(requiredHeaders);
    }
}
