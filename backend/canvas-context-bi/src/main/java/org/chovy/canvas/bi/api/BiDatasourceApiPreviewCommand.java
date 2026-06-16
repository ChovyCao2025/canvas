package org.chovy.canvas.bi.api;

import java.util.Map;
/**
 * BiDatasourceApiPreviewCommand 命令。
 */
public record BiDatasourceApiPreviewCommand(
        /**
         * parameters 对应的数据集合。
         */
        Map<String, Object> parameters,
        /**
         * path 字段值。
         */
        String path,
        /**
         * method 字段值。
         */
        String method,
        /**
         * headers 对应的数据集合。
         */
        Map<String, String> headers,
        /**
         * body 字段值。
         */
        Map<String, Object> body,
        int limit) {

    public BiDatasourceApiPreviewCommand {
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        body = body == null ? Map.of() : Map.copyOf(body);
    }
}
