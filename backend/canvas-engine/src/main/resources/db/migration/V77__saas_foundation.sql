CREATE TABLE IF NOT EXISTS tenant (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_key VARCHAR(64) NOT NULL,
  name VARCHAR(128) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  plan_code VARCHAR(64) NOT NULL DEFAULT 'default',
  quota_json JSON NULL,
  remark VARCHAR(500) NULL,
  created_by VARCHAR(64) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by VARCHAR(64) NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_key (tenant_key),
  KEY idx_tenant_status (status)
);

INSERT INTO tenant (tenant_key, name, status, plan_code, quota_json, remark, created_by)
VALUES ('default', '默认租户', 'ACTIVE', 'default',
        JSON_OBJECT('maxBatchReplay', 100, 'maxCanvases', 1000),
        '迁移历史单租户数据', 'migration')
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  status = VALUES(status),
  plan_code = VALUES(plan_code);

ALTER TABLE sys_user
  ADD COLUMN tenant_id BIGINT NULL AFTER id;

UPDATE sys_user
SET tenant_id = (SELECT id FROM tenant WHERE tenant_key = 'default')
WHERE tenant_id IS NULL;

ALTER TABLE sys_user
  MODIFY role VARCHAR(32) NOT NULL COMMENT 'ADMIN / SUPER_ADMIN / TENANT_ADMIN / OPERATOR',
  ADD INDEX idx_sys_user_tenant (tenant_id, enabled);

ALTER TABLE system_option
  ADD COLUMN tenant_id BIGINT NULL AFTER id,
  ADD KEY idx_system_option_tenant_category_enabled_sort (tenant_id, category, enabled, sort_order, id);

ALTER TABLE canvas ADD COLUMN tenant_id BIGINT NULL AFTER id;
UPDATE canvas SET tenant_id = (SELECT id FROM tenant WHERE tenant_key = 'default') WHERE tenant_id IS NULL;
ALTER TABLE canvas ADD KEY idx_canvas_tenant_status (tenant_id, status, updated_at);

ALTER TABLE canvas_version ADD COLUMN tenant_id BIGINT NULL AFTER id;
UPDATE canvas_version v JOIN canvas c ON c.id = v.canvas_id SET v.tenant_id = c.tenant_id WHERE v.tenant_id IS NULL;
UPDATE canvas_version SET tenant_id = (SELECT id FROM tenant WHERE tenant_key = 'default') WHERE tenant_id IS NULL;
ALTER TABLE canvas_version ADD KEY idx_canvas_version_tenant_canvas (tenant_id, canvas_id, version);

ALTER TABLE canvas_execution ADD COLUMN tenant_id BIGINT NULL AFTER id;
UPDATE canvas_execution e JOIN canvas c ON c.id = e.canvas_id SET e.tenant_id = c.tenant_id WHERE e.tenant_id IS NULL;
UPDATE canvas_execution SET tenant_id = (SELECT id FROM tenant WHERE tenant_key = 'default') WHERE tenant_id IS NULL;
ALTER TABLE canvas_execution ADD KEY idx_execution_tenant_canvas_created (tenant_id, canvas_id, created_at);

ALTER TABLE canvas_execution_trace ADD COLUMN tenant_id BIGINT NULL AFTER id;
UPDATE canvas_execution_trace t JOIN canvas_execution e ON e.id = t.execution_id SET t.tenant_id = e.tenant_id WHERE t.tenant_id IS NULL;
UPDATE canvas_execution_trace SET tenant_id = (SELECT id FROM tenant WHERE tenant_key = 'default') WHERE tenant_id IS NULL;
ALTER TABLE canvas_execution_trace ADD KEY idx_trace_tenant_execution (tenant_id, execution_id);
