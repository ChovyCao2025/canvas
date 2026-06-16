package org.chovy.canvas.flink;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 汇总 Flink 作业启动所需的环境配置。
 *
 * <p>该配置由环境变量构建，负责把连接串、租户、Doris 分层库名和 checkpoint 上报参数整理成 SQL 模板可使用的占位符。
 */
public final class CanvasFlinkJobConfig {

    /**
     * 默认 Doris ODS 层数据库名。
     */
    private static final String DEFAULT_DORIS_ODS_DATABASE = "canvas_ods";

    /**
     * 默认 Doris DWD 层数据库名。
     */
    private static final String DEFAULT_DORIS_DWD_DATABASE = "canvas_dwd";

    /**
     * 默认 Doris DWS 层数据库名。
     */
    private static final String DEFAULT_DORIS_DWS_DATABASE = "canvas_dws";

    /**
     * 默认 checkpoint 上报方标识。
     */
    private static final String DEFAULT_REPORTED_BY = "canvas-flink-jobs";

    /**
     * 作业管道标识，用于选择 SQL asset 和 checkpoint 上报维度。
     */
    private final String pipelineKey;

    /**
     * 租户 ID，写入 SQL 模板后用于多租户数据隔离。
     */
    private final long tenantId;

    /**
     * MySQL JDBC URL，CDC 类管道作为源端连接串使用。
     */
    private final String mysqlUrl;

    /**
     * 从 MySQL JDBC URL 解析出的主机名，供 Flink connector 独立参数使用。
     */
    private final String mysqlHostname;

    /**
     * 从 MySQL JDBC URL 解析出的端口，未指定时默认为 3306。
     */
    private final int mysqlPort;

    /**
     * 从 MySQL JDBC URL 解析出的数据库名。
     */
    private final String mysqlDatabase;

    /**
     * MySQL 源端用户名。
     */
    private final String mysqlUsername;

    /**
     * MySQL 源端密码。
     */
    private final String mysqlPassword;

    /**
     * Doris FE 节点列表，供 Flink Doris connector 写入使用。
     */
    private final String dorisFeNodes;

    /**
     * Doris BE 节点列表，供数据写入和负载均衡使用。
     */
    private final String dorisBeNodes;

    /**
     * Doris JDBC URL，供维表或 SQL 查询类连接使用。
     */
    private final String dorisJdbcUrl;

    /**
     * Doris 写入用户名。
     */
    private final String dorisUsername;

    /**
     * Doris 写入密码，可为空字符串。
     */
    private final String dorisPassword;

    /**
     * Doris ODS 层数据库名。
     */
    private final String dorisOdsDatabase;

    /**
     * Doris DWD 层数据库名。
     */
    private final String dorisDwdDatabase;

    /**
     * Doris DWS 层数据库名。
     */
    private final String dorisDwsDatabase;

    /**
     * 作业启动和失败 checkpoint 上报的内部接口地址。
     */
    private final String checkpointEndpoint;

    /**
     * 调用内部 checkpoint 接口时携带的认证 token。
     */
    private final String internalApiToken;

    /**
     * checkpoint 记录中的上报方标识。
     */
    private final String reportedBy;

    /**
     * Doris 写入 label 后缀，用于区分环境或批次。
     */
    private final String dorisLabelSuffix;

    /**
     * 源端 schema 版本，随 checkpoint 一起上报用于排查兼容性。
     */
    private final String sourceSchemaVersion;

    /**
     * 目标端 schema 版本，随 checkpoint 一起上报用于排查兼容性。
     */
    private final String sinkSchemaVersion;

    /**
     * 创建 Flink 作业启动配置。
     *
     * @param pipelineKey 作业管道标识
     * @param tenantId 租户 ID
     * @param mysqlUrl MySQL JDBC URL
     * @param mysqlHostname MySQL 主机名
     * @param mysqlPort MySQL 端口
     * @param mysqlDatabase MySQL 数据库名
     * @param mysqlUsername MySQL 用户名
     * @param mysqlPassword MySQL 密码
     * @param dorisFeNodes Doris FE 节点列表
     * @param dorisBeNodes Doris BE 节点列表
     * @param dorisJdbcUrl Doris JDBC URL
     * @param dorisUsername Doris 用户名
     * @param dorisPassword Doris 密码
     * @param dorisOdsDatabase Doris ODS 层数据库名
     * @param dorisDwdDatabase Doris DWD 层数据库名
     * @param dorisDwsDatabase Doris DWS 层数据库名
     * @param checkpointEndpoint checkpoint 上报接口地址
     * @param internalApiToken 内部接口认证 token
     * @param reportedBy checkpoint 上报方标识
     * @param dorisLabelSuffix Doris 写入 label 后缀
     * @param sourceSchemaVersion 源端 schema 版本
     * @param sinkSchemaVersion 目标端 schema 版本
     */
    public CanvasFlinkJobConfig(String pipelineKey,
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
        this.pipelineKey = pipelineKey;
        this.tenantId = tenantId;
        this.mysqlUrl = mysqlUrl;
        this.mysqlHostname = mysqlHostname;
        this.mysqlPort = mysqlPort;
        this.mysqlDatabase = mysqlDatabase;
        this.mysqlUsername = mysqlUsername;
        this.mysqlPassword = mysqlPassword;
        this.dorisFeNodes = dorisFeNodes;
        this.dorisBeNodes = dorisBeNodes;
        this.dorisJdbcUrl = dorisJdbcUrl;
        this.dorisUsername = dorisUsername;
        this.dorisPassword = dorisPassword;
        this.dorisOdsDatabase = dorisOdsDatabase;
        this.dorisDwdDatabase = dorisDwdDatabase;
        this.dorisDwsDatabase = dorisDwsDatabase;
        this.checkpointEndpoint = checkpointEndpoint;
        this.internalApiToken = internalApiToken;
        this.reportedBy = reportedBy;
        this.dorisLabelSuffix = dorisLabelSuffix;
        this.sourceSchemaVersion = sourceSchemaVersion;
        this.sinkSchemaVersion = sinkSchemaVersion;
    }

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
        Map<String, String> values = env == null ? Map.of() : env;
        String pipelineKey = required(values, "CANVAS_FLINK_JOB_PIPELINE_KEY");
        long tenantId = parseLong(optional(values, "CANVAS_FLINK_TENANT_ID", "0"),
                "CANVAS_FLINK_TENANT_ID");

        MysqlConnection mysql = MysqlConnection.empty();
        if (requiresMysql(pipelineKey)) {
            // MySQL CDC 管道需要把 JDBC URL 拆成 Flink connector 的独立参数。
            mysql = MysqlConnection.parse(
                    required(values, "CANVAS_FLINK_MYSQL_URL"),
                    required(values, "CANVAS_FLINK_MYSQL_USERNAME"),
                    required(values, "CANVAS_FLINK_MYSQL_PASSWORD"));
        }

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
     * <p>该方法把配置转换为大写占位符名称，并把可选 MySQL 字段规范化为空字符串，避免模板出现 null。
     *
     * @return 不可变 SQL 占位符映射
     */
    public Map<String, String> placeholders() {
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
     * 返回作业管道标识。
     *
     * @return 作业管道标识
     */
    public String pipelineKey() {
        return pipelineKey;
    }

    /**
     * 返回租户 ID。
     *
     * @return 租户 ID
     */
    public long tenantId() {
        return tenantId;
    }

    /**
     * 返回 MySQL JDBC URL。
     *
     * @return MySQL JDBC URL
     */
    public String mysqlUrl() {
        return mysqlUrl;
    }

    /**
     * 返回 MySQL 主机名。
     *
     * @return MySQL 主机名
     */
    public String mysqlHostname() {
        return mysqlHostname;
    }

    /**
     * 返回 MySQL 端口。
     *
     * @return MySQL 端口
     */
    public int mysqlPort() {
        return mysqlPort;
    }

    /**
     * 返回 MySQL 数据库名。
     *
     * @return MySQL 数据库名
     */
    public String mysqlDatabase() {
        return mysqlDatabase;
    }

    /**
     * 返回 MySQL 用户名。
     *
     * @return MySQL 用户名
     */
    public String mysqlUsername() {
        return mysqlUsername;
    }

    /**
     * 返回 MySQL 密码。
     *
     * @return MySQL 密码
     */
    public String mysqlPassword() {
        return mysqlPassword;
    }

    /**
     * 返回 Doris FE 节点列表。
     *
     * @return Doris FE 节点列表
     */
    public String dorisFeNodes() {
        return dorisFeNodes;
    }

    /**
     * 返回 Doris BE 节点列表。
     *
     * @return Doris BE 节点列表
     */
    public String dorisBeNodes() {
        return dorisBeNodes;
    }

    /**
     * 返回 Doris JDBC URL。
     *
     * @return Doris JDBC URL
     */
    public String dorisJdbcUrl() {
        return dorisJdbcUrl;
    }

    /**
     * 返回 Doris 用户名。
     *
     * @return Doris 用户名
     */
    public String dorisUsername() {
        return dorisUsername;
    }

    /**
     * 返回 Doris 密码。
     *
     * @return Doris 密码
     */
    public String dorisPassword() {
        return dorisPassword;
    }

    /**
     * 返回 Doris ODS 层数据库名。
     *
     * @return Doris ODS 层数据库名
     */
    public String dorisOdsDatabase() {
        return dorisOdsDatabase;
    }

    /**
     * 返回 Doris DWD 层数据库名。
     *
     * @return Doris DWD 层数据库名
     */
    public String dorisDwdDatabase() {
        return dorisDwdDatabase;
    }

    /**
     * 返回 Doris DWS 层数据库名。
     *
     * @return Doris DWS 层数据库名
     */
    public String dorisDwsDatabase() {
        return dorisDwsDatabase;
    }

    /**
     * 返回 checkpoint 上报接口地址。
     *
     * @return checkpoint 上报接口地址
     */
    public String checkpointEndpoint() {
        return checkpointEndpoint;
    }

    /**
     * 返回内部接口认证 token。
     *
     * @return 内部接口认证 token
     */
    public String internalApiToken() {
        return internalApiToken;
    }

    /**
     * 返回 checkpoint 上报方标识。
     *
     * @return checkpoint 上报方标识
     */
    public String reportedBy() {
        return reportedBy;
    }

    /**
     * 返回 Doris 写入 label 后缀。
     *
     * @return Doris 写入 label 后缀
     */
    public String dorisLabelSuffix() {
        return dorisLabelSuffix;
    }

    /**
     * 返回源端 schema 版本。
     *
     * @return 源端 schema 版本
     */
    public String sourceSchemaVersion() {
        return sourceSchemaVersion;
    }

    /**
     * 返回目标端 schema 版本。
     *
     * @return 目标端 schema 版本
     */
    public String sinkSchemaVersion() {
        return sinkSchemaVersion;
    }

    /**
     * 按字段值判断两个配置是否相同。
     *
     * @param o 待比较对象
     * @return true 表示所有配置字段相同
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CanvasFlinkJobConfig that)) {
            return false;
        }
        return tenantId == that.tenantId
                && mysqlPort == that.mysqlPort
                && Objects.equals(pipelineKey, that.pipelineKey)
                && Objects.equals(mysqlUrl, that.mysqlUrl)
                && Objects.equals(mysqlHostname, that.mysqlHostname)
                && Objects.equals(mysqlDatabase, that.mysqlDatabase)
                && Objects.equals(mysqlUsername, that.mysqlUsername)
                && Objects.equals(mysqlPassword, that.mysqlPassword)
                && Objects.equals(dorisFeNodes, that.dorisFeNodes)
                && Objects.equals(dorisBeNodes, that.dorisBeNodes)
                && Objects.equals(dorisJdbcUrl, that.dorisJdbcUrl)
                && Objects.equals(dorisUsername, that.dorisUsername)
                && Objects.equals(dorisPassword, that.dorisPassword)
                && Objects.equals(dorisOdsDatabase, that.dorisOdsDatabase)
                && Objects.equals(dorisDwdDatabase, that.dorisDwdDatabase)
                && Objects.equals(dorisDwsDatabase, that.dorisDwsDatabase)
                && Objects.equals(checkpointEndpoint, that.checkpointEndpoint)
                && Objects.equals(internalApiToken, that.internalApiToken)
                && Objects.equals(reportedBy, that.reportedBy)
                && Objects.equals(dorisLabelSuffix, that.dorisLabelSuffix)
                && Objects.equals(sourceSchemaVersion, that.sourceSchemaVersion)
                && Objects.equals(sinkSchemaVersion, that.sinkSchemaVersion);
    }

    /**
     * 基于所有配置字段生成 hashCode。
     *
     * @return 字段组合哈希值
     */
    @Override
    public int hashCode() {
        int result = Objects.hashCode(pipelineKey);
        result = 31 * result + Long.hashCode(tenantId);
        result = 31 * result + Objects.hashCode(mysqlUrl);
        result = 31 * result + Objects.hashCode(mysqlHostname);
        result = 31 * result + Integer.hashCode(mysqlPort);
        result = 31 * result + Objects.hashCode(mysqlDatabase);
        result = 31 * result + Objects.hashCode(mysqlUsername);
        result = 31 * result + Objects.hashCode(mysqlPassword);
        result = 31 * result + Objects.hashCode(dorisFeNodes);
        result = 31 * result + Objects.hashCode(dorisBeNodes);
        result = 31 * result + Objects.hashCode(dorisJdbcUrl);
        result = 31 * result + Objects.hashCode(dorisUsername);
        result = 31 * result + Objects.hashCode(dorisPassword);
        result = 31 * result + Objects.hashCode(dorisOdsDatabase);
        result = 31 * result + Objects.hashCode(dorisDwdDatabase);
        result = 31 * result + Objects.hashCode(dorisDwsDatabase);
        result = 31 * result + Objects.hashCode(checkpointEndpoint);
        result = 31 * result + Objects.hashCode(internalApiToken);
        result = 31 * result + Objects.hashCode(reportedBy);
        result = 31 * result + Objects.hashCode(dorisLabelSuffix);
        result = 31 * result + Objects.hashCode(sourceSchemaVersion);
        result = 31 * result + Objects.hashCode(sinkSchemaVersion);
        return result;
    }

    /**
     * 返回与原 record 形式一致的调试字符串。
     *
     * @return 字段名和值组成的字符串
     */
    @Override
    public String toString() {
        return "CanvasFlinkJobConfig["
                + "pipelineKey=" + pipelineKey
                + ", tenantId=" + tenantId
                + ", mysqlUrl=" + mysqlUrl
                + ", mysqlHostname=" + mysqlHostname
                + ", mysqlPort=" + mysqlPort
                + ", mysqlDatabase=" + mysqlDatabase
                + ", mysqlUsername=" + mysqlUsername
                + ", mysqlPassword=" + mysqlPassword
                + ", dorisFeNodes=" + dorisFeNodes
                + ", dorisBeNodes=" + dorisBeNodes
                + ", dorisJdbcUrl=" + dorisJdbcUrl
                + ", dorisUsername=" + dorisUsername
                + ", dorisPassword=" + dorisPassword
                + ", dorisOdsDatabase=" + dorisOdsDatabase
                + ", dorisDwdDatabase=" + dorisDwdDatabase
                + ", dorisDwsDatabase=" + dorisDwsDatabase
                + ", checkpointEndpoint=" + checkpointEndpoint
                + ", internalApiToken=" + internalApiToken
                + ", reportedBy=" + reportedBy
                + ", dorisLabelSuffix=" + dorisLabelSuffix
                + ", sourceSchemaVersion=" + sourceSchemaVersion
                + ", sinkSchemaVersion=" + sinkSchemaVersion
                + ']';
    }

    /**
     * MySQL 连接配置快照。
     */
    private static final class MysqlConnection {

        /**
         * 原始 MySQL JDBC URL。
         */
        private final String url;

        /**
         * MySQL 主机名。
         */
        private final String hostname;

        /**
         * MySQL 端口。
         */
        private final int port;

        /**
         * MySQL 数据库名。
         */
        private final String database;

        /**
         * MySQL 用户名。
         */
        private final String username;

        /**
         * MySQL 密码。
         */
        private final String password;

        /**
         * 创建 MySQL 连接配置快照。
         *
         * @param url 原始 MySQL JDBC URL
         * @param hostname MySQL 主机名
         * @param port MySQL 端口
         * @param database MySQL 数据库名
         * @param username MySQL 用户名
         * @param password MySQL 密码
         */
        private MysqlConnection(String url,
                                String hostname,
                                int port,
                                String database,
                                String username,
                                String password) {
            this.url = url;
            this.hostname = hostname;
            this.port = port;
            this.database = database;
            this.username = username;
            this.password = password;
        }

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
            return new MysqlConnection(jdbcUrl, hostname, port, path.substring(1), username, password);
        }

        /**
         * 返回原始 MySQL JDBC URL。
         *
         * @return 原始 MySQL JDBC URL
         */
        String url() {
            return url;
        }

        /**
         * 返回 MySQL 主机名。
         *
         * @return MySQL 主机名
         */
        String hostname() {
            return hostname;
        }

        /**
         * 返回 MySQL 端口。
         *
         * @return MySQL 端口
         */
        int port() {
            return port;
        }

        /**
         * 返回 MySQL 数据库名。
         *
         * @return MySQL 数据库名
         */
        String database() {
            return database;
        }

        /**
         * 返回 MySQL 用户名。
         *
         * @return MySQL 用户名
         */
        String username() {
            return username;
        }

        /**
         * 返回 MySQL 密码。
         *
         * @return MySQL 密码
         */
        String password() {
            return password;
        }
    }
}
