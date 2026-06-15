-- V72 adds performance tracking columns to event_log, but the legacy table
-- creation migration is no longer present in the chain.
SET @event_log_table_compatibility := IF(
  (
    SELECT version
    FROM flyway_schema_history
    WHERE success = 1
    ORDER BY installed_rank DESC
    LIMIT 1
  ) = '71',
  'CREATE TABLE IF NOT EXISTS event_log (
     id BIGINT PRIMARY KEY AUTO_INCREMENT,
     event_code VARCHAR(128) NOT NULL,
     user_id VARCHAR(64) NOT NULL,
     attributes TEXT NULL,
     canvas_triggered INT NOT NULL DEFAULT 0,
     canvas_count INT NOT NULL DEFAULT 0,
     created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
     KEY idx_event_log_event_user_time (event_code, user_id, created_at),
     KEY idx_event_log_created (created_at)
   ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''事件上报日志''',
  'DO 0'
);

PREPARE event_log_table_compatibility_stmt FROM @event_log_table_compatibility;
EXECUTE event_log_table_compatibility_stmt;
DEALLOCATE PREPARE event_log_table_compatibility_stmt;
