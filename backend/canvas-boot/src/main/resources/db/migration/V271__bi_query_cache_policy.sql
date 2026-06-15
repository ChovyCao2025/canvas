CREATE TABLE IF NOT EXISTS bi_query_cache_policy (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  resource_type VARCHAR(32) NOT NULL,
  resource_key VARCHAR(128) NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  ttl_seconds BIGINT NOT NULL DEFAULT 300,
  cache_mode VARCHAR(32) NOT NULL DEFAULT 'CACHE',
  updated_by VARCHAR(128),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bi_query_cache_policy_resource (tenant_id, resource_type, resource_key),
  KEY idx_bi_query_cache_policy_tenant (tenant_id, updated_at)
);
