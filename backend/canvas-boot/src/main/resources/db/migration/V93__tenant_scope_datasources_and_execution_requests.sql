SET @data_source_tenant_column_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'data_source_config'
    AND COLUMN_NAME = 'tenant_id'
);

SET @data_source_tenant_column_ddl := IF(
  @data_source_tenant_column_exists = 0,
  'ALTER TABLE data_source_config ADD COLUMN tenant_id BIGINT NULL AFTER id',
  'DO 0'
);

PREPARE data_source_tenant_column_stmt FROM @data_source_tenant_column_ddl;
EXECUTE data_source_tenant_column_stmt;
DEALLOCATE PREPARE data_source_tenant_column_stmt;

UPDATE data_source_config
SET tenant_id = (SELECT id FROM tenant WHERE tenant_key = 'default')
WHERE tenant_id IS NULL;

SET @data_source_tenant_index_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'data_source_config'
    AND INDEX_NAME = 'idx_data_source_tenant_type_enabled'
);

SET @data_source_tenant_not_null_ddl := IF(
  @data_source_tenant_index_exists = 0,
  'ALTER TABLE data_source_config MODIFY COLUMN tenant_id BIGINT NOT NULL, ADD KEY idx_data_source_tenant_type_enabled (tenant_id, type, enabled)',
  'ALTER TABLE data_source_config MODIFY COLUMN tenant_id BIGINT NOT NULL'
);

PREPARE data_source_tenant_not_null_stmt FROM @data_source_tenant_not_null_ddl;
EXECUTE data_source_tenant_not_null_stmt;
DEALLOCATE PREPARE data_source_tenant_not_null_stmt;

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
