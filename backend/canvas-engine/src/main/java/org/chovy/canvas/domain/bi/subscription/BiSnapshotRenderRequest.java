package org.chovy.canvas.domain.bi.subscription;

import java.util.Map;

/**
 * BiSnapshotRenderRequest 承载 domain.bi.subscription 场景中的不可变数据快照。
 * @param html html 字段。
 * @param resourceUrl resourceUrl 字段。
 * @param format format 字段。
 * @param width width 字段。
 * @param height height 字段。
 * @param scale scale 字段。
 * @param metadata metadata 字段。
 */
public record BiSnapshotRenderRequest(
        String html,
        String resourceUrl,
        String format,
        int width,
        int height,
        double scale,
        Map<String, Object> metadata
) {
    public BiSnapshotRenderRequest {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
