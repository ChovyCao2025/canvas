-- pipeline: mysql_cdp_event_log_to_doris_ods
-- sink: canvas_ods.cdp_event_log
-- Purpose: stream accepted CDP event ledger rows from MySQL into Doris ODS for downstream DWD facts.
CREATE TABLE mysql_cdp_event_log_source (
    id BIGINT,
    tenant_id BIGINT,
    write_key_id BIGINT,
    message_id STRING,
    event_type STRING,
    event_code STRING,
    user_id STRING,
    anonymous_id STRING,
    session_id STRING,
    device_id STRING,
    platform STRING,
    sdk_context STRING,
    properties STRING,
    idempotency_key STRING,
    event_time TIMESTAMP(3),
    sent_at TIMESTAMP(3),
    received_at TIMESTAMP(3),
    status STRING,
    error_message STRING,
    created_at TIMESTAMP(3),
    PRIMARY KEY (id) NOT ENFORCED
) WITH (
    'connector' = 'mysql-cdc',
    'hostname' = '${MYSQL_HOSTNAME}',
    'port' = '${MYSQL_PORT}',
    'username' = '${MYSQL_USERNAME}',
    'password' = '${MYSQL_PASSWORD}',
    'database-name' = '${MYSQL_DATABASE}',
    'table-name' = 'cdp_event_log',
    'server-time-zone' = 'Asia/Shanghai',
    'scan.startup.mode' = 'initial'
);

-- Doris ODS stores only fields needed by realtime warehouse transformations and lineage checks.
CREATE TABLE doris_cdp_event_log_ods_sink (
    tenant_id BIGINT,
    event_log_id BIGINT,
    event_code STRING,
    message_id STRING,
    user_id STRING,
    anonymous_id STRING,
    session_id STRING,
    device_id STRING,
    platform STRING,
    properties STRING,
    event_time TIMESTAMP(3),
    received_at TIMESTAMP(3)
) WITH (
    'connector' = 'doris',
    'fenodes' = '${DORIS_FE_NODES}',
    'benodes' = '${DORIS_BE_NODES}',
    'jdbc-url' = '${DORIS_JDBC_URL}',
    'table.identifier' = '${DORIS_ODS_DATABASE}.cdp_event_log',
    'username' = '${DORIS_USERNAME}',
    'password' = '${DORIS_PASSWORD}',
    'sink.label-prefix' = 'mysql_cdp_event_log_to_doris_ods_${TENANT_ID}${DORIS_LABEL_SUFFIX}',
    'sink.enable-delete' = 'false'
);

INSERT INTO doris_cdp_event_log_ods_sink
SELECT
    tenant_id,
    id AS event_log_id,
    event_code,
    message_id,
    user_id,
    anonymous_id,
    session_id,
    device_id,
    platform,
    properties,
    event_time,
    received_at
FROM mysql_cdp_event_log_source
-- Only accepted events are analytical facts; rejected rows stay in MySQL for ingestion diagnostics.
WHERE status = 'ACCEPTED';
