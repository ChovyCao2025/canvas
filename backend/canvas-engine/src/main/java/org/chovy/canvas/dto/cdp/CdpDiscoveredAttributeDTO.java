package org.chovy.canvas.dto.cdp;

import java.time.LocalDateTime;

/**
 * CdpDiscoveredAttributeDTO 承载 dto.cdp 场景中的不可变数据快照。
 * @param id id 字段。
 * @param eventCode eventCode 字段。
 * @param attrName attrName 字段。
 * @param attrType attrType 字段。
 * @param status status 字段。
 * @param sampleValue sampleValue 字段。
 * @param firstSeenAt firstSeenAt 字段。
 * @param lastSeenAt lastSeenAt 字段。
 */
public record CdpDiscoveredAttributeDTO(
        Long id,
        String eventCode,
        String attrName,
        String attrType,
        String status,
        String sampleValue,
        LocalDateTime firstSeenAt,
        LocalDateTime lastSeenAt
) {
}
