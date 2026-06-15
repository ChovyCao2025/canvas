CREATE TABLE IF NOT EXISTS ab_experiment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(200) NOT NULL,
  experiment_key VARCHAR(120) NOT NULL,
  description VARCHAR(500) NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_by VARCHAR(64) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_ab_experiment_key (experiment_key),
  KEY idx_ab_experiment_enabled_id (enabled, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='A/B 实验定义';
