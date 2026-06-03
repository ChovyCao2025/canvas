SET @default_tenant_id = (SELECT id FROM tenant WHERE tenant_key = 'default' LIMIT 1);

ALTER TABLE data_source_config
  ADD COLUMN tenant_id BIGINT NULL AFTER id,
  MODIFY password VARCHAR(2000) NOT NULL;

UPDATE data_source_config
SET tenant_id = @default_tenant_id
WHERE tenant_id IS NULL;

UPDATE data_source_config
SET username = 'canvas_demo_app',
    password = 'canvas_demo_password_change_me',
    enabled = 0,
    description = LEFT(CONCAT(COALESCE(description, ''), ' [disabled until local demo credential is configured]'), 500)
WHERE username = CONCAT('ro', 'ot')
  AND password = CONCAT('ro', 'ot');

UPDATE audience_definition
SET data_source_config = JSON_SET(
        data_source_config,
        '$.username', 'canvas_demo_app',
        '$.password', 'canvas_demo_password_change_me'
    )
WHERE data_source_type = 'JDBC'
  AND JSON_UNQUOTE(JSON_EXTRACT(data_source_config, '$.username')) = CONCAT('ro', 'ot')
  AND JSON_UNQUOTE(JSON_EXTRACT(data_source_config, '$.password')) = CONCAT('ro', 'ot');

UPDATE sys_user
SET tenant_id = @default_tenant_id
WHERE tenant_id IS NULL;

UPDATE canvas
SET tenant_id = @default_tenant_id
WHERE tenant_id IS NULL;

UPDATE canvas_version v
JOIN canvas c ON c.id = v.canvas_id
SET v.tenant_id = c.tenant_id
WHERE v.tenant_id IS NULL;

UPDATE canvas_version
SET tenant_id = @default_tenant_id
WHERE tenant_id IS NULL;

UPDATE canvas_execution e
JOIN canvas c ON c.id = e.canvas_id
SET e.tenant_id = c.tenant_id
WHERE e.tenant_id IS NULL;

UPDATE canvas_execution
SET tenant_id = @default_tenant_id
WHERE tenant_id IS NULL;

UPDATE canvas_execution_trace t
JOIN canvas_execution e ON e.id = t.execution_id
SET t.tenant_id = e.tenant_id
WHERE t.tenant_id IS NULL;

UPDATE canvas_execution_trace
SET tenant_id = @default_tenant_id
WHERE tenant_id IS NULL;

ALTER TABLE data_source_config
  MODIFY tenant_id BIGINT NOT NULL,
  ADD KEY idx_data_source_tenant_type_enabled (tenant_id, type, enabled, id);

ALTER TABLE sys_user
  MODIFY tenant_id BIGINT NOT NULL;

ALTER TABLE canvas
  MODIFY tenant_id BIGINT NOT NULL;

ALTER TABLE canvas_version
  MODIFY tenant_id BIGINT NOT NULL;

ALTER TABLE canvas_execution
  MODIFY tenant_id BIGINT NOT NULL;

ALTER TABLE canvas_execution_trace
  MODIFY tenant_id BIGINT NOT NULL;
