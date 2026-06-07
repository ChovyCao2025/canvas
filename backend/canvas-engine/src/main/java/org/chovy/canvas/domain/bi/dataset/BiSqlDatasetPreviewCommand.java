package org.chovy.canvas.domain.bi.dataset;

import java.util.Map;

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
