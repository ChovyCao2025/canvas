package org.chovy.canvas.domain.programmatic;

import java.util.Map;

/**
 * ProgrammaticDspMutationRequest 承载 domain.programmatic 场景中的不可变数据快照。
 * @param tenantId tenantId 字段。
 * @param seatId seatId 字段。
 * @param campaignId campaignId 字段。
 * @param lineItemId lineItemId 字段。
 * @param supplyPathId supplyPathId 字段。
 * @param provider provider 字段。
 * @param seatKey seatKey 字段。
 * @param advertiserAccountId advertiserAccountId 字段。
 * @param mutationType mutationType 字段。
 * @param entityType entityType 字段。
 * @param externalEntityId externalEntityId 字段。
 * @param idempotencyKey idempotencyKey 字段。
 * @param dryRun dryRun 字段。
 * @param partialFailure partialFailure 字段。
 * @param payload payload 字段。
 * @param metadata metadata 字段。
 */
public record ProgrammaticDspMutationRequest(
        Long tenantId,
        Long seatId,
        Long campaignId,
        Long lineItemId,
        Long supplyPathId,
        String provider,
        String seatKey,
        String advertiserAccountId,
        String mutationType,
        String entityType,
        String externalEntityId,
        String idempotencyKey,
        boolean dryRun,
        boolean partialFailure,
        Map<String, Object> payload,
        Map<String, Object> metadata
) {
}
