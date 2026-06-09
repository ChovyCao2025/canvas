-- pipeline: risk_realtime_features
-- source: canvas-risk-events
-- redis-sink-key: risk:feature:{tenantId}:{featureKey}:{subjectHash}
-- doris-sink: canvas_dws.risk_realtime_feature_snapshot
-- Purpose: derive first online risk-control features for Redis lookup and Doris audit snapshots.

CREATE TABLE risk_event_source (
    tenant_id BIGINT,
    user_id STRING,
    device_id STRING,
    ip STRING,
    event_type STRING,
    event_result STRING,
    benefit_amount DOUBLE,
    event_time TIMESTAMP(3),
    WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND
) WITH (
    'connector' = 'rocketmq',
    'topic' = 'canvas-risk-events',
    'consumerGroup' = 'canvas-risk-realtime-feature-job'
);

CREATE TABLE risk_feature_redis_sink (
    tenant_id BIGINT,
    feature_key STRING,
    subject_hash STRING,
    feature_value STRING,
    redis_key_pattern STRING,
    updated_at TIMESTAMP(3)
) WITH (
    'connector' = 'redis',
    'key.pattern' = 'risk:feature:{tenantId}:{featureKey}:{subjectHash}'
);

CREATE TABLE risk_feature_doris_sink (
    tenant_id BIGINT,
    feature_key STRING,
    subject_hash STRING,
    feature_value STRING,
    window_start TIMESTAMP(3),
    window_end TIMESTAMP(3),
    updated_at TIMESTAMP(3)
) WITH (
    'connector' = 'doris',
    'fenodes' = '${DORIS_FE_NODES}',
    'benodes' = '${DORIS_BE_NODES}',
    'jdbc-url' = '${DORIS_JDBC_URL}',
    'table.identifier' = 'canvas_dws.risk_realtime_feature_snapshot',
    'username' = '${DORIS_USERNAME}',
    'password' = '${DORIS_PASSWORD}'
);

INSERT INTO risk_feature_redis_sink
SELECT tenant_id, feature_key, subject_hash, CAST(feature_value AS STRING), 'risk:feature:{tenantId}:{featureKey}:{subjectHash}', window_end
FROM (
    SELECT tenant_id, 'user.fail_count_1d' AS feature_key, user_id AS subject_hash, COUNT(*) AS feature_value, window_start, window_end
    FROM TABLE(TUMBLE(TABLE risk_event_source, DESCRIPTOR(event_time), INTERVAL '1' DAY))
    WHERE event_result = 'FAIL' AND user_id IS NOT NULL
    GROUP BY tenant_id, user_id, window_start, window_end

    UNION ALL
    SELECT tenant_id, 'user.success_count_1d' AS feature_key, user_id AS subject_hash, COUNT(*) AS feature_value, window_start, window_end
    FROM TABLE(TUMBLE(TABLE risk_event_source, DESCRIPTOR(event_time), INTERVAL '1' DAY))
    WHERE event_result = 'SUCCESS' AND user_id IS NOT NULL
    GROUP BY tenant_id, user_id, window_start, window_end

    UNION ALL
    SELECT tenant_id, 'device.change_user_1d' AS feature_key, device_id AS subject_hash, COUNT(DISTINCT user_id) AS feature_value, window_start, window_end
    FROM TABLE(TUMBLE(TABLE risk_event_source, DESCRIPTOR(event_time), INTERVAL '1' DAY))
    WHERE device_id IS NOT NULL
    GROUP BY tenant_id, device_id, window_start, window_end

    UNION ALL
    SELECT tenant_id, 'ip.change_user_1h' AS feature_key, ip AS subject_hash, COUNT(DISTINCT user_id) AS feature_value, window_start, window_end
    FROM TABLE(TUMBLE(TABLE risk_event_source, DESCRIPTOR(event_time), INTERVAL '1' HOUR))
    WHERE ip IS NOT NULL
    GROUP BY tenant_id, ip, window_start, window_end

    UNION ALL
    SELECT tenant_id, 'benefit.issue_amount_1d' AS feature_key, user_id AS subject_hash, SUM(benefit_amount) AS feature_value, window_start, window_end
    FROM TABLE(TUMBLE(TABLE risk_event_source, DESCRIPTOR(event_time), INTERVAL '1' DAY))
    WHERE event_type = 'BENEFIT_ISSUED' AND user_id IS NOT NULL
    GROUP BY tenant_id, user_id, window_start, window_end
);

INSERT INTO risk_feature_doris_sink
SELECT tenant_id, feature_key, subject_hash, feature_value, updated_at, updated_at, updated_at
FROM risk_feature_redis_sink;
