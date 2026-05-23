CREATE TABLE audience_data_source (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    url VARCHAR(1000) NOT NULL,
    username VARCHAR(200) NOT NULL,
    password VARCHAR(500) NOT NULL,
    driver_class_name VARCHAR(255),
    enabled TINYINT NOT NULL DEFAULT 1,
    created_by VARCHAR(100),
    created_at DATETIME,
    updated_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE audience_definition
    ADD COLUMN data_source_id BIGINT NULL AFTER data_source_type;

CREATE INDEX idx_audience_definition_source_type_id
    ON audience_definition (data_source_type, data_source_id);

INSERT INTO audience_data_source
    (name, description, url, username, password, driver_class_name, enabled, created_by, created_at, updated_at)
SELECT
    CONCAT('迁移 JDBC 数据源 ', ROW_NUMBER() OVER (ORDER BY config.url, config.username, config.driver_class_name)),
    '从 audience_definition 迁移而来',
    config.url,
    config.username,
    config.password,
    config.driver_class_name,
    1,
    'system',
    NOW(),
    NOW()
FROM (
    SELECT DISTINCT
        JSON_UNQUOTE(JSON_EXTRACT(data_source_config, '$.url')) AS url,
        JSON_UNQUOTE(JSON_EXTRACT(data_source_config, '$.username')) AS username,
        JSON_UNQUOTE(JSON_EXTRACT(data_source_config, '$.password')) AS password,
        COALESCE(
            JSON_UNQUOTE(JSON_EXTRACT(data_source_config, '$.driverClassName')),
            'com.mysql.cj.jdbc.Driver'
        ) AS driver_class_name
    FROM audience_definition
    WHERE data_source_type = 'JDBC'
      AND data_source_config IS NOT NULL
      AND JSON_EXTRACT(data_source_config, '$.url') IS NOT NULL
      AND JSON_EXTRACT(data_source_config, '$.username') IS NOT NULL
      AND JSON_EXTRACT(data_source_config, '$.password') IS NOT NULL
) config
LEFT JOIN audience_data_source existing
    ON existing.url = config.url
   AND existing.username = config.username
   AND existing.password = config.password
   AND COALESCE(existing.driver_class_name, 'com.mysql.cj.jdbc.Driver') = config.driver_class_name
WHERE existing.id IS NULL;

UPDATE audience_definition definition_row
JOIN audience_data_source data_source
    ON data_source.url = JSON_UNQUOTE(JSON_EXTRACT(definition_row.data_source_config, '$.url'))
   AND data_source.username = JSON_UNQUOTE(JSON_EXTRACT(definition_row.data_source_config, '$.username'))
   AND data_source.password = JSON_UNQUOTE(JSON_EXTRACT(definition_row.data_source_config, '$.password'))
   AND COALESCE(data_source.driver_class_name, 'com.mysql.cj.jdbc.Driver') = COALESCE(
        JSON_UNQUOTE(JSON_EXTRACT(definition_row.data_source_config, '$.driverClassName')),
        'com.mysql.cj.jdbc.Driver'
   )
SET definition_row.data_source_id = data_source.id,
    definition_row.data_source_config = JSON_OBJECT(
        'baseTable', JSON_UNQUOTE(JSON_EXTRACT(definition_row.data_source_config, '$.baseTable')),
        'userIdColumn', COALESCE(
            JSON_UNQUOTE(JSON_EXTRACT(definition_row.data_source_config, '$.userIdColumn')),
            'user_id'
        ),
        'maxRows', CASE
            WHEN JSON_EXTRACT(definition_row.data_source_config, '$.maxRows') IS NULL THEN NULL
            ELSE CAST(JSON_UNQUOTE(JSON_EXTRACT(definition_row.data_source_config, '$.maxRows')) AS UNSIGNED)
        END
    )
WHERE definition_row.data_source_type = 'JDBC'
  AND definition_row.data_source_config IS NOT NULL
  AND JSON_EXTRACT(definition_row.data_source_config, '$.url') IS NOT NULL
  AND JSON_EXTRACT(definition_row.data_source_config, '$.username') IS NOT NULL
  AND JSON_EXTRACT(definition_row.data_source_config, '$.password') IS NOT NULL;
