-- pipeline: mysql_canvas_trace_to_doris_ods
-- sink: canvas_ods.canvas_execution_trace
-- Purpose: mirror node execution traces from MySQL into Doris ODS for runtime analytics.
CREATE TABLE mysql_canvas_execution_trace_source (
    id BIGINT,
    tenant_id BIGINT,
    execution_id STRING,
    node_id STRING,
    node_type STRING,
    node_name STRING,
    status INT,
    input_data STRING,
    output_data STRING,
    error_msg STRING,
    started_at TIMESTAMP(3),
    finished_at TIMESTAMP(3),
    duration_ms BIGINT,
    PRIMARY KEY (id) NOT ENFORCED
) WITH (
    'connector' = 'mysql-cdc',
    'hostname' = '${MYSQL_HOSTNAME}',
    'port' = '${MYSQL_PORT}',
    'username' = '${MYSQL_USERNAME}',
    'password' = '${MYSQL_PASSWORD}',
    'database-name' = '${MYSQL_DATABASE}',
    'table-name' = 'canvas_execution_trace',
    'server-time-zone' = 'Asia/Shanghai',
    'scan.startup.mode' = 'initial'
);

-- Doris ODS trace table keeps input/output/error payloads for drill-down and replay evidence.
CREATE TABLE doris_canvas_execution_trace_ods_sink (
    trace_id BIGINT,
    tenant_id BIGINT,
    execution_id STRING,
    node_id STRING,
    node_type STRING,
    node_name STRING,
    status INT,
    input_data STRING,
    output_data STRING,
    error_msg STRING,
    started_at TIMESTAMP(3),
    finished_at TIMESTAMP(3),
    duration_ms BIGINT,
    created_at TIMESTAMP(3)
) WITH (
    'connector' = 'doris',
    'fenodes' = '${DORIS_FE_NODES}',
    'benodes' = '${DORIS_BE_NODES}',
    'jdbc-url' = '${DORIS_JDBC_URL}',
    'table.identifier' = '${DORIS_ODS_DATABASE}.canvas_execution_trace',
    'username' = '${DORIS_USERNAME}',
    'password' = '${DORIS_PASSWORD}',
    'sink.label-prefix' = 'mysql_canvas_trace_to_doris_ods_${TENANT_ID}${DORIS_LABEL_SUFFIX}',
    'sink.enable-delete' = 'false'
);

INSERT INTO doris_canvas_execution_trace_ods_sink
SELECT
    id AS trace_id,
    tenant_id,
    execution_id,
    node_id,
    node_type,
    node_name,
    status,
    input_data,
    output_data,
    error_msg,
    started_at,
    finished_at,
    duration_ms,
    -- Prefer finished_at when available so completed nodes order by finalization time.
    COALESCE(finished_at, started_at) AS created_at
FROM mysql_canvas_execution_trace_source;
