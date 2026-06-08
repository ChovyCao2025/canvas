package org.chovy.canvas.domain.bi.embed;

import java.time.Instant;

/**
 * BiEmbedTicket 承载 domain.bi.embed 场景中的不可变数据快照。
 * @param ticket ticket 字段。
 * @param expiresAt expiresAt 字段。
 * @param embedUrl embedUrl 字段。
 */
public record BiEmbedTicket(
        String ticket,
        Instant expiresAt,
        String embedUrl
) {
}
