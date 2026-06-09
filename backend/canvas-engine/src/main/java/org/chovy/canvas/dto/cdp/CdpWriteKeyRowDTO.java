package org.chovy.canvas.dto.cdp;

import java.time.LocalDateTime;

/**
 * CdpWriteKeyRowDTO 承载 dto.cdp 场景中的不可变数据快照。
 * @param id id 字段。
 * @param name name 字段。
 * @param keyPrefix keyPrefix 字段。
 * @param platform platform 字段。
 * @param status status 字段。
 * @param rateLimitQps rateLimitQps 字段。
 * @param dailyQuota dailyQuota 字段。
 * @param description description 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
public record CdpWriteKeyRowDTO(
        Long id,
        String name,
        String keyPrefix,
        String platform,
        String status,
        Integer rateLimitQps,
        Long dailyQuota,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
