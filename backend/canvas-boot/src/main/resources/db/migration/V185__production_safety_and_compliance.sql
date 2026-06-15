SET @audience_tenant_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'audience_definition'
      AND column_name = 'tenant_id'
);
SET @audience_tenant_sql := IF(
    @audience_tenant_exists = 0,
    "ALTER TABLE audience_definition ADD COLUMN tenant_id BIGINT NULL AFTER id",
    "SELECT 1"
);
PREPARE audience_tenant_stmt FROM @audience_tenant_sql;
EXECUTE audience_tenant_stmt;
DEALLOCATE PREPARE audience_tenant_stmt;

SET @notification_tenant_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'notification'
      AND column_name = 'tenant_id'
);
SET @notification_tenant_sql := IF(
    @notification_tenant_exists = 0,
    "ALTER TABLE notification ADD COLUMN tenant_id BIGINT NULL AFTER id",
    "SELECT 1"
);
PREPARE notification_tenant_stmt FROM @notification_tenant_sql;
EXECUTE notification_tenant_stmt;
DEALLOCATE PREPARE notification_tenant_stmt;

SET @customer_profile_tenant_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'customer_profile'
      AND column_name = 'tenant_id'
);
SET @customer_profile_tenant_sql := IF(
    @customer_profile_tenant_exists = 0,
    "ALTER TABLE customer_profile ADD COLUMN tenant_id BIGINT NULL AFTER id",
    "SELECT 1"
);
PREPARE customer_profile_tenant_stmt FROM @customer_profile_tenant_sql;
EXECUTE customer_profile_tenant_stmt;
DEALLOCATE PREPARE customer_profile_tenant_stmt;

SET @customer_channel_tenant_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'customer_channel'
      AND column_name = 'tenant_id'
);
SET @customer_channel_tenant_sql := IF(
    @customer_channel_tenant_exists = 0,
    "ALTER TABLE customer_channel ADD COLUMN tenant_id BIGINT NULL AFTER id",
    "SELECT 1"
);
PREPARE customer_channel_tenant_stmt FROM @customer_channel_tenant_sql;
EXECUTE customer_channel_tenant_stmt;
DEALLOCATE PREPARE customer_channel_tenant_stmt;

SET @marketing_consent_tenant_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'marketing_consent'
      AND column_name = 'tenant_id'
);
SET @marketing_consent_tenant_sql := IF(
    @marketing_consent_tenant_exists = 0,
    "ALTER TABLE marketing_consent ADD COLUMN tenant_id BIGINT NULL AFTER id",
    "SELECT 1"
);
PREPARE marketing_consent_tenant_stmt FROM @marketing_consent_tenant_sql;
EXECUTE marketing_consent_tenant_stmt;
DEALLOCATE PREPARE marketing_consent_tenant_stmt;

SET @marketing_suppression_tenant_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'marketing_suppression'
      AND column_name = 'tenant_id'
);
SET @marketing_suppression_tenant_sql := IF(
    @marketing_suppression_tenant_exists = 0,
    "ALTER TABLE marketing_suppression ADD COLUMN tenant_id BIGINT NULL AFTER id",
    "SELECT 1"
);
PREPARE marketing_suppression_tenant_stmt FROM @marketing_suppression_tenant_sql;
EXECUTE marketing_suppression_tenant_stmt;
DEALLOCATE PREPARE marketing_suppression_tenant_stmt;

SET @message_send_tenant_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'message_send_record'
      AND column_name = 'tenant_id'
);
SET @message_send_tenant_sql := IF(
    @message_send_tenant_exists = 0,
    "ALTER TABLE message_send_record ADD COLUMN tenant_id BIGINT NULL AFTER id",
    "SELECT 1"
);
PREPARE message_send_tenant_stmt FROM @message_send_tenant_sql;
EXECUTE message_send_tenant_stmt;
DEALLOCATE PREPARE message_send_tenant_stmt;

SET @cdp_profile_tenant_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'cdp_user_profile'
      AND column_name = 'tenant_id'
);
SET @cdp_profile_tenant_sql := IF(
    @cdp_profile_tenant_exists = 0,
    "ALTER TABLE cdp_user_profile ADD COLUMN tenant_id BIGINT NULL AFTER id",
    "SELECT 1"
);
PREPARE cdp_profile_tenant_stmt FROM @cdp_profile_tenant_sql;
EXECUTE cdp_profile_tenant_stmt;
DEALLOCATE PREPARE cdp_profile_tenant_stmt;

SET @cdp_identity_tenant_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'cdp_user_identity'
      AND column_name = 'tenant_id'
);
SET @cdp_identity_tenant_sql := IF(
    @cdp_identity_tenant_exists = 0,
    "ALTER TABLE cdp_user_identity ADD COLUMN tenant_id BIGINT NULL AFTER id",
    "SELECT 1"
);
PREPARE cdp_identity_tenant_stmt FROM @cdp_identity_tenant_sql;
EXECUTE cdp_identity_tenant_stmt;
DEALLOCATE PREPARE cdp_identity_tenant_stmt;

SET @cdp_tag_tenant_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'cdp_user_tag'
      AND column_name = 'tenant_id'
);
SET @cdp_tag_tenant_sql := IF(
    @cdp_tag_tenant_exists = 0,
    "ALTER TABLE cdp_user_tag ADD COLUMN tenant_id BIGINT NULL AFTER id",
    "SELECT 1"
);
PREPARE cdp_tag_tenant_stmt FROM @cdp_tag_tenant_sql;
EXECUTE cdp_tag_tenant_stmt;
DEALLOCATE PREPARE cdp_tag_tenant_stmt;

SET @cdp_tag_history_tenant_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'cdp_user_tag_history'
      AND column_name = 'tenant_id'
);
SET @cdp_tag_history_tenant_sql := IF(
    @cdp_tag_history_tenant_exists = 0,
    "ALTER TABLE cdp_user_tag_history ADD COLUMN tenant_id BIGINT NULL AFTER id",
    "SELECT 1"
);
PREPARE cdp_tag_history_tenant_stmt FROM @cdp_tag_history_tenant_sql;
EXECUTE cdp_tag_history_tenant_stmt;
DEALLOCATE PREPARE cdp_tag_history_tenant_stmt;

SET @cdp_tag_operation_tenant_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'cdp_tag_operation'
      AND column_name = 'tenant_id'
);
SET @cdp_tag_operation_tenant_sql := IF(
    @cdp_tag_operation_tenant_exists = 0,
    "ALTER TABLE cdp_tag_operation ADD COLUMN tenant_id BIGINT NULL AFTER id",
    "SELECT 1"
);
PREPARE cdp_tag_operation_tenant_stmt FROM @cdp_tag_operation_tenant_sql;
EXECUTE cdp_tag_operation_tenant_stmt;
DEALLOCATE PREPARE cdp_tag_operation_tenant_stmt;

UPDATE audience_definition SET tenant_id = (SELECT id FROM tenant WHERE tenant_key = 'default') WHERE tenant_id IS NULL;
UPDATE notification SET tenant_id = (SELECT id FROM tenant WHERE tenant_key = 'default') WHERE tenant_id IS NULL;
UPDATE customer_profile SET tenant_id = (SELECT id FROM tenant WHERE tenant_key = 'default') WHERE tenant_id IS NULL;
UPDATE customer_channel SET tenant_id = (SELECT id FROM tenant WHERE tenant_key = 'default') WHERE tenant_id IS NULL;
UPDATE marketing_consent SET tenant_id = (SELECT id FROM tenant WHERE tenant_key = 'default') WHERE tenant_id IS NULL;
UPDATE marketing_suppression SET tenant_id = (SELECT id FROM tenant WHERE tenant_key = 'default') WHERE tenant_id IS NULL;
UPDATE message_send_record r JOIN canvas c ON c.id = r.canvas_id SET r.tenant_id = c.tenant_id WHERE r.tenant_id IS NULL;
UPDATE message_send_record SET tenant_id = (SELECT id FROM tenant WHERE tenant_key = 'default') WHERE tenant_id IS NULL;
UPDATE cdp_user_profile SET tenant_id = (SELECT id FROM tenant WHERE tenant_key = 'default') WHERE tenant_id IS NULL;
UPDATE cdp_user_identity SET tenant_id = (SELECT id FROM tenant WHERE tenant_key = 'default') WHERE tenant_id IS NULL;
UPDATE cdp_user_tag SET tenant_id = (SELECT id FROM tenant WHERE tenant_key = 'default') WHERE tenant_id IS NULL;
UPDATE cdp_user_tag_history SET tenant_id = (SELECT id FROM tenant WHERE tenant_key = 'default') WHERE tenant_id IS NULL;
UPDATE cdp_tag_operation SET tenant_id = (SELECT id FROM tenant WHERE tenant_key = 'default') WHERE tenant_id IS NULL;

SET @customer_profile_old_unique_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'customer_profile'
      AND index_name = 'uk_customer_profile_user'
);
SET @customer_profile_old_unique_sql := IF(
    @customer_profile_old_unique_exists = 0,
    "SELECT 1",
    "ALTER TABLE customer_profile DROP INDEX uk_customer_profile_user"
);
PREPARE customer_profile_old_unique_stmt FROM @customer_profile_old_unique_sql;
EXECUTE customer_profile_old_unique_stmt;
DEALLOCATE PREPARE customer_profile_old_unique_stmt;

SET @customer_channel_old_unique_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'customer_channel'
      AND index_name = 'uk_customer_channel'
);
SET @customer_channel_old_unique_sql := IF(
    @customer_channel_old_unique_exists = 0,
    "SELECT 1",
    "ALTER TABLE customer_channel DROP INDEX uk_customer_channel"
);
PREPARE customer_channel_old_unique_stmt FROM @customer_channel_old_unique_sql;
EXECUTE customer_channel_old_unique_stmt;
DEALLOCATE PREPARE customer_channel_old_unique_stmt;

SET @marketing_consent_old_unique_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'marketing_consent'
      AND index_name = 'uk_marketing_consent'
);
SET @marketing_consent_old_unique_sql := IF(
    @marketing_consent_old_unique_exists = 0,
    "SELECT 1",
    "ALTER TABLE marketing_consent DROP INDEX uk_marketing_consent"
);
PREPARE marketing_consent_old_unique_stmt FROM @marketing_consent_old_unique_sql;
EXECUTE marketing_consent_old_unique_stmt;
DEALLOCATE PREPARE marketing_consent_old_unique_stmt;

SET @notification_old_dedup_unique_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'notification'
      AND index_name = 'uk_notification_user_dedup'
);
SET @notification_old_dedup_unique_sql := IF(
    @notification_old_dedup_unique_exists = 0,
    "SELECT 1",
    "ALTER TABLE notification DROP INDEX uk_notification_user_dedup"
);
PREPARE notification_old_dedup_unique_stmt FROM @notification_old_dedup_unique_sql;
EXECUTE notification_old_dedup_unique_stmt;
DEALLOCATE PREPARE notification_old_dedup_unique_stmt;

SET @cdp_profile_old_unique_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'cdp_user_profile'
      AND index_name = 'uk_cdp_user_id'
);
SET @cdp_profile_old_unique_sql := IF(
    @cdp_profile_old_unique_exists = 0,
    "SELECT 1",
    "ALTER TABLE cdp_user_profile DROP INDEX uk_cdp_user_id"
);
PREPARE cdp_profile_old_unique_stmt FROM @cdp_profile_old_unique_sql;
EXECUTE cdp_profile_old_unique_stmt;
DEALLOCATE PREPARE cdp_profile_old_unique_stmt;

SET @cdp_identity_old_unique_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'cdp_user_identity'
      AND index_name = 'uk_identity'
);
SET @cdp_identity_old_unique_sql := IF(
    @cdp_identity_old_unique_exists = 0,
    "SELECT 1",
    "ALTER TABLE cdp_user_identity DROP INDEX uk_identity"
);
PREPARE cdp_identity_old_unique_stmt FROM @cdp_identity_old_unique_sql;
EXECUTE cdp_identity_old_unique_stmt;
DEALLOCATE PREPARE cdp_identity_old_unique_stmt;

SET @cdp_tag_old_unique_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'cdp_user_tag'
      AND index_name = 'uk_user_tag'
);
SET @cdp_tag_old_unique_sql := IF(
    @cdp_tag_old_unique_exists = 0,
    "SELECT 1",
    "ALTER TABLE cdp_user_tag DROP INDEX uk_user_tag"
);
PREPARE cdp_tag_old_unique_stmt FROM @cdp_tag_old_unique_sql;
EXECUTE cdp_tag_old_unique_stmt;
DEALLOCATE PREPARE cdp_tag_old_unique_stmt;

SET @cdp_tag_history_old_unique_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'cdp_user_tag_history'
      AND index_name = 'uk_tag_history_idempotency'
);
SET @cdp_tag_history_old_unique_sql := IF(
    @cdp_tag_history_old_unique_exists = 0,
    "SELECT 1",
    "ALTER TABLE cdp_user_tag_history DROP INDEX uk_tag_history_idempotency"
);
PREPARE cdp_tag_history_old_unique_stmt FROM @cdp_tag_history_old_unique_sql;
EXECUTE cdp_tag_history_old_unique_stmt;
DEALLOCATE PREPARE cdp_tag_history_old_unique_stmt;

SET @audience_tenant_idx_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'audience_definition'
      AND index_name = 'idx_audience_tenant_enabled'
);
SET @audience_tenant_idx_sql := IF(
    @audience_tenant_idx_exists = 0,
    "ALTER TABLE audience_definition ADD KEY idx_audience_tenant_enabled (tenant_id, enabled, updated_at)",
    "SELECT 1"
);
PREPARE audience_tenant_idx_stmt FROM @audience_tenant_idx_sql;
EXECUTE audience_tenant_idx_stmt;
DEALLOCATE PREPARE audience_tenant_idx_stmt;

SET @notification_tenant_idx_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'notification'
      AND index_name = 'idx_notification_tenant_user_status'
);
SET @notification_tenant_idx_sql := IF(
    @notification_tenant_idx_exists = 0,
    "ALTER TABLE notification ADD KEY idx_notification_tenant_user_status (tenant_id, user_id, status, created_at)",
    "SELECT 1"
);
PREPARE notification_tenant_idx_stmt FROM @notification_tenant_idx_sql;
EXECUTE notification_tenant_idx_stmt;
DEALLOCATE PREPARE notification_tenant_idx_stmt;

SET @notification_tenant_dedup_unique_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'notification'
      AND index_name = 'uk_notification_tenant_user_dedup'
);
SET @notification_tenant_dedup_unique_sql := IF(
    @notification_tenant_dedup_unique_exists = 0,
    "ALTER TABLE notification ADD UNIQUE KEY uk_notification_tenant_user_dedup (tenant_id, user_id, dedup_key)",
    "SELECT 1"
);
PREPARE notification_tenant_dedup_unique_stmt FROM @notification_tenant_dedup_unique_sql;
EXECUTE notification_tenant_dedup_unique_stmt;
DEALLOCATE PREPARE notification_tenant_dedup_unique_stmt;

SET @customer_profile_tenant_unique_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'customer_profile'
      AND index_name = 'uk_customer_profile_tenant_user'
);
SET @customer_profile_tenant_unique_sql := IF(
    @customer_profile_tenant_unique_exists = 0,
    "ALTER TABLE customer_profile ADD UNIQUE KEY uk_customer_profile_tenant_user (tenant_id, user_id)",
    "SELECT 1"
);
PREPARE customer_profile_tenant_unique_stmt FROM @customer_profile_tenant_unique_sql;
EXECUTE customer_profile_tenant_unique_stmt;
DEALLOCATE PREPARE customer_profile_tenant_unique_stmt;

SET @customer_channel_tenant_unique_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'customer_channel'
      AND index_name = 'uk_customer_channel_tenant_user_channel'
);
SET @customer_channel_tenant_unique_sql := IF(
    @customer_channel_tenant_unique_exists = 0,
    "ALTER TABLE customer_channel ADD UNIQUE KEY uk_customer_channel_tenant_user_channel (tenant_id, user_id, channel)",
    "SELECT 1"
);
PREPARE customer_channel_tenant_unique_stmt FROM @customer_channel_tenant_unique_sql;
EXECUTE customer_channel_tenant_unique_stmt;
DEALLOCATE PREPARE customer_channel_tenant_unique_stmt;

SET @marketing_consent_tenant_unique_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'marketing_consent'
      AND index_name = 'uk_marketing_consent_tenant_user_channel'
);
SET @marketing_consent_tenant_unique_sql := IF(
    @marketing_consent_tenant_unique_exists = 0,
    "ALTER TABLE marketing_consent ADD UNIQUE KEY uk_marketing_consent_tenant_user_channel (tenant_id, user_id, channel)",
    "SELECT 1"
);
PREPARE marketing_consent_tenant_unique_stmt FROM @marketing_consent_tenant_unique_sql;
EXECUTE marketing_consent_tenant_unique_stmt;
DEALLOCATE PREPARE marketing_consent_tenant_unique_stmt;

SET @suppression_tenant_idx_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'marketing_suppression'
      AND index_name = 'idx_suppression_tenant_user_channel'
);
SET @suppression_tenant_idx_sql := IF(
    @suppression_tenant_idx_exists = 0,
    "ALTER TABLE marketing_suppression ADD KEY idx_suppression_tenant_user_channel (tenant_id, user_id, channel, active)",
    "SELECT 1"
);
PREPARE suppression_tenant_idx_stmt FROM @suppression_tenant_idx_sql;
EXECUTE suppression_tenant_idx_stmt;
DEALLOCATE PREPARE suppression_tenant_idx_stmt;

SET @message_send_tenant_idx_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'message_send_record'
      AND index_name = 'idx_message_send_tenant_canvas_created'
);
SET @message_send_tenant_idx_sql := IF(
    @message_send_tenant_idx_exists = 0,
    "ALTER TABLE message_send_record ADD KEY idx_message_send_tenant_canvas_created (tenant_id, canvas_id, created_at)",
    "SELECT 1"
);
PREPARE message_send_tenant_idx_stmt FROM @message_send_tenant_idx_sql;
EXECUTE message_send_tenant_idx_stmt;
DEALLOCATE PREPARE message_send_tenant_idx_stmt;

SET @cdp_profile_tenant_unique_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'cdp_user_profile'
      AND index_name = 'uk_cdp_profile_tenant_user'
);
SET @cdp_profile_tenant_unique_sql := IF(
    @cdp_profile_tenant_unique_exists = 0,
    "ALTER TABLE cdp_user_profile ADD UNIQUE KEY uk_cdp_profile_tenant_user (tenant_id, user_id)",
    "SELECT 1"
);
PREPARE cdp_profile_tenant_unique_stmt FROM @cdp_profile_tenant_unique_sql;
EXECUTE cdp_profile_tenant_unique_stmt;
DEALLOCATE PREPARE cdp_profile_tenant_unique_stmt;

SET @cdp_profile_tenant_seen_idx_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'cdp_user_profile'
      AND index_name = 'idx_cdp_profile_tenant_seen'
);
SET @cdp_profile_tenant_seen_idx_sql := IF(
    @cdp_profile_tenant_seen_idx_exists = 0,
    "ALTER TABLE cdp_user_profile ADD KEY idx_cdp_profile_tenant_seen (tenant_id, last_seen_at, id)",
    "SELECT 1"
);
PREPARE cdp_profile_tenant_seen_idx_stmt FROM @cdp_profile_tenant_seen_idx_sql;
EXECUTE cdp_profile_tenant_seen_idx_stmt;
DEALLOCATE PREPARE cdp_profile_tenant_seen_idx_stmt;

SET @cdp_identity_tenant_unique_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'cdp_user_identity'
      AND index_name = 'uk_cdp_identity_tenant'
);
SET @cdp_identity_tenant_unique_sql := IF(
    @cdp_identity_tenant_unique_exists = 0,
    "ALTER TABLE cdp_user_identity ADD UNIQUE KEY uk_cdp_identity_tenant (tenant_id, identity_type, identity_value)",
    "SELECT 1"
);
PREPARE cdp_identity_tenant_unique_stmt FROM @cdp_identity_tenant_unique_sql;
EXECUTE cdp_identity_tenant_unique_stmt;
DEALLOCATE PREPARE cdp_identity_tenant_unique_stmt;

SET @cdp_identity_tenant_user_idx_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'cdp_user_identity'
      AND index_name = 'idx_cdp_identity_tenant_user'
);
SET @cdp_identity_tenant_user_idx_sql := IF(
    @cdp_identity_tenant_user_idx_exists = 0,
    "ALTER TABLE cdp_user_identity ADD KEY idx_cdp_identity_tenant_user (tenant_id, user_id, identity_type)",
    "SELECT 1"
);
PREPARE cdp_identity_tenant_user_idx_stmt FROM @cdp_identity_tenant_user_idx_sql;
EXECUTE cdp_identity_tenant_user_idx_stmt;
DEALLOCATE PREPARE cdp_identity_tenant_user_idx_stmt;

SET @cdp_tag_tenant_unique_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'cdp_user_tag'
      AND index_name = 'uk_cdp_user_tag_tenant'
);
SET @cdp_tag_tenant_unique_sql := IF(
    @cdp_tag_tenant_unique_exists = 0,
    "ALTER TABLE cdp_user_tag ADD UNIQUE KEY uk_cdp_user_tag_tenant (tenant_id, user_id, tag_code)",
    "SELECT 1"
);
PREPARE cdp_tag_tenant_unique_stmt FROM @cdp_tag_tenant_unique_sql;
EXECUTE cdp_tag_tenant_unique_stmt;
DEALLOCATE PREPARE cdp_tag_tenant_unique_stmt;

SET @cdp_tag_tenant_status_idx_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'cdp_user_tag'
      AND index_name = 'idx_cdp_tag_tenant_user_status'
);
SET @cdp_tag_tenant_status_idx_sql := IF(
    @cdp_tag_tenant_status_idx_exists = 0,
    "ALTER TABLE cdp_user_tag ADD KEY idx_cdp_tag_tenant_user_status (tenant_id, user_id, status, updated_at)",
    "SELECT 1"
);
PREPARE cdp_tag_tenant_status_idx_stmt FROM @cdp_tag_tenant_status_idx_sql;
EXECUTE cdp_tag_tenant_status_idx_stmt;
DEALLOCATE PREPARE cdp_tag_tenant_status_idx_stmt;

SET @cdp_tag_history_tenant_unique_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'cdp_user_tag_history'
      AND index_name = 'uk_cdp_tag_history_tenant_idempotency'
);
SET @cdp_tag_history_tenant_unique_sql := IF(
    @cdp_tag_history_tenant_unique_exists = 0,
    "ALTER TABLE cdp_user_tag_history ADD UNIQUE KEY uk_cdp_tag_history_tenant_idempotency (tenant_id, idempotency_key)",
    "SELECT 1"
);
PREPARE cdp_tag_history_tenant_unique_stmt FROM @cdp_tag_history_tenant_unique_sql;
EXECUTE cdp_tag_history_tenant_unique_stmt;
DEALLOCATE PREPARE cdp_tag_history_tenant_unique_stmt;

SET @cdp_tag_history_tenant_idx_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'cdp_user_tag_history'
      AND index_name = 'idx_cdp_tag_history_tenant_user'
);
SET @cdp_tag_history_tenant_idx_sql := IF(
    @cdp_tag_history_tenant_idx_exists = 0,
    "ALTER TABLE cdp_user_tag_history ADD KEY idx_cdp_tag_history_tenant_user (tenant_id, user_id, tag_code, operated_at)",
    "SELECT 1"
);
PREPARE cdp_tag_history_tenant_idx_stmt FROM @cdp_tag_history_tenant_idx_sql;
EXECUTE cdp_tag_history_tenant_idx_stmt;
DEALLOCATE PREPARE cdp_tag_history_tenant_idx_stmt;

SET @cdp_tag_operation_tenant_idx_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'cdp_tag_operation'
      AND index_name = 'idx_cdp_tag_operation_tenant_status'
);
SET @cdp_tag_operation_tenant_idx_sql := IF(
    @cdp_tag_operation_tenant_idx_exists = 0,
    "ALTER TABLE cdp_tag_operation ADD KEY idx_cdp_tag_operation_tenant_status (tenant_id, status, created_at)",
    "SELECT 1"
);
PREPARE cdp_tag_operation_tenant_idx_stmt FROM @cdp_tag_operation_tenant_idx_sql;
EXECUTE cdp_tag_operation_tenant_idx_stmt;
DEALLOCATE PREPARE cdp_tag_operation_tenant_idx_stmt;
