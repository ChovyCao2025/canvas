
-- Add tenant scoping to manual approvals without failing environments that already applied the column.
SET @manual_approval_tenant_exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'canvas_manual_approval'
    AND column_name = 'tenant_id'
);
SET @manual_approval_tenant_sql := IF(
  @manual_approval_tenant_exists = 0,
  "ALTER TABLE canvas_manual_approval ADD COLUMN tenant_id BIGINT NULL AFTER execution_id",
  "SELECT 1"
);
PREPARE manual_approval_tenant_stmt FROM @manual_approval_tenant_sql;
EXECUTE manual_approval_tenant_stmt;
DEALLOCATE PREPARE manual_approval_tenant_stmt;

-- Backfill approval tenant ownership from the execution record before the tenant-aware index is created.
UPDATE canvas_manual_approval a
JOIN canvas_execution e ON e.id = a.execution_id
SET a.tenant_id = e.tenant_id
WHERE a.tenant_id IS NULL;

-- Keep timeout polling efficient for tenant-scoped approval queues.
SET @manual_approval_tenant_idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'canvas_manual_approval'
    AND index_name = 'idx_canvas_manual_approval_tenant_status'
);
SET @manual_approval_tenant_idx_sql := IF(
  @manual_approval_tenant_idx_exists = 0,
  "ALTER TABLE canvas_manual_approval ADD KEY idx_canvas_manual_approval_tenant_status (tenant_id, status, timeout_at)",
  "SELECT 1"
);
PREPARE manual_approval_tenant_idx_stmt FROM @manual_approval_tenant_idx_sql;
EXECUTE manual_approval_tenant_idx_stmt;
DEALLOCATE PREPARE manual_approval_tenant_idx_stmt;
