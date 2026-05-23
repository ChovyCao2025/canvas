CREATE TABLE async_task_subscription (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(100) NOT NULL,
    created_at DATETIME,
    UNIQUE KEY uk_async_task_subscription (task_id, user_id),
    INDEX idx_async_task_subscription_user (user_id, created_at),
    INDEX idx_async_task_subscription_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
