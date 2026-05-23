package org.chovy.canvas.engine.audience;

public record JdbcConfig(
        Long dataSourceId,
        String baseTable,
        String url,
        String username,
        String password,
        String userIdColumn,
        String driverClassName,
        Integer maxRows
) {
}
