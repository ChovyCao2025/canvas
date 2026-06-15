SET @notification_category_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'notification'
      AND column_name = 'category'
);
SET @notification_category_sql := IF(
    @notification_category_exists = 0,
    "ALTER TABLE notification ADD COLUMN category VARCHAR(32) NOT NULL DEFAULT 'TASK' AFTER type",
    'SELECT 1'
);
PREPARE notification_category_stmt FROM @notification_category_sql;
EXECUTE notification_category_stmt;
DEALLOCATE PREPARE notification_category_stmt;

SET @notification_severity_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'notification'
      AND column_name = 'severity'
);
SET @notification_severity_sql := IF(
    @notification_severity_exists = 0,
    "ALTER TABLE notification ADD COLUMN severity VARCHAR(16) NOT NULL DEFAULT 'INFO' AFTER category",
    'SELECT 1'
);
PREPARE notification_severity_stmt FROM @notification_severity_sql;
EXECUTE notification_severity_stmt;
DEALLOCATE PREPARE notification_severity_stmt;

SET @notification_status_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'notification'
      AND column_name = 'status'
);
SET @notification_status_sql := IF(
    @notification_status_exists = 0,
    "ALTER TABLE notification ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'UNREAD' AFTER severity",
    'SELECT 1'
);
PREPARE notification_status_stmt FROM @notification_status_sql;
EXECUTE notification_status_stmt;
DEALLOCATE PREPARE notification_status_stmt;

SET @notification_biz_type_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'notification'
      AND column_name = 'biz_type'
);
SET @notification_biz_type_sql := IF(
    @notification_biz_type_exists = 0,
    "ALTER TABLE notification ADD COLUMN biz_type VARCHAR(64) NULL AFTER task_id",
    'SELECT 1'
);
PREPARE notification_biz_type_stmt FROM @notification_biz_type_sql;
EXECUTE notification_biz_type_stmt;
DEALLOCATE PREPARE notification_biz_type_stmt;

SET @notification_biz_id_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'notification'
      AND column_name = 'biz_id'
);
SET @notification_biz_id_sql := IF(
    @notification_biz_id_exists = 0,
    "ALTER TABLE notification ADD COLUMN biz_id VARCHAR(128) NULL AFTER biz_type",
    'SELECT 1'
);
PREPARE notification_biz_id_stmt FROM @notification_biz_id_sql;
EXECUTE notification_biz_id_stmt;
DEALLOCATE PREPARE notification_biz_id_stmt;

SET @notification_action_label_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'notification'
      AND column_name = 'action_label'
);
SET @notification_action_label_sql := IF(
    @notification_action_label_exists = 0,
    "ALTER TABLE notification ADD COLUMN action_label VARCHAR(64) NULL AFTER target_url",
    'SELECT 1'
);
PREPARE notification_action_label_stmt FROM @notification_action_label_sql;
EXECUTE notification_action_label_stmt;
DEALLOCATE PREPARE notification_action_label_stmt;

SET @notification_action_url_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'notification'
      AND column_name = 'action_url'
);
SET @notification_action_url_sql := IF(
    @notification_action_url_exists = 0,
    "ALTER TABLE notification ADD COLUMN action_url VARCHAR(500) NULL AFTER action_label",
    'SELECT 1'
);
PREPARE notification_action_url_stmt FROM @notification_action_url_sql;
EXECUTE notification_action_url_stmt;
DEALLOCATE PREPARE notification_action_url_stmt;

SET @notification_dedup_key_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'notification'
      AND column_name = 'dedup_key'
);
SET @notification_dedup_key_sql := IF(
    @notification_dedup_key_exists = 0,
    "ALTER TABLE notification ADD COLUMN dedup_key VARCHAR(200) NULL AFTER biz_id",
    'SELECT 1'
);
PREPARE notification_dedup_key_stmt FROM @notification_dedup_key_sql;
EXECUTE notification_dedup_key_stmt;
DEALLOCATE PREPARE notification_dedup_key_stmt;

SET @notification_payload_json_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'notification'
      AND column_name = 'payload_json'
);
SET @notification_payload_json_sql := IF(
    @notification_payload_json_exists = 0,
    "ALTER TABLE notification ADD COLUMN payload_json JSON NULL AFTER dedup_key",
    'SELECT 1'
);
PREPARE notification_payload_json_stmt FROM @notification_payload_json_sql;
EXECUTE notification_payload_json_stmt;
DEALLOCATE PREPARE notification_payload_json_stmt;

SET @notification_archived_at_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'notification'
      AND column_name = 'archived_at'
);
SET @notification_archived_at_sql := IF(
    @notification_archived_at_exists = 0,
    "ALTER TABLE notification ADD COLUMN archived_at DATETIME NULL AFTER read_at",
    'SELECT 1'
);
PREPARE notification_archived_at_stmt FROM @notification_archived_at_sql;
EXECUTE notification_archived_at_stmt;
DEALLOCATE PREPARE notification_archived_at_stmt;

SET @notification_delivered_at_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'notification'
      AND column_name = 'delivered_at'
);
SET @notification_delivered_at_sql := IF(
    @notification_delivered_at_exists = 0,
    "ALTER TABLE notification ADD COLUMN delivered_at DATETIME NULL AFTER archived_at",
    'SELECT 1'
);
PREPARE notification_delivered_at_stmt FROM @notification_delivered_at_sql;
EXECUTE notification_delivered_at_stmt;
DEALLOCATE PREPARE notification_delivered_at_stmt;

SET @notification_user_dedup_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'notification'
      AND index_name = 'uk_notification_user_dedup'
);
SET @notification_user_dedup_sql := IF(
    @notification_user_dedup_exists = 0,
    'ALTER TABLE notification ADD UNIQUE KEY uk_notification_user_dedup (user_id, dedup_key)',
    'SELECT 1'
);
PREPARE notification_user_dedup_stmt FROM @notification_user_dedup_sql;
EXECUTE notification_user_dedup_stmt;
DEALLOCATE PREPARE notification_user_dedup_stmt;

SET @notification_user_category_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'notification'
      AND index_name = 'idx_notification_user_category'
);
SET @notification_user_category_sql := IF(
    @notification_user_category_exists = 0,
    'ALTER TABLE notification ADD INDEX idx_notification_user_category (user_id, category, created_at)',
    'SELECT 1'
);
PREPARE notification_user_category_stmt FROM @notification_user_category_sql;
EXECUTE notification_user_category_stmt;
DEALLOCATE PREPARE notification_user_category_stmt;

SET @notification_user_archived_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'notification'
      AND index_name = 'idx_notification_user_archived'
);
SET @notification_user_archived_sql := IF(
    @notification_user_archived_exists = 0,
    'ALTER TABLE notification ADD INDEX idx_notification_user_archived (user_id, archived_at, created_at)',
    'SELECT 1'
);
PREPARE notification_user_archived_stmt FROM @notification_user_archived_sql;
EXECUTE notification_user_archived_stmt;
DEALLOCATE PREPARE notification_user_archived_stmt;

SET @notification_severity_index_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'notification'
      AND index_name = 'idx_notification_severity'
);
SET @notification_severity_index_sql := IF(
    @notification_severity_index_exists = 0,
    'ALTER TABLE notification ADD INDEX idx_notification_severity (severity, created_at)',
    'SELECT 1'
);
PREPARE notification_severity_index_stmt FROM @notification_severity_index_sql;
EXECUTE notification_severity_index_stmt;
DEALLOCATE PREPARE notification_severity_index_stmt;
