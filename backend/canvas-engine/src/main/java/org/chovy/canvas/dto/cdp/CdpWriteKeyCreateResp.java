package org.chovy.canvas.dto.cdp;

/**
 * CdpWriteKeyCreateResp 承载 dto.cdp 场景中的不可变数据快照。
 * @param id id 字段。
 * @param name name 字段。
 * @param writeKey writeKey 字段。
 * @param keyPrefix keyPrefix 字段。
 * @param platform platform 字段。
 * @param rateLimitQps rateLimitQps 字段。
 * @param dailyQuota dailyQuota 字段。
 */
public record CdpWriteKeyCreateResp(
        Long id,
        String name,
        String writeKey,
        String keyPrefix,
        String platform,
        Integer rateLimitQps,
        Long dailyQuota
) {
}
