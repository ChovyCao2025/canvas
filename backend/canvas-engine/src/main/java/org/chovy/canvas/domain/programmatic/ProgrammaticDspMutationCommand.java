package org.chovy.canvas.domain.programmatic;

import java.util.Map;

/**
 * ProgrammaticDspMutationCommand 承载 domain.programmatic 场景中的不可变数据快照。
 * @param seatId seatId 字段。
 * @param campaignId campaignId 字段。
 * @param lineItemId lineItemId 字段。
 * @param supplyPathId supplyPathId 字段。
 * @param mutationKey mutationKey 字段。
 * @param mutationType mutationType 字段。
 * @param entityType entityType 字段。
 * @param externalEntityId externalEntityId 字段。
 * @param dryRunRequired dryRunRequired 字段。
 * @param idempotencyKey idempotencyKey 字段。
 * @param payload payload 字段。
 */
public record ProgrammaticDspMutationCommand(
        Long seatId,
        Long campaignId,
        Long lineItemId,
        Long supplyPathId,
        String mutationKey,
        String mutationType,
        String entityType,
        String externalEntityId,
        Boolean dryRunRequired,
        String idempotencyKey,
        Map<String, Object> payload
) {
}
