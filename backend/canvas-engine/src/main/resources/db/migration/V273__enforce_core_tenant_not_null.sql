INSERT INTO tenant (tenant_key, name, status, plan_code, quota_json, remark, created_by)
VALUES ('default', '默认租户', 'ACTIVE', 'default',
        JSON_OBJECT('maxBatchReplay', 100, 'maxCanvases', 1000),
        '迁移历史单租户数据', 'migration')
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  status = VALUES(status),
  plan_code = VALUES(plan_code);

UPDATE sys_user
SET tenant_id = (SELECT id FROM tenant WHERE tenant_key = 'default')
WHERE tenant_id IS NULL;

UPDATE canvas
SET tenant_id = (SELECT id FROM tenant WHERE tenant_key = 'default')
WHERE tenant_id IS NULL;

UPDATE canvas_version v
LEFT JOIN canvas c ON c.id = v.canvas_id
SET v.tenant_id = COALESCE(c.tenant_id, (SELECT id FROM tenant WHERE tenant_key = 'default'))
WHERE v.tenant_id IS NULL;

UPDATE canvas_execution e
LEFT JOIN canvas c ON c.id = e.canvas_id
SET e.tenant_id = COALESCE(c.tenant_id, (SELECT id FROM tenant WHERE tenant_key = 'default'))
WHERE e.tenant_id IS NULL;

UPDATE canvas_execution_trace t
LEFT JOIN canvas_execution e ON e.id = t.execution_id
SET t.tenant_id = COALESCE(e.tenant_id, (SELECT id FROM tenant WHERE tenant_key = 'default'))
WHERE t.tenant_id IS NULL;

ALTER TABLE sys_user MODIFY COLUMN tenant_id BIGINT NOT NULL;
ALTER TABLE canvas MODIFY COLUMN tenant_id BIGINT NOT NULL;
ALTER TABLE canvas_version MODIFY COLUMN tenant_id BIGINT NOT NULL;
ALTER TABLE canvas_execution MODIFY COLUMN tenant_id BIGINT NOT NULL;
ALTER TABLE canvas_execution_trace MODIFY COLUMN tenant_id BIGINT NOT NULL;
