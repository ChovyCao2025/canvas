SET @bi_embed_token_resource_key_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'bi_embed_token'
    AND column_name = 'resource_key'
);
SET @bi_embed_token_resource_key_sql := IF(
  @bi_embed_token_resource_key_exists = 0,
  "ALTER TABLE bi_embed_token ADD COLUMN resource_key VARCHAR(128) NULL AFTER resource_id",
  "SELECT 1"
);
PREPARE bi_embed_token_resource_key_stmt FROM @bi_embed_token_resource_key_sql;
EXECUTE bi_embed_token_resource_key_stmt;
DEALLOCATE PREPARE bi_embed_token_resource_key_stmt;

SET @bi_embed_token_nonce_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'bi_embed_token'
    AND column_name = 'nonce'
);
SET @bi_embed_token_nonce_sql := IF(
  @bi_embed_token_nonce_exists = 0,
  "ALTER TABLE bi_embed_token ADD COLUMN nonce VARCHAR(64) NULL AFTER scope_json",
  "SELECT 1"
);
PREPARE bi_embed_token_nonce_stmt FROM @bi_embed_token_nonce_sql;
EXECUTE bi_embed_token_nonce_stmt;
DEALLOCATE PREPARE bi_embed_token_nonce_stmt;

SET @bi_embed_token_consumed_at_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'bi_embed_token'
    AND column_name = 'consumed_at'
);
SET @bi_embed_token_consumed_at_sql := IF(
  @bi_embed_token_consumed_at_exists = 0,
  "ALTER TABLE bi_embed_token ADD COLUMN consumed_at DATETIME NULL AFTER revoked",
  "SELECT 1"
);
PREPARE bi_embed_token_consumed_at_stmt FROM @bi_embed_token_consumed_at_sql;
EXECUTE bi_embed_token_consumed_at_stmt;
DEALLOCATE PREPARE bi_embed_token_consumed_at_stmt;

SET @bi_embed_token_consumed_origin_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'bi_embed_token'
    AND column_name = 'consumed_origin'
);
SET @bi_embed_token_consumed_origin_sql := IF(
  @bi_embed_token_consumed_origin_exists = 0,
  "ALTER TABLE bi_embed_token ADD COLUMN consumed_origin VARCHAR(255) NULL AFTER consumed_at",
  "SELECT 1"
);
PREPARE bi_embed_token_consumed_origin_stmt FROM @bi_embed_token_consumed_origin_sql;
EXECUTE bi_embed_token_consumed_origin_stmt;
DEALLOCATE PREPARE bi_embed_token_consumed_origin_stmt;

SET @bi_embed_token_nonce_index_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'bi_embed_token'
    AND index_name = 'idx_bi_embed_token_nonce'
);
SET @bi_embed_token_nonce_index_sql := IF(
  @bi_embed_token_nonce_index_exists = 0,
  "ALTER TABLE bi_embed_token ADD INDEX idx_bi_embed_token_nonce (tenant_id, nonce, revoked, expires_at)",
  "SELECT 1"
);
PREPARE bi_embed_token_nonce_index_stmt FROM @bi_embed_token_nonce_index_sql;
EXECUTE bi_embed_token_nonce_index_stmt;
DEALLOCATE PREPARE bi_embed_token_nonce_index_stmt;

SET @bi_embed_token_resource_key_index_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'bi_embed_token'
    AND index_name = 'idx_bi_embed_token_resource_key'
);
SET @bi_embed_token_resource_key_index_sql := IF(
  @bi_embed_token_resource_key_index_exists = 0,
  "ALTER TABLE bi_embed_token ADD INDEX idx_bi_embed_token_resource_key (tenant_id, resource_type, resource_key)",
  "SELECT 1"
);
PREPARE bi_embed_token_resource_key_index_stmt FROM @bi_embed_token_resource_key_index_sql;
EXECUTE bi_embed_token_resource_key_index_stmt;
DEALLOCATE PREPARE bi_embed_token_resource_key_index_stmt;
