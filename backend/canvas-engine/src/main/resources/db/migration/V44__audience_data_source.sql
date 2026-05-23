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
