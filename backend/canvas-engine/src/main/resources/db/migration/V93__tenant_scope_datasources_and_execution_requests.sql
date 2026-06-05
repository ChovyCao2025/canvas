ALTER TABLE data_source_config
  ADD COLUMN tenant_id BIGINT NULL AFTER id;

UPDATE data_source_config
SET tenant_id = (SELECT id FROM tenant WHERE tenant_key = 'default')
WHERE tenant_id IS NULL;

ALTER TABLE data_source_config
  MODIFY COLUMN tenant_id BIGINT NOT NULL,
  ADD KEY idx_data_source_tenant_type_enabled (tenant_id, type, enabled);

ALTER TABLE canvas_execution_request
  ADD COLUMN tenant_id BIGINT NULL AFTER id;

UPDATE canvas_execution_request r
LEFT JOIN canvas c ON c.id = r.canvas_id
SET r.tenant_id = COALESCE(c.tenant_id, (SELECT id FROM tenant WHERE tenant_key = 'default'))
WHERE r.tenant_id IS NULL;

ALTER TABLE canvas_execution_request
  MODIFY COLUMN tenant_id BIGINT NOT NULL,
  ADD KEY idx_execution_request_tenant_status_updated (tenant_id, status, updated_at),
  ADD KEY idx_execution_request_tenant_canvas_status_updated (tenant_id, canvas_id, status, updated_at);
