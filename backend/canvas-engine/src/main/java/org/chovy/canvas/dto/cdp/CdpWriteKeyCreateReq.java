package org.chovy.canvas.dto.cdp;

/**
 * CdpWriteKeyCreateReq 承载 dto.cdp 场景中的不可变数据快照。
 * @param name name 字段。
 * @param platform platform 字段。
 * @param rateLimitQps rateLimitQps 字段。
 * @param dailyQuota dailyQuota 字段。
 * @param description description 字段。
 */
public record CdpWriteKeyCreateReq(
        String name,
        String platform,
        Integer rateLimitQps,
        Long dailyQuota,
        String description
) {
}
