-- Local Doris bootstrap schema for canvas execution trace analytics.
-- ODS keeps raw trace rows, while DWS tables aggregate canvas/node daily metrics
-- for dashboards and realtime warehouse smoke tests.
CREATE DATABASE IF NOT EXISTS canvas_ods;
CREATE DATABASE IF NOT EXISTS canvas_dws;

-- Raw execution trace sink populated from MySQL/Flink pipeline outputs.
CREATE TABLE IF NOT EXISTS canvas_ods.canvas_execution_trace (
    trace_id BIGINT NOT NULL,
    tenant_id BIGINT,
    execution_id VARCHAR(64) NOT NULL,
    node_id VARCHAR(64) NOT NULL,
    node_type VARCHAR(64),
    node_name VARCHAR(128),
    status INT,
    input_data TEXT,
    output_data TEXT,
    error_msg TEXT,
    started_at DATETIME,
    finished_at DATETIME,
    duration_ms BIGINT,
    created_at DATETIME
)
DUPLICATE KEY(trace_id, tenant_id, execution_id)
DISTRIBUTED BY HASH(execution_id) BUCKETS 8
PROPERTIES ("replication_num" = "1");

-- Canvas-level daily aggregate used by operations dashboards.
CREATE TABLE IF NOT EXISTS canvas_dws.canvas_daily_stats (
    stat_date DATE NOT NULL,
    canvas_id BIGINT NOT NULL,
    canvas_name VARCHAR(256),
    trigger_type VARCHAR(64) NOT NULL,
    total_executions BIGINT SUM DEFAULT "0",
    success_count BIGINT SUM DEFAULT "0",
    fail_count BIGINT SUM DEFAULT "0",
    running_count BIGINT SUM DEFAULT "0",
    unique_users BIGINT SUM DEFAULT "0",
    total_duration_ms BIGINT SUM DEFAULT "0"
)
AGGREGATE KEY(stat_date, canvas_id, canvas_name, trigger_type)
DISTRIBUTED BY HASH(canvas_id) BUCKETS 8
PROPERTIES ("replication_num" = "1");

-- Node-level daily aggregate used to identify slow or failing journey nodes.
CREATE TABLE IF NOT EXISTS canvas_dws.node_daily_stats (
    stat_date DATE NOT NULL,
    canvas_id BIGINT NOT NULL,
    node_id VARCHAR(64) NOT NULL,
    node_type VARCHAR(64),
    node_name VARCHAR(128),
    total_entered BIGINT SUM DEFAULT "0",
    total_success BIGINT SUM DEFAULT "0",
    total_failed BIGINT SUM DEFAULT "0",
    total_skipped BIGINT SUM DEFAULT "0",
    total_duration_ms BIGINT SUM DEFAULT "0"
)
AGGREGATE KEY(stat_date, canvas_id, node_id, node_type, node_name)
DISTRIBUTED BY HASH(canvas_id) BUCKETS 8
PROPERTIES ("replication_num" = "1");
