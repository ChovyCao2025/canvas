package org.chovy.canvas.domain.content;

import java.time.Duration;
import java.util.Map;

/**
 * MarketingAssetUploadHandoffRequest 承载 domain.content 场景中的不可变数据快照。
 * @param tenantId tenantId 字段。
 * @param assetKey assetKey 字段。
 * @param assetType assetType 字段。
 * @param provider provider 字段。
 * @param mimeType mimeType 字段。
 * @param fileName fileName 字段。
 * @param sizeBytes sizeBytes 字段。
 * @param intentKey intentKey 字段。
 * @param uploadToken uploadToken 字段。
 * @param objectKey objectKey 字段。
 * @param ttl ttl 字段。
 * @param requiredHeaders requiredHeaders 字段。
 */
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
