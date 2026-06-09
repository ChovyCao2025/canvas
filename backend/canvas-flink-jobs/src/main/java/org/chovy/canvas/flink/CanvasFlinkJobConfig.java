package org.chovy.canvas.flink;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Flink 作业启动配置记录。
 *
 * @param pipelineKey 作业管道标识，用于选择 SQL asset 和 checkpoint 上报维度.
 * @param tenantId 租户 ID，写入 SQL 模板后用于多租户数据隔离.
 * @param mysqlUrl MySQL JDBC URL，CDC 类管道作为源端连接串使用.
 * @param mysqlHostname 从 MySQL JDBC URL 解析出的主机名，供 Flink connector 独立参数使用.
 * @param mysqlPort 从 MySQL JDBC URL 解析出的端口，未指定时默认为 3306.
 * @param mysqlDatabase 从 MySQL JDBC URL 解析出的数据库名.
 * @param mysqlUsername MySQL 源端用户名.
 * @param mysqlPassword MySQL 源端密码.
 * @param dorisFeNodes Doris FE 节点列表，供 Flink Doris connector 写入使用.
 * @param dorisBeNodes Doris BE 节点列表，供数据写入和负载均衡使用.
 * @param dorisJdbcUrl Doris JDBC URL，供维表或 SQL 查询类连接使用.
 * @param dorisUsername Doris 写入用户名.
 * @param dorisPassword Doris 写入密码，可为空字符串.
 * @param dorisOdsDatabase Doris ODS 层数据库名.
 * @param dorisDwdDatabase Doris DWD 层数据库名.
 * @param dorisDwsDatabase Doris DWS 层数据库名.
 * @param checkpointEndpoint 作业启动和失败 checkpoint 上报的内部接口地址.
 * @param internalApiToken 调用内部 checkpoint 接口时携带的认证 token.
 * @param reportedBy checkpoint 记录中的上报方标识.
 * @param dorisLabelSuffix Doris 写入 label 后缀，用于区分环境或批次.
 * @param sourceSchemaVersion 源端 schema 版本，随 checkpoint 一起上报用于排查兼容性.
 * @param sinkSchemaVersion 目标端 schema 版本，随 checkpoint 一起上报用于排查兼容性.
 */
public record CanvasFlinkJobConfig(
        String pipelineKey,
        long tenantId,
        String mysqlUrl,
        String mysqlHostname,
        int mysqlPort,
        String mysqlDatabase,
        String mysqlUsername,
        String mysqlPassword,
        String dorisFeNodes,
        String dorisBeNodes,
        String dorisJdbcUrl,
        String dorisUsername,
        String dorisPassword,
        String dorisOdsDatabase,
        String dorisDwdDatabase,
        String dorisDwsDatabase,
        String checkpointEndpoint,
        String internalApiToken,
        String reportedBy,
        String dorisLabelSuffix,
        String sourceSchemaVersion,
        String sinkSchemaVersion) {

    private static final String DEFAULT_DORIS_ODS_DATABASE = "canvas_ods";
    private static final String DEFAULT_DORIS_DWD_DATABASE = "canvas_dwd";
    private static final String DEFAULT_DORIS_DWS_DATABASE = "canvas_dws";
    private static final String DEFAULT_REPORTED_BY = "canvas-flink-jobs";

    /**
     * 从当前进程环境变量构建 Flink 作业配置。
     *
     * <p>该入口用于生产启动路径，必填项缺失会立即抛出，避免提交半配置的 Flink 作业。
     *
     * @return 环境变量解析得到的作业配置
     */
    public static CanvasFlinkJobConfig fromEnvironment() {
        return from(System.getenv());
    }

    /**
     * 从给定环境变量映射构建 Flink 作业配置。
     *
     * <p>MySQL CDC 管道会强制要求 MySQL 连接信息；Doris 连接、checkpoint endpoint 和 pipelineKey 始终必填。
     *
     * @param env 环境变量映射，测试可传入自定义 Map
     * @return 校验并规范化后的作业配置
     */
    public static CanvasFlinkJobConfig from(Map<String, String> env) {
        // 准备本次处理所需的上下文和中间变量。
        Map<String, String> values = env == null ? Map.of() : env;
        String pipelineKey = required(values, "CANVAS_FLINK_JOB_PIPELINE_KEY");
        long tenantId = parseLong(optional(values, "CANVAS_FLINK_TENANT_ID", "0"),
                "CANVAS_FLINK_TENANT_ID");

        MysqlConnection mysql = MysqlConnection.empty();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (requiresMysql(pipelineKey)) {
            mysql = MysqlConnection.parse(
                    required(values, "CANVAS_FLINK_MYSQL_URL"),
                    required(values, "CANVAS_FLINK_MYSQL_USERNAME"),
                    required(values, "CANVAS_FLINK_MYSQL_PASSWORD"));
        }

        // 汇总前面计算出的状态和明细，返回给调用方。
        return new CanvasFlinkJobConfig(
                pipelineKey,
                tenantId,
                mysql.url(),
                mysql.hostname(),
                mysql.port(),
                mysql.database(),
                mysql.username(),
                mysql.password(),
                required(values, "CANVAS_FLINK_DORIS_FE_NODES"),
                required(values, "CANVAS_FLINK_DORIS_BE_NODES"),
                required(values, "CANVAS_FLINK_DORIS_JDBC_URL"),
                required(values, "CANVAS_FLINK_DORIS_USERNAME"),
                optional(values, "CANVAS_FLINK_DORIS_PASSWORD", ""),
                optional(values, "CANVAS_FLINK_DORIS_ODS_DATABASE", DEFAULT_DORIS_ODS_DATABASE),
                optional(values, "CANVAS_FLINK_DORIS_DWD_DATABASE", DEFAULT_DORIS_DWD_DATABASE),
                optional(values, "CANVAS_FLINK_DORIS_DWS_DATABASE", DEFAULT_DORIS_DWS_DATABASE),
                required(values, "CANVAS_FLINK_CHECKPOINT_ENDPOINT"),
                optional(values, "CANVAS_FLINK_INTERNAL_API_TOKEN", ""),
                optional(values, "CANVAS_FLINK_REPORTED_BY", DEFAULT_REPORTED_BY),
                optionalDorisLabelSuffix(values),
                optional(values, "CANVAS_FLINK_SOURCE_SCHEMA_VERSION", ""),
                optional(values, "CANVAS_FLINK_SINK_SCHEMA_VERSION", ""));
    }

    /**
     * 生成 SQL 模板渲染所需的占位符映射。
     *
     * <p>该方法把 record 配置转换为大写占位符名称，并把可选 MySQL 字段规范化为空字符串，避免模板出现 null。
     *
     * @return 不可变 SQL 占位符映射
     */
    public Map<String, String> placeholders() {
        // 准备本次处理所需的上下文和中间变量。
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("PIPELINE_KEY", pipelineKey);
        placeholders.put("TENANT_ID", Long.toString(tenantId));
        placeholders.put("MYSQL_URL", nullToEmpty(mysqlUrl));
        placeholders.put("MYSQL_HOSTNAME", nullToEmpty(mysqlHostname));
        placeholders.put("MYSQL_PORT", mysqlPort <= 0 ? "" : Integer.toString(mysqlPort));
        placeholders.put("MYSQL_DATABASE", nullToEmpty(mysqlDatabase));
        placeholders.put("MYSQL_USERNAME", nullToEmpty(mysqlUsername));
        placeholders.put("MYSQL_PASSWORD", nullToEmpty(mysqlPassword));
        placeholders.put("DORIS_FE_NODES", dorisFeNodes);
        placeholders.put("DORIS_BE_NODES", dorisBeNodes);
        placeholders.put("DORIS_JDBC_URL", dorisJdbcUrl);
        placeholders.put("DORIS_USERNAME", dorisUsername);
        placeholders.put("DORIS_PASSWORD", nullToEmpty(dorisPassword));
        placeholders.put("DORIS_ODS_DATABASE", dorisOdsDatabase);
        placeholders.put("DORIS_DWD_DATABASE", dorisDwdDatabase);
        placeholders.put("DORIS_DWS_DATABASE", dorisDwsDatabase);
        placeholders.put("CANVAS_CHECKPOINT_ENDPOINT", checkpointEndpoint);
        placeholders.put("REPORTED_BY", reportedBy);
        placeholders.put("DORIS_LABEL_SUFFIX", dorisLabelSuffix);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return Map.copyOf(placeholders);
    }

    /**
     * 判断指定管道是否需要 MySQL CDC 连接配置。
     *
     * @param pipelineKey 作业管道标识
     * @return true 表示需要解析 MySQL 连接信息
     */
    static boolean requiresMysql(String pipelineKey) {
        return pipelineKey != null && pipelineKey.startsWith("mysql_");
    }

    /**
     * 读取必填环境变量。
     *
     * @param values 环境变量映射
     * @param key 环境变量名称
     * @return 去除首尾空白后的环境变量值
     */
    private static String required(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value.trim();
    }

    /**
     * 读取可选环境变量并在缺失时返回默认值。
     *
     * @param values 环境变量映射
     * @param key 环境变量名称
     * @param defaultValue 默认值
     * @return 规范化后的环境变量值或默认值
     */
    private static String optional(Map<String, String> values, String key, String defaultValue) {
        String value = values.get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    /**
     * 读取并校验 Doris label 后缀。
     *
     * @param values 环境变量映射
     * @return 可安全拼接到 Doris label 的后缀
     */
    private static String optionalDorisLabelSuffix(Map<String, String> values) {
        String value = optional(values, "CANVAS_FLINK_DORIS_LABEL_SUFFIX", "");
        if (!value.matches("[A-Za-z0-9_-]*")) {
            throw new IllegalArgumentException(
                    "CANVAS_FLINK_DORIS_LABEL_SUFFIX may only contain letters, numbers, '_' and '-'");
        }
        return value;
    }

    /**
     * 将环境变量值解析为 long。
     *
     * @param value 原始字符串
     * @param key 环境变量名称，用于错误信息
     * @return 解析后的 long 值
     */
    private static long parseLong(String value, String key) {
        try {
            return Long.parseLong(value);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(key + " must be a number", ex);
        }
    }

    /**
     * 将 null 字符串归一化为空字符串。
     *
     * @param value 原始字符串
     * @return 非 null 字符串
     */
    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * MySQL 连接配置记录。
     *
     * @param url 原始 MySQL JDBC URL.
     * @param hostname MySQL 主机名.
     * @param port MySQL 端口.
     * @param database MySQL 数据库名.
     * @param username MySQL 用户名.
     * @param password MySQL 密码.
     */
    private record MysqlConnection(
        String url,
        String hostname,
        int port,
        String database,
        String username,
        String password) {

        /**
         * 创建无需 MySQL 连接时使用的空配置。
         *
         * @return 字段均为空或 0 的 MySQL 连接配置
         */
        static MysqlConnection empty() {
            return new MysqlConnection("", "", 0, "", "", "");
        }

        /**
         * 从 JDBC URL 和账号信息解析 MySQL 连接配置。
         *
         * @param jdbcUrl MySQL JDBC URL
         * @param username MySQL 用户名
         * @param password MySQL 密码
         * @return 拆分后的 MySQL 连接配置
         */
        static MysqlConnection parse(String jdbcUrl, String username, String password) {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (!jdbcUrl.startsWith("jdbc:mysql://")) {
                throw new IllegalArgumentException("CANVAS_FLINK_MYSQL_URL must start with jdbc:mysql://");
            }
            URI uri = URI.create(jdbcUrl.substring("jdbc:".length()));
            String hostname = uri.getHost();
            String path = uri.getPath();
            if (hostname == null || hostname.isBlank()) {
                throw new IllegalArgumentException("CANVAS_FLINK_MYSQL_URL host is required");
            }
            if (path == null || path.length() <= 1) {
                throw new IllegalArgumentException("CANVAS_FLINK_MYSQL_URL database is required");
            }
            int port = uri.getPort() == -1 ? 3306 : uri.getPort();
            // 汇总前面计算出的状态和明细，返回给调用方。
            return new MysqlConnection(jdbcUrl, hostname, port, path.substring(1), username, password);
        }
    }
}
