CREATE TABLE data_source_config (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    name              VARCHAR(100) NOT NULL,
    type              VARCHAR(30) NOT NULL DEFAULT 'JDBC',
    url               VARCHAR(1000) NOT NULL,
    username          VARCHAR(200) NOT NULL,
    password          VARCHAR(500) NOT NULL,
    driver_class_name VARCHAR(200) NOT NULL DEFAULT 'com.mysql.cj.jdbc.Driver',
    description       VARCHAR(500),
    enabled           TINYINT NOT NULL DEFAULT 1,
    created_by        VARCHAR(100),
    created_at        DATETIME,
    updated_at        DATETIME,
    KEY idx_data_source_type_enabled (type, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO data_source_config
(name, type, url, username, password, driver_class_name, description, enabled, created_by, created_at, updated_at)
SELECT
    CONCAT('历史 JDBC 数据源 ', ROW_NUMBER() OVER (ORDER BY migrated.url, migrated.username, migrated.driver_class_name)),
    'JDBC',
    migrated.url,
    migrated.username,
    migrated.password,
    migrated.driver_class_name,
    '从历史人群 JDBC 配置迁移',
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
            NULLIF(JSON_UNQUOTE(JSON_EXTRACT(data_source_config, '$.driverClassName')), ''),
            'com.mysql.cj.jdbc.Driver'
        ) AS driver_class_name
    FROM audience_definition
    WHERE data_source_type = 'JDBC'
      AND JSON_EXTRACT(data_source_config, '$.dataSourceId') IS NULL
      AND JSON_EXTRACT(data_source_config, '$.url') IS NOT NULL
      AND JSON_EXTRACT(data_source_config, '$.username') IS NOT NULL
      AND JSON_EXTRACT(data_source_config, '$.password') IS NOT NULL
) migrated;

INSERT INTO data_source_config
(name, type, url, username, password, driver_class_name, description, enabled, created_by, created_at, updated_at)
SELECT
    '本地演示 MySQL',
    'JDBC',
    'jdbc:mysql://127.0.0.1:3306/canvas_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai',
    'root',
    'root',
    'com.mysql.cj.jdbc.Driver',
    '演示人群圈选数据源',
    1,
    'system',
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM data_source_config WHERE type = 'JDBC');

UPDATE audience_definition ad
JOIN data_source_config ds
  ON ds.type = 'JDBC'
 AND ds.url = JSON_UNQUOTE(JSON_EXTRACT(ad.data_source_config, '$.url'))
 AND ds.username = JSON_UNQUOTE(JSON_EXTRACT(ad.data_source_config, '$.username'))
 AND ds.password = JSON_UNQUOTE(JSON_EXTRACT(ad.data_source_config, '$.password'))
 AND ds.driver_class_name = COALESCE(
        NULLIF(JSON_UNQUOTE(JSON_EXTRACT(ad.data_source_config, '$.driverClassName')), ''),
        'com.mysql.cj.jdbc.Driver'
     )
SET ad.data_source_config = JSON_OBJECT(
    'dataSourceId', ds.id,
    'baseTable', JSON_UNQUOTE(JSON_EXTRACT(ad.data_source_config, '$.baseTable')),
    'userIdColumn', JSON_UNQUOTE(JSON_EXTRACT(ad.data_source_config, '$.userIdColumn')),
    'maxRows', JSON_EXTRACT(ad.data_source_config, '$.maxRows')
)
WHERE ad.data_source_type = 'JDBC'
  AND JSON_EXTRACT(ad.data_source_config, '$.dataSourceId') IS NULL
  AND JSON_EXTRACT(ad.data_source_config, '$.baseTable') IS NOT NULL;
