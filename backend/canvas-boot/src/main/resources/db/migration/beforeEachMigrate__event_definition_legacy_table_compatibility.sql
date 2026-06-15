-- V20 is currently a no-op, while V102 expects the legacy event_definition
-- table to exist. Create the base table immediately before V102 on empty or
-- replayed schemas without changing the historical versioned migration.
SET @event_definition_table_compatibility := IF(
  (
    SELECT version
    FROM flyway_schema_history
    WHERE success = 1
    ORDER BY installed_rank DESC
    LIMIT 1
  ) = '101',
  'CREATE TABLE IF NOT EXISTS event_definition (
     id BIGINT PRIMARY KEY AUTO_INCREMENT,
     name VARCHAR(128) NOT NULL,
     event_code VARCHAR(128) NOT NULL,
     attributes JSON NULL,
     description VARCHAR(500) NULL,
     enabled TINYINT NOT NULL DEFAULT 1,
     created_by VARCHAR(128) NULL,
     created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
     updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
     UNIQUE KEY uk_event_definition_code (event_code),
     KEY idx_event_definition_enabled (enabled, updated_at)
   ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''Business event definitions''',
  'DO 0'
);

PREPARE event_definition_table_compatibility_stmt FROM @event_definition_table_compatibility;
EXECUTE event_definition_table_compatibility_stmt;
DEALLOCATE PREPARE event_definition_table_compatibility_stmt;
