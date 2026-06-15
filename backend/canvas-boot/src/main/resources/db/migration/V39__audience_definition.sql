CREATE TABLE audience_definition (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(100) NOT NULL,
    description         VARCHAR(500),
    rule_json           TEXT NOT NULL,
    engine_type         VARCHAR(20) NOT NULL DEFAULT 'AVIATOR',
    data_source_type    VARCHAR(20) NOT NULL DEFAULT 'TAGGER_API',
    data_source_config  TEXT,
    evaluation_strategy VARCHAR(20) NOT NULL DEFAULT 'OFFLINE_BATCH',
    cron_expression     VARCHAR(100),
    enabled             TINYINT NOT NULL DEFAULT 1,
    created_by          VARCHAR(100),
    created_at          DATETIME,
    updated_at          DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE audience_stat (
    audience_id     BIGINT PRIMARY KEY,
    estimated_size  BIGINT,
    bitmap_size_kb  INT,
    computed_at     DATETIME,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING | COMPUTING | READY | FAILED',
    error_msg       VARCHAR(500)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
