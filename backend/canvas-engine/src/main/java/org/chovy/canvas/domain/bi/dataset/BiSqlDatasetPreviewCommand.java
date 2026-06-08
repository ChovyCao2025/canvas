package org.chovy.canvas.domain.bi.dataset;

import java.util.Map;

/**
 * BiSqlDatasetPreviewCommand 承载 domain.bi.dataset 场景中的不可变数据快照。
 * @param resource resource 字段。
 * @param sqlParameters sqlParameters 字段。
 * @param limit limit 字段。
 * @param executeSample executeSample 字段。
 */
public record BiSqlDatasetPreviewCommand(
        BiDatasetResource resource,
        Map<String, String> sqlParameters,
        Integer limit,
        Boolean executeSample
) {
    public BiSqlDatasetPreviewCommand {
        sqlParameters = sqlParameters == null ? Map.of() : Map.copyOf(sqlParameters);
    }
}
