CREATE TABLE IF NOT EXISTS approval_definition (
  id BIGINT NOT NULL AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  definition_key VARCHAR(96) NOT NULL,
  name VARCHAR(128) NOT NULL,
  domain VARCHAR(64) NOT NULL,
  target_type VARCHAR(64) NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  mode VARCHAR(32) NOT NULL DEFAULT 'ANY_ONE',
  min_approvals INT NOT NULL DEFAULT 1,
  default_due_hours INT NOT NULL DEFAULT 24,
  external_provider VARCHAR(32) NOT NULL DEFAULT 'LOCAL',
  external_definition_code VARCHAR(128) NULL,
  risk_rule_json TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_approval_definition_key (tenant_id, definition_key),
  KEY idx_approval_definition_target (tenant_id, domain, target_type, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Reusable approval workflow definitions';

CREATE TABLE IF NOT EXISTS approval_instance (
  id BIGINT NOT NULL AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  definition_key VARCHAR(96) NOT NULL,
  domain VARCHAR(64) NOT NULL,
  target_type VARCHAR(64) NOT NULL,
  target_id VARCHAR(128) NOT NULL,
  target_version_id BIGINT NULL,
  status VARCHAR(32) NOT NULL,
  submitter VARCHAR(128) NOT NULL,
  submit_reason VARCHAR(512) NULL,
  risk_level VARCHAR(32) NULL,
  risk_reasons_json TEXT NULL,
  snapshot_json LONGTEXT NULL,
  external_instance_id VARCHAR(128) NULL,
  requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at DATETIME NULL,
  completed_by VARCHAR(128) NULL,
  result_comment VARCHAR(512) NULL,
  auto_action VARCHAR(64) NULL,
  auto_action_status VARCHAR(32) NULL,
  auto_action_error VARCHAR(1024) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_approval_instance_target (tenant_id, target_type, target_id, target_version_id, status, requested_at),
  KEY idx_approval_instance_definition (tenant_id, definition_key, status, requested_at),
  KEY idx_approval_instance_status (tenant_id, status, requested_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Submitted approval workflow instances';

CREATE TABLE IF NOT EXISTS approval_task (
  id BIGINT NOT NULL AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  instance_id BIGINT NOT NULL,
  step_no INT NOT NULL DEFAULT 1,
  approver VARCHAR(128) NOT NULL,
  status VARCHAR(32) NOT NULL,
  due_at DATETIME NULL,
  acted_at DATETIME NULL,
  action_comment VARCHAR(512) NULL,
  delegated_from VARCHAR(128) NULL,
  external_task_id VARCHAR(128) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_approval_task_inbox (tenant_id, approver, status, due_at),
  KEY idx_approval_task_instance (tenant_id, instance_id, status, step_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Approval tasks assigned to reviewers';

CREATE TABLE IF NOT EXISTS approval_audit_event (
  id BIGINT NOT NULL AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  instance_id BIGINT NOT NULL,
  task_id BIGINT NULL,
  event_type VARCHAR(64) NOT NULL,
  actor VARCHAR(128) NOT NULL,
  actor_role VARCHAR(64) NULL,
  old_status VARCHAR(32) NULL,
  new_status VARCHAR(32) NULL,
  detail_json TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_approval_audit_instance (tenant_id, instance_id, created_at),
  KEY idx_approval_audit_actor (tenant_id, actor, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Immutable approval lifecycle audit events';

INSERT INTO approval_definition (
  tenant_id, definition_key, name, domain, target_type, enabled, mode, min_approvals, default_due_hours, external_provider
) VALUES
  (0, 'CANVAS_PUBLISH_DEFAULT', 'Canvas publish approval', 'CANVAS', 'CANVAS', 1, 'ANY_ONE', 1, 24, 'LOCAL'),
  (0, 'BI_PUBLISH_DEFAULT', 'BI publish approval', 'BI', 'BI_RESOURCE', 1, 'ANY_ONE', 1, 24, 'LOCAL'),
  (0, 'RUNTIME_MANUAL_DEFAULT', 'Runtime manual approval', 'RUNTIME', 'EXECUTION_NODE', 1, 'ANY_ONE', 1, 24, 'LOCAL')
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  domain = VALUES(domain),
  target_type = VALUES(target_type),
  enabled = VALUES(enabled),
  mode = VALUES(mode),
  min_approvals = VALUES(min_approvals),
  default_due_hours = VALUES(default_due_hours),
  external_provider = VALUES(external_provider);
