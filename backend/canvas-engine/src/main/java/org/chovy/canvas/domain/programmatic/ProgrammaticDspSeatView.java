package org.chovy.canvas.domain.programmatic;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * ProgrammaticDspSeatView 承载 domain.programmatic 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param provider provider 字段。
 * @param seatKey seatKey 字段。
 * @param displayName displayName 字段。
 * @param advertiserAccountId advertiserAccountId 字段。
 * @param currency currency 字段。
 * @param timezone timezone 字段。
 * @param supplyChainEnforcement supplyChainEnforcement 字段。
 * @param enabled enabled 字段。
 * @param metadata metadata 字段。
 * @param createdBy createdBy 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
public record ProgrammaticDspSeatView(
        Long id,
        Long tenantId,
        String provider,
        String seatKey,
        String displayName,
        String advertiserAccountId,
        String currency,
        String timezone,
        String supplyChainEnforcement,
        boolean enabled,
        Map<String, Object> metadata,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
