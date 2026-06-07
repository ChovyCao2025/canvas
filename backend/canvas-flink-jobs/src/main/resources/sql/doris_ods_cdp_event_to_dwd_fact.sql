-- pipeline: doris_ods_cdp_event_to_dwd_fact
-- sink: canvas_dwd.cdp_user_event_fact
CREATE TABLE doris_cdp_event_log_ods_source (
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
    'password' = '${DORIS_PASSWORD}'
);

CREATE TABLE doris_cdp_user_event_fact_dwd_sink (
    tenant_id BIGINT,
    user_id STRING,
    event_code STRING,
    event_time TIMESTAMP(3),
    channel STRING,
    canvas_id BIGINT,
    node_id STRING,
    properties_json STRING,
    event_date DATE
) WITH (
    'connector' = 'doris',
    'fenodes' = '${DORIS_FE_NODES}',
    'benodes' = '${DORIS_BE_NODES}',
    'jdbc-url' = '${DORIS_JDBC_URL}',
    'table.identifier' = '${DORIS_DWD_DATABASE}.cdp_user_event_fact',
    'username' = '${DORIS_USERNAME}',
    'password' = '${DORIS_PASSWORD}',
    'sink.label-prefix' = 'doris_ods_cdp_event_to_dwd_fact_${TENANT_ID}${DORIS_LABEL_SUFFIX}',
    'sink.enable-delete' = 'false'
);

INSERT INTO doris_cdp_user_event_fact_dwd_sink
SELECT
    tenant_id,
    COALESCE(NULLIF(user_id, ''), anonymous_id) AS user_id,
    event_code,
    event_time,
    JSON_VALUE(properties, '$.channel') AS channel,
    CAST(JSON_VALUE(properties, '$.canvasId') AS BIGINT) AS canvas_id,
    JSON_VALUE(properties, '$.nodeId') AS node_id,
    properties AS properties_json,
    CAST(event_time AS DATE) AS event_date
FROM doris_cdp_event_log_ods_source
WHERE event_code IS NOT NULL
  AND COALESCE(NULLIF(user_id, ''), anonymous_id) IS NOT NULL;
