CREATE DATABASE IF NOT EXISTS canvas_ods;
CREATE DATABASE IF NOT EXISTS canvas_dwd;
CREATE DATABASE IF NOT EXISTS canvas_dws;

CREATE TABLE IF NOT EXISTS canvas_ods.cdp_event_log (
    tenant_id BIGINT NOT NULL,
    event_log_id BIGINT NOT NULL,
    event_code VARCHAR(128) NOT NULL,
    message_id VARCHAR(128),
    user_id VARCHAR(128),
    anonymous_id VARCHAR(128),
    session_id VARCHAR(128),
    device_id VARCHAR(128),
    platform VARCHAR(64),
    properties JSON,
    event_time DATETIME NOT NULL,
    received_at DATETIME NOT NULL
)
DUPLICATE KEY(tenant_id, event_log_id, event_code)
PARTITION BY RANGE(event_time) ()
DISTRIBUTED BY HASH(tenant_id, event_code) BUCKETS 16
PROPERTIES (
    "replication_num" = "3",
    "dynamic_partition.enable" = "true",
    "dynamic_partition.time_unit" = "DAY",
    "dynamic_partition.start" = "-90",
    "dynamic_partition.end" = "7",
    "dynamic_partition.prefix" = "p",
    "dynamic_partition.buckets" = "16",
    "dynamic_partition.create_history_partition" = "true"
);

CREATE TABLE IF NOT EXISTS canvas_dwd.cdp_user_event_fact (
    tenant_id BIGINT NOT NULL,
    user_id VARCHAR(128) NOT NULL,
    event_code VARCHAR(128) NOT NULL,
    event_time DATETIME NOT NULL,
    channel VARCHAR(64),
    canvas_id BIGINT,
    node_id VARCHAR(128),
    properties_json STRING,
    event_date DATE NOT NULL
)
DUPLICATE KEY(tenant_id, user_id, event_code, event_time)
PARTITION BY RANGE(event_date) ()
DISTRIBUTED BY HASH(tenant_id, user_id) BUCKETS 32
PROPERTIES (
    "replication_num" = "3",
    "dynamic_partition.enable" = "true",
    "dynamic_partition.time_unit" = "DAY",
    "dynamic_partition.start" = "-180",
    "dynamic_partition.end" = "7",
    "dynamic_partition.prefix" = "p",
    "dynamic_partition.buckets" = "32",
    "dynamic_partition.create_history_partition" = "true"
);

CREATE TABLE IF NOT EXISTS canvas_dws.user_event_metric_daily (
    stat_date DATE NOT NULL,
    tenant_id BIGINT NOT NULL,
    user_id VARCHAR(128) NOT NULL,
    event_code VARCHAR(128) NOT NULL,
    count_value BIGINT SUM DEFAULT "0",
    numeric_sum DOUBLE SUM DEFAULT "0",
    max_numeric DOUBLE MAX,
    latest_event_time DATETIME MAX
)
AGGREGATE KEY(stat_date, tenant_id, user_id, event_code)
PARTITION BY RANGE(stat_date) ()
DISTRIBUTED BY HASH(tenant_id, user_id) BUCKETS 32
PROPERTIES (
    "replication_num" = "3",
    "dynamic_partition.enable" = "true",
    "dynamic_partition.time_unit" = "DAY",
    "dynamic_partition.start" = "-730",
    "dynamic_partition.end" = "7",
    "dynamic_partition.prefix" = "p",
    "dynamic_partition.buckets" = "32",
    "dynamic_partition.create_history_partition" = "true"
);
