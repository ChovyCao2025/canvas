-- pipeline: doris_dwd_user_fact_to_dws_metric_daily
-- sink: canvas_dws.user_event_metric_daily
-- Purpose: aggregate normalized user-event facts into daily per-user metrics.
CREATE TABLE doris_cdp_user_event_fact_dwd_source (
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
    'password' = '${DORIS_PASSWORD}'
);

-- DWS daily metrics feed dashboards and audience/readiness checks.
CREATE TABLE doris_user_event_metric_daily_dws_sink (
    stat_date DATE,
    tenant_id BIGINT,
    user_id STRING,
    event_code STRING,
    count_value BIGINT,
    numeric_sum DOUBLE,
    max_numeric DOUBLE,
    latest_event_time TIMESTAMP(3)
) WITH (
    'connector' = 'doris',
    'fenodes' = '${DORIS_FE_NODES}',
    'benodes' = '${DORIS_BE_NODES}',
    'jdbc-url' = '${DORIS_JDBC_URL}',
    'table.identifier' = '${DORIS_DWS_DATABASE}.user_event_metric_daily',
    'username' = '${DORIS_USERNAME}',
    'password' = '${DORIS_PASSWORD}',
    'sink.label-prefix' = 'doris_dwd_user_fact_to_dws_metric_daily_${TENANT_ID}${DORIS_LABEL_SUFFIX}',
    'sink.enable-delete' = 'false'
);

INSERT INTO doris_user_event_metric_daily_dws_sink
SELECT
    event_date AS stat_date,
    tenant_id,
    user_id,
    event_code,
    COUNT(*) AS count_value,
    -- Numeric aggregation columns are reserved for future typed event-property metrics.
    CAST(0 AS DOUBLE) AS numeric_sum,
    MAX(CAST(NULL AS DOUBLE)) AS max_numeric,
    MAX(event_time) AS latest_event_time
FROM doris_cdp_user_event_fact_dwd_source
GROUP BY event_date, tenant_id, user_id, event_code;
