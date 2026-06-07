CREATE TABLE IF NOT EXISTS marketing_monitor_source (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  source_key VARCHAR(128) NOT NULL,
  source_type VARCHAR(64) NOT NULL,
  display_name VARCHAR(256) NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  metadata_json JSON NULL,
  created_by VARCHAR(128) NULL,
  created_at DATETIME NULL,
  updated_at DATETIME NULL,
  UNIQUE KEY uk_marketing_monitor_source (tenant_id, source_key),
  KEY idx_marketing_monitor_source_type (tenant_id, source_type, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Marketing monitoring source registry';

CREATE TABLE IF NOT EXISTS marketing_monitor_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  source_id BIGINT NOT NULL,
  external_item_id VARCHAR(256) NOT NULL,
  source_type VARCHAR(64) NOT NULL,
  source_url VARCHAR(1000) NULL,
  author_key VARCHAR(256) NULL,
  brand_key VARCHAR(128) NULL,
  text_content TEXT NOT NULL,
  language VARCHAR(32) NULL,
  published_at DATETIME NULL,
  ingested_at DATETIME NOT NULL,
  raw_payload_json JSON NULL,
  created_at DATETIME NULL,
  updated_at DATETIME NULL,
  UNIQUE KEY uk_marketing_monitor_item_external (tenant_id, source_id, external_item_id),
  KEY idx_marketing_monitor_item_time (tenant_id, published_at, ingested_at),
  KEY idx_marketing_monitor_item_brand (tenant_id, brand_key, ingested_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Normalized monitored marketing mentions';

CREATE TABLE IF NOT EXISTS marketing_sentiment_analysis (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  item_id BIGINT NOT NULL,
  sentiment_label VARCHAR(32) NOT NULL,
  sentiment_score DECIMAL(8,5) NOT NULL DEFAULT 0.00000,
  confidence DECIMAL(6,5) NOT NULL DEFAULT 0.50000,
  model_key VARCHAR(80) NOT NULL,
  model_version VARCHAR(80) NOT NULL,
  keyword_hits_json JSON NULL,
  created_at DATETIME NULL,
  UNIQUE KEY uk_marketing_sentiment_item (tenant_id, item_id),
  KEY idx_marketing_sentiment_label (tenant_id, sentiment_label, sentiment_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Deterministic sentiment analysis for monitored mentions';

CREATE TABLE IF NOT EXISTS marketing_competitor_mention (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  item_id BIGINT NOT NULL,
  competitor_key VARCHAR(128) NOT NULL,
  competitor_name VARCHAR(256) NOT NULL,
  matched_terms_json JSON NULL,
  sentiment_label VARCHAR(32) NOT NULL,
  sentiment_score DECIMAL(8,5) NOT NULL DEFAULT 0.00000,
  created_at DATETIME NULL,
  KEY idx_marketing_competitor_item (tenant_id, item_id),
  KEY idx_marketing_competitor_key (tenant_id, competitor_key, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Competitor mentions extracted from monitored items';

CREATE TABLE IF NOT EXISTS marketing_monitor_alert (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  alert_type VARCHAR(64) NOT NULL,
  severity VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  scope_key VARCHAR(128) NULL,
  title VARCHAR(256) NOT NULL,
  reason VARCHAR(1000) NULL,
  item_count INT NOT NULL DEFAULT 0,
  window_start DATETIME NULL,
  window_end DATETIME NULL,
  metadata_json JSON NULL,
  created_by VARCHAR(128) NULL,
  resolved_by VARCHAR(128) NULL,
  resolved_at DATETIME NULL,
  created_at DATETIME NULL,
  updated_at DATETIME NULL,
  KEY idx_marketing_monitor_alert_status (tenant_id, status, severity, created_at),
  KEY idx_marketing_monitor_alert_scope (tenant_id, alert_type, scope_key, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Sentiment and competitor monitoring alert workflow';
