package org.chovy.canvas.flink;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import java.util.ArrayList;
import java.util.List;

/**
 * CanvasFlinkSqlJobRunner 支撑 flink 场景的后端处理。
 */
public class CanvasFlinkSqlJobRunner {

    /**
     * 使用默认 Flink TableEnvironment 执行一段 SQL 脚本。
     *
     * <p>脚本会按分号拆分为多条语句顺序提交，适合启动由 SQL asset 描述的流式同步或聚合作业。
     *
     * @param sql 待执行的 Flink SQL 脚本
     * @return 实际提交给 Flink 的 SQL 语句数量
     */
    public int run(String sql) {
        return run(sql, flinkExecutor());
    }

    /**
     * 使用指定执行器顺序执行 SQL 脚本。
     *
     * <p>该方法不吞掉执行异常；任一语句失败会中断后续语句并把异常交给调用方用于作业失败上报。
     *
     * @param sql 待执行的 Flink SQL 脚本
     * @param executor SQL 语句执行器，生产环境通常封装 Flink Table API
     * @return 实际提交给执行器的 SQL 语句数量
     */
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

    /**
     * 创建基于当前 Flink 运行环境的 SQL 执行器。
     *
     * <p>执行器使用 {@link StreamExecutionEnvironment#getExecutionEnvironment()} 获取集群或本地环境，
     * 并通过 {@link StreamTableEnvironment#executeSql(String)} 提交每条语句。
     *
     * @return Flink SQL 执行器
     */
    public static SqlExecutor flinkExecutor() {
        StreamExecutionEnvironment environment = StreamExecutionEnvironment.getExecutionEnvironment();
        StreamTableEnvironment tableEnvironment = StreamTableEnvironment.create(environment);
        return statement -> tableEnvironment.executeSql(statement);
    }

    /**
     * 将 SQL 脚本拆分为可独立提交的语句。
     *
     * <p>拆分时会识别单引号字符串、行注释、块注释和 PostgreSQL 风格 dollar quote，
     * 避免误把这些片段内部的分号当成语句边界。
     *
     * @param sql 原始 SQL 脚本
     * @return 去除首尾空白后的语句列表
     */
    static List<String> statements(String sql) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (sql == null || sql.isBlank()) {
            return List.of();
        }
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        String dollarQuoteDelimiter = null;
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (dollarQuoteDelimiter != null) {
                if (startsWith(sql, i, dollarQuoteDelimiter)) {
                    current.append(dollarQuoteDelimiter);
                    i += dollarQuoteDelimiter.length() - 1;
                    dollarQuoteDelimiter = null;
                } else {
                    current.append(c);
                }
                continue;
            }
            if (inLineComment) {
                current.append(c);
                if (c == '\n' || c == '\r') {
                    inLineComment = false;
                }
                continue;
            }
            if (inBlockComment) {
                current.append(c);
                if (c == '*' && i + 1 < sql.length() && sql.charAt(i + 1) == '/') {
                    current.append(sql.charAt(i + 1));
                    i++;
                    inBlockComment = false;
                }
                continue;
            }
            if (!inSingleQuote && c == '-' && i + 1 < sql.length() && sql.charAt(i + 1) == '-') {
                current.append(c);
                current.append(sql.charAt(i + 1));
                i++;
                inLineComment = true;
                continue;
            }
            if (!inSingleQuote && c == '/' && i + 1 < sql.length() && sql.charAt(i + 1) == '*') {
                current.append(c);
                current.append(sql.charAt(i + 1));
                i++;
                inBlockComment = true;
                continue;
            }
            String openingDollarQuoteDelimiter = dollarQuoteDelimiter(sql, i);
            if (!inSingleQuote && openingDollarQuoteDelimiter != null) {
                current.append(openingDollarQuoteDelimiter);
                i += openingDollarQuoteDelimiter.length() - 1;
                dollarQuoteDelimiter = openingDollarQuoteDelimiter;
                continue;
            }
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return List.copyOf(statements);
    }

    /**
     * 识别从当前位置开始的 dollar quote 分隔符。
     *
     * @param sql 原始 SQL 脚本
     * @param start 当前扫描位置
     * @return 完整分隔符，当前位置不是合法分隔符时返回 null
     */
    private static String dollarQuoteDelimiter(String sql, int start) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (sql.charAt(start) != '$') {
            return null;
        }
        int end = sql.indexOf('$', start + 1);
        if (end < 0) {
            return null;
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (int i = start + 1; i < end; i++) {
            char c = sql.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_') {
                return null;
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return sql.substring(start, end + 1);
    }

    /**
     * 判断 SQL 指定位置是否匹配目标字符串。
     *
     * @param sql 原始 SQL 脚本
     * @param start 当前扫描位置
     * @param value 待匹配字符串
     * @return true 表示当前位置完整匹配 value
     */
    private static boolean startsWith(String sql, int start, String value) {
        return start + value.length() <= sql.length() && sql.startsWith(value, start);
    }

    /**
     * 将当前缓冲区中的非空语句加入结果列表。
     *
     * @param statements 语句结果列表
     * @param current 当前语句缓冲区
     */
    private static void addStatement(List<String> statements, StringBuilder current) {
        String statement = current.toString().trim();
        if (!statement.isEmpty()) {
            statements.add(statement);
        }
    }

    /**
     * SQL 语句执行器抽象。
     */
    @FunctionalInterface
    public interface SqlExecutor {
        /**
         * 提交单条 Flink SQL 语句。
         *
         * <p>实现可以是真实 Flink TableEnvironment，也可以是测试替身；异常应向上传播给 job main 统一上报。
         *
         * @param statement 单条已去除首尾空白的 SQL 语句
         */
        void execute(String statement);
    }
}
