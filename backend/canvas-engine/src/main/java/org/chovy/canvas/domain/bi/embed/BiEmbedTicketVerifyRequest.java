package org.chovy.canvas.domain.bi.embed;

/**
 * BiEmbedTicketVerifyRequest 承载 domain.bi.embed 场景中的不可变数据快照。
 * @param ticket ticket 字段。
 */
public record BiEmbedTicketVerifyRequest(
        String ticket
) {
}
