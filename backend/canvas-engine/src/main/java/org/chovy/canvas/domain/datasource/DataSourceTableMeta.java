package org.chovy.canvas.domain.datasource;

import java.util.List;

public record DataSourceTableMeta(
        String name,
        List<String> columns
) {
}
