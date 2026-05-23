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
    (id, name, description, url, username, password, driver_class_name, enabled, created_by, created_at, updated_at)
VALUES
    (
        90001,
        '演示 JDBC 数据源',
        '从历史 audience_definition 迁移而来',
        'jdbc:mysql://127.0.0.1:3306/canvas_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai',
        'root',
        'root',
        'com.mysql.cj.jdbc.Driver',
        1,
        'system',
        NOW(),
        NOW()
    )
ON DUPLICATE KEY UPDATE
    updated_at = VALUES(updated_at);

UPDATE audience_definition
SET data_source_id = 90001,
    data_source_config = '{"baseTable":"audience_demo_user","userIdColumn":"user_id","maxRows":10000}'
WHERE id IN (90001, 90002, 90003);
