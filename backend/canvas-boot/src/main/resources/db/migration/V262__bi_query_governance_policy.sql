CREATE TABLE IF NOT EXISTS bi_query_governance_policy (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  dataset_key VARCHAR(128) NOT NULL,
  timeout_ms BIGINT NOT NULL,
  quota_rows INT NOT NULL,
  updated_by VARCHAR(128),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bi_query_governance_policy_dataset (tenant_id, dataset_key),
  KEY idx_bi_query_governance_policy_tenant (tenant_id, updated_at)
);
