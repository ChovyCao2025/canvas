package org.chovy.canvas.flink;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

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

    public static CanvasFlinkJobConfig fromEnvironment() {
        return from(System.getenv());
    }

    public static CanvasFlinkJobConfig from(Map<String, String> env) {
        Map<String, String> values = env == null ? Map.of() : env;
        String pipelineKey = required(values, "CANVAS_FLINK_JOB_PIPELINE_KEY");
        long tenantId = parseLong(optional(values, "CANVAS_FLINK_TENANT_ID", "0"),
                "CANVAS_FLINK_TENANT_ID");

        MysqlConnection mysql = MysqlConnection.empty();
        if (requiresMysql(pipelineKey)) {
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

    static boolean requiresMysql(String pipelineKey) {
        return pipelineKey != null && pipelineKey.startsWith("mysql_");
    }

    private static String required(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value.trim();
    }

    private static String optional(Map<String, String> values, String key, String defaultValue) {
        String value = values.get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    private static String optionalDorisLabelSuffix(Map<String, String> values) {
        String value = optional(values, "CANVAS_FLINK_DORIS_LABEL_SUFFIX", "");
        if (!value.matches("[A-Za-z0-9_-]*")) {
            throw new IllegalArgumentException(
                    "CANVAS_FLINK_DORIS_LABEL_SUFFIX may only contain letters, numbers, '_' and '-'");
        }
        return value;
    }

    private static long parseLong(String value, String key) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(key + " must be a number", ex);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record MysqlConnection(
            String url,
            String hostname,
            int port,
            String database,
            String username,
            String password) {

        static MysqlConnection empty() {
            return new MysqlConnection("", "", 0, "", "", "");
        }

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
    }
}
