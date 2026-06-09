
-- Replace unsafe local demo credentials while limiting the update to loopback MySQL examples.
UPDATE data_source_config
SET username = 'canvas_demo',
    password = 'canvas_demo_local_password',
    updated_at = NOW()
WHERE username = 'root'
  AND password = 'root'
  AND url LIKE 'jdbc:mysql://127.0.0.1:%';

-- Keep embedded JDBC audience configs aligned with the sanitized datasource defaults.
UPDATE audience_definition
SET data_source_config = JSON_SET(
        CAST(data_source_config AS JSON),
        '$.username', 'canvas_demo',
        '$.password', 'canvas_demo_local_password')
WHERE data_source_type = 'JDBC'
  AND JSON_VALID(data_source_config)
  AND JSON_UNQUOTE(JSON_EXTRACT(data_source_config, '$.username')) = 'root'
  AND JSON_UNQUOTE(JSON_EXTRACT(data_source_config, '$.password')) = 'root';
