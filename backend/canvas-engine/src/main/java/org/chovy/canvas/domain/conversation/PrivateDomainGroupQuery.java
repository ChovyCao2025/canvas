package org.chovy.canvas.domain.conversation;

/**
 * PrivateDomainGroupQuery 承载 domain.conversation 场景中的不可变数据快照。
 * @param provider provider 字段。
 * @param ownerUserId ownerUserId 字段。
 * @param limit limit 字段。
 */
public record PrivateDomainGroupQuery(
        String provider,
        String ownerUserId,
        int limit) {
}
