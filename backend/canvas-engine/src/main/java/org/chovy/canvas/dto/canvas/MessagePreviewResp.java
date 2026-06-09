package org.chovy.canvas.dto.canvas;

import java.util.List;
import java.util.Map;

/**
 * MessagePreviewResp 承载 dto.canvas 场景中的不可变数据快照。
 * @param channel channel 字段。
 * @param templateId templateId 字段。
 * @param content content 字段。
 * @param variables variables 字段。
 * @param warnings warnings 字段。
 */
public record MessagePreviewResp(
        String channel,
        String templateId,
        Map<String, Object> content,
        Map<String, Object> variables,
        List<String> warnings
) {
}
