package org.chovy.canvas.domain.bi.portal;

import java.time.LocalDateTime;

/**
 * BiPortalVersionView 承载 domain.bi.portal 场景中的不可变数据快照。
 * @param id id 字段。
 * @param portalKey portalKey 字段。
 * @param version version 字段。
 * @param status status 字段。
 * @param resource resource 字段。
 * @param publishedBy publishedBy 字段。
 * @param createdAt createdAt 字段。
 */
public record BiPortalVersionView(
        Long id,
        String portalKey,
        Integer version,
        String status,
        BiPortalResource resource,
        String publishedBy,
        LocalDateTime createdAt) {
}
