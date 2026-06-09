package org.chovy.canvas.domain.content;

import java.util.Map;

/**
 * MarketingAssetUploadHandoff 承载 domain.content 场景中的不可变数据快照。
 * @param uploadUrl uploadUrl 字段。
 * @param storageUrl storageUrl 字段。
 * @param uploadParams uploadParams 字段。
 * @param requiredHeaders requiredHeaders 字段。
 */
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
