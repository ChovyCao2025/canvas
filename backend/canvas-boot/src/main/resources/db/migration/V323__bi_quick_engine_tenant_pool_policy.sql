ALTER TABLE bi_quick_engine_capacity_policy
  ADD COLUMN pool_key VARCHAR(64) NOT NULL DEFAULT 'STANDARD' AFTER notification_receivers,
  ADD COLUMN max_concurrent_queries INT NOT NULL DEFAULT 8 AFTER pool_key,
  ADD COLUMN queue_limit INT NOT NULL DEFAULT 50 AFTER max_concurrent_queries,
  ADD COLUMN queue_timeout_seconds INT NOT NULL DEFAULT 120 AFTER queue_limit,
  ADD COLUMN pool_weight INT NOT NULL DEFAULT 100 AFTER queue_timeout_seconds,
  ADD INDEX idx_bi_quick_engine_pool (tenant_id, pool_key);
