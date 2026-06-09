package org.chovy.canvas.dto.audience;

import java.util.List;

/**
 * AudiencePreviewResp 承载 dto.audience 场景中的不可变数据快照。
 * @param estimatedSize estimatedSize 字段。
 * @param sampleUserIds sampleUserIds 字段。
 */
public record AudiencePreviewResp(
        long estimatedSize,
        List<String> sampleUserIds
) {}
