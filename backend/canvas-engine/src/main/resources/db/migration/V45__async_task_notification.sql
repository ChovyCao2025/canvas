CREATE TABLE async_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id VARCHAR(64) NOT NULL UNIQUE,
    task_type VARCHAR(50) NOT NULL,
    biz_type VARCHAR(50) NOT NULL,
    biz_id VARCHAR(64) NOT NULL,
    title VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL,
    progress INT NOT NULL DEFAULT 0,
    result_summary VARCHAR(1000),
    error_msg VARCHAR(1000),
    created_by VARCHAR(100),
    started_at DATETIME,
    finished_at DATETIME,
    created_at DATETIME,
    updated_at DATETIME,
    active_key VARCHAR(200) GENERATED ALWAYS AS (CASE WHEN status IN ('QUEUED','RUNNING') THEN CONCAT(task_type, ':', biz_type, ':', biz_id) ELSE NULL END) STORED,
    UNIQUE KEY uk_async_task_active (active_key),
    INDEX idx_async_task_biz (biz_type, biz_id),
    INDEX idx_async_task_status (status),
    INDEX idx_async_task_creator (created_by, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    notification_id VARCHAR(64) NOT NULL UNIQUE,
    user_id VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content VARCHAR(1000),
    target_url VARCHAR(500),
    task_id VARCHAR(64),
    read_at DATETIME,
    created_at DATETIME,
    INDEX idx_notification_user_read (user_id, read_at, created_at),
    INDEX idx_notification_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE async_task_subscription (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(100) NOT NULL,
    created_at DATETIME,
    UNIQUE KEY uk_async_task_subscription (task_id, user_id),
    INDEX idx_async_task_subscription_user (user_id, created_at),
    INDEX idx_async_task_subscription_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
