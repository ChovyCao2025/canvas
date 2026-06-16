package org.chovy.canvas.bi.api;

import java.util.Map;
/**
 * BiSelfServicePreviewCommand 命令。
 */
public record BiSelfServicePreviewCommand(
        /**
         * 查询定义。
         */
        Map<String, Object> query,
        Integer previewLimit) {

    public BiSelfServicePreviewCommand {
        query = query == null ? Map.of() : Map.copyOf(query);
    }
}
