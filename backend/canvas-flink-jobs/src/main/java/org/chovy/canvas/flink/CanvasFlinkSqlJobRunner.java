package org.chovy.canvas.flink;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import java.util.ArrayList;
import java.util.List;

public class CanvasFlinkSqlJobRunner {

    public int run(String sql) {
        return run(sql, flinkExecutor());
    }

    public static int run(String sql, SqlExecutor executor) {
        if (executor == null) {
            throw new IllegalArgumentException("SQL executor is required");
        }
        List<String> statements = statements(sql);
        for (String statement : statements) {
            executor.execute(statement);
        }
        return statements.size();
    }

    public static SqlExecutor flinkExecutor() {
        StreamExecutionEnvironment environment = StreamExecutionEnvironment.getExecutionEnvironment();
        StreamTableEnvironment tableEnvironment = StreamTableEnvironment.create(environment);
        return statement -> tableEnvironment.executeSql(statement);
    }

    static List<String> statements(String sql) {
        if (sql == null || sql.isBlank()) {
            return List.of();
        }
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (c == '\'') {
                current.append(c);
                if (inSingleQuote && i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                    current.append(sql.charAt(i + 1));
                    i++;
                } else {
                    inSingleQuote = !inSingleQuote;
                }
                continue;
            }
            if (c == ';' && !inSingleQuote) {
                addStatement(statements, current);
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        addStatement(statements, current);
        return List.copyOf(statements);
    }

    private static void addStatement(List<String> statements, StringBuilder current) {
        String statement = current.toString().trim();
        if (!statement.isEmpty()) {
            statements.add(statement);
        }
    }

    @FunctionalInterface
    public interface SqlExecutor {
        void execute(String statement);
    }
}
