SET @bi_embed_token_access_count_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'bi_embed_token'
    AND column_name = 'access_count'
);
SET @bi_embed_token_access_count_sql := IF(
  @bi_embed_token_access_count_exists = 0,
  "ALTER TABLE bi_embed_token ADD COLUMN access_count INT NOT NULL DEFAULT 0 AFTER consumed_origin",
  "SELECT 1"
);
PREPARE bi_embed_token_access_count_stmt FROM @bi_embed_token_access_count_sql;
EXECUTE bi_embed_token_access_count_stmt;
DEALLOCATE PREPARE bi_embed_token_access_count_stmt;

SET @bi_embed_token_max_access_count_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'bi_embed_token'
    AND column_name = 'max_access_count'
);
SET @bi_embed_token_max_access_count_sql := IF(
  @bi_embed_token_max_access_count_exists = 0,
  "ALTER TABLE bi_embed_token ADD COLUMN max_access_count INT NOT NULL DEFAULT 1 AFTER access_count",
  "SELECT 1"
);
PREPARE bi_embed_token_max_access_count_stmt FROM @bi_embed_token_max_access_count_sql;
EXECUTE bi_embed_token_max_access_count_stmt;
DEALLOCATE PREPARE bi_embed_token_max_access_count_stmt;

SET @bi_embed_token_rate_limit_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'bi_embed_token'
    AND column_name = 'rate_limit_per_minute'
);
SET @bi_embed_token_rate_limit_sql := IF(
  @bi_embed_token_rate_limit_exists = 0,
  "ALTER TABLE bi_embed_token ADD COLUMN rate_limit_per_minute INT NOT NULL DEFAULT 60 AFTER max_access_count",
  "SELECT 1"
);
PREPARE bi_embed_token_rate_limit_stmt FROM @bi_embed_token_rate_limit_sql;
EXECUTE bi_embed_token_rate_limit_stmt;
DEALLOCATE PREPARE bi_embed_token_rate_limit_stmt;

SET @bi_embed_token_rate_window_started_at_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'bi_embed_token'
    AND column_name = 'rate_window_started_at'
);
SET @bi_embed_token_rate_window_started_at_sql := IF(
  @bi_embed_token_rate_window_started_at_exists = 0,
  "ALTER TABLE bi_embed_token ADD COLUMN rate_window_started_at DATETIME NULL AFTER rate_limit_per_minute",
  "SELECT 1"
);
PREPARE bi_embed_token_rate_window_started_at_stmt FROM @bi_embed_token_rate_window_started_at_sql;
EXECUTE bi_embed_token_rate_window_started_at_stmt;
DEALLOCATE PREPARE bi_embed_token_rate_window_started_at_stmt;

SET @bi_embed_token_rate_window_count_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'bi_embed_token'
    AND column_name = 'rate_window_count'
);
SET @bi_embed_token_rate_window_count_sql := IF(
  @bi_embed_token_rate_window_count_exists = 0,
  "ALTER TABLE bi_embed_token ADD COLUMN rate_window_count INT NOT NULL DEFAULT 0 AFTER rate_window_started_at",
  "SELECT 1"
);
PREPARE bi_embed_token_rate_window_count_stmt FROM @bi_embed_token_rate_window_count_sql;
EXECUTE bi_embed_token_rate_window_count_stmt;
DEALLOCATE PREPARE bi_embed_token_rate_window_count_stmt;

SET @bi_embed_token_last_accessed_at_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'bi_embed_token'
    AND column_name = 'last_accessed_at'
);
SET @bi_embed_token_last_accessed_at_sql := IF(
  @bi_embed_token_last_accessed_at_exists = 0,
  "ALTER TABLE bi_embed_token ADD COLUMN last_accessed_at DATETIME NULL AFTER rate_window_count",
  "SELECT 1"
);
PREPARE bi_embed_token_last_accessed_at_stmt FROM @bi_embed_token_last_accessed_at_sql;
EXECUTE bi_embed_token_last_accessed_at_stmt;
DEALLOCATE PREPARE bi_embed_token_last_accessed_at_stmt;

SET @bi_embed_token_last_access_origin_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'bi_embed_token'
    AND column_name = 'last_access_origin'
);
SET @bi_embed_token_last_access_origin_sql := IF(
  @bi_embed_token_last_access_origin_exists = 0,
  "ALTER TABLE bi_embed_token ADD COLUMN last_access_origin VARCHAR(255) NULL AFTER last_accessed_at",
  "SELECT 1"
);
PREPARE bi_embed_token_last_access_origin_stmt FROM @bi_embed_token_last_access_origin_sql;
EXECUTE bi_embed_token_last_access_origin_stmt;
DEALLOCATE PREPARE bi_embed_token_last_access_origin_stmt;

SET @bi_embed_token_access_limit_index_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'bi_embed_token'
    AND index_name = 'idx_bi_embed_token_access_limit'
);
SET @bi_embed_token_access_limit_index_sql := IF(
  @bi_embed_token_access_limit_index_exists = 0,
  "ALTER TABLE bi_embed_token ADD INDEX idx_bi_embed_token_access_limit (tenant_id, token_hash, revoked, expires_at)",
  "SELECT 1"
);
PREPARE bi_embed_token_access_limit_index_stmt FROM @bi_embed_token_access_limit_index_sql;
EXECUTE bi_embed_token_access_limit_index_stmt;
DEALLOCATE PREPARE bi_embed_token_access_limit_index_stmt;
