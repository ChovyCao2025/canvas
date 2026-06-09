package org.chovy.canvas.domain.paidmedia;

import java.time.LocalDateTime;

/**
 * PaidMediaAudienceMemberView 承载 domain.paidmedia 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param runId runId 字段。
 * @param destinationId destinationId 字段。
 * @param audienceId audienceId 字段。
 * @param provider provider 字段。
 * @param userId userId 字段。
 * @param identifierType identifierType 字段。
 * @param identifierHash identifierHash 字段。
 * @param eligibilityStatus eligibilityStatus 字段。
 * @param reason reason 字段。
 * @param syncedAt syncedAt 字段。
 */
public record PaidMediaAudienceMemberView(
        Long id,
        Long tenantId,
        Long runId,
        Long destinationId,
        Long audienceId,
        String provider,
        String userId,
        String identifierType,
        String identifierHash,
        String eligibilityStatus,
        String reason,
        LocalDateTime syncedAt) {
}
