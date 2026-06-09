package org.chovy.canvas.domain.programmatic;

import java.util.Map;

/**
 * ProgrammaticDspSeatCommand 承载 domain.programmatic 场景中的不可变数据快照。
 * @param provider provider 字段。
 * @param seatKey seatKey 字段。
 * @param displayName displayName 字段。
 * @param advertiserAccountId advertiserAccountId 字段。
 * @param currency currency 字段。
 * @param timezone timezone 字段。
 * @param supplyChainEnforcement supplyChainEnforcement 字段。
 * @param enabled enabled 字段。
 * @param metadata metadata 字段。
 */
public record ProgrammaticDspSeatCommand(
        String provider,
        String seatKey,
        String displayName,
        String advertiserAccountId,
        String currency,
        String timezone,
        String supplyChainEnforcement,
        Boolean enabled,
        Map<String, Object> metadata) {
}
