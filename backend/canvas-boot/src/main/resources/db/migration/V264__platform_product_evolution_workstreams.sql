CREATE TABLE IF NOT EXISTS platform_workstream (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  workstream_key VARCHAR(128) NOT NULL,
  display_name VARCHAR(128) NOT NULL,
  priority VARCHAR(16) NOT NULL,
  requires_child_spec TINYINT NOT NULL DEFAULT 1,
  child_spec_path VARCHAR(255) NULL,
  summary VARCHAR(1000) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'DISCOVERY',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_platform_workstream_key (workstream_key),
  INDEX idx_platform_workstream_status (priority, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO platform_workstream(workstream_key, display_name, priority, requires_child_spec, child_spec_path, summary, status)
VALUES
  ('platformization', 'Platformization', 'P2', 1, NULL, 'Extension points, developer portal basics, API keys, outbound webhooks, and schema improvements.', 'DISCOVERY'),
  ('data-assets', 'Data Assets', 'P2', 1, 'docs/product-evolution/specs/p2-016-analytics-event-trace-schema-and-sink.md', 'Data quality, data catalog, path analytics, reports, and event pipeline foundations.', 'READY'),
  ('channels', 'Channels', 'P2', 1, 'docs/product-evolution/specs/p2-012-channel-intelligence-and-scheduling.md', 'WeCom L1/L2, adapter abstraction, and channel cost/receipt tracking.', 'READY'),
  ('operations', 'Operations', 'P2', 1, NULL, 'Approval expansion, audit timeline, command dashboard, and alert rules.', 'DISCOVERY'),
  ('knowledge', 'Knowledge', 'P2', 1, 'docs/product-evolution/specs/p2-013-knowledge-base-best-practice-library.md', 'Template market, best-practice library, contextual help, and playbooks.', 'READY'),
  ('integrations', 'Integrations', 'P2', 1, 'docs/product-evolution/specs/p2-008-integration-readiness.md', 'Inbound webhook, API key management, SSO/OIDC decision, and data source improvements.', 'READY')
ON DUPLICATE KEY UPDATE
  display_name = VALUES(display_name),
  priority = VALUES(priority),
  requires_child_spec = VALUES(requires_child_spec),
  child_spec_path = VALUES(child_spec_path),
  summary = VALUES(summary),
  status = VALUES(status);
