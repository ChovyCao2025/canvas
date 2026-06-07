package org.chovy.canvas.domain.content;

import java.util.Map;

public record MarketingAssetUploadHandoff(
        String uploadUrl,
        String storageUrl,
        Map<String, Object> uploadParams,
        Map<String, String> requiredHeaders
) {

    public MarketingAssetUploadHandoff {
        uploadParams = uploadParams == null ? Map.of() : Map.copyOf(uploadParams);
        requiredHeaders = requiredHeaders == null ? Map.of() : Map.copyOf(requiredHeaders);
    }
}
