CREATE TABLE IF NOT EXISTS execution_retention_policy (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    table_name VARCHAR(128) NOT NULL,
    retention_days INT NOT NULL,
    action VARCHAR(32) NOT NULL,
    archive_target VARCHAR(256) NULL,
    legal_hold_supported TINYINT NOT NULL DEFAULT 0,
    enabled TINYINT NOT NULL DEFAULT 1,
    owner VARCHAR(128) NOT NULL,
    notes VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_execution_retention_policy_table (table_name),
    KEY idx_execution_retention_policy_enabled (enabled, action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Execution data retention policy registry';

CREATE TABLE IF NOT EXISTS execution_retention_run (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    policy_id BIGINT NOT NULL,
    table_name VARCHAR(128) NOT NULL,
    action VARCHAR(32) NOT NULL,
    cutoff_at DATETIME NOT NULL,
    status VARCHAR(32) NOT NULL,
    rows_scanned BIGINT NOT NULL DEFAULT 0,
    rows_archived BIGINT NOT NULL DEFAULT 0,
    rows_deleted BIGINT NOT NULL DEFAULT 0,
    started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at DATETIME NULL,
    error_message VARCHAR(1000) NULL,
    KEY idx_execution_retention_run_policy (policy_id, started_at),
    KEY idx_execution_retention_run_status (status, started_at),
    KEY idx_execution_retention_run_table_cutoff (table_name, cutoff_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Execution retention cleanup run ledger';

CREATE TABLE IF NOT EXISTS execution_retention_archive_manifest (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id BIGINT NOT NULL,
    table_name VARCHAR(128) NOT NULL,
    archive_target VARCHAR(256) NOT NULL,
    object_uri VARCHAR(500) NULL,
    checksum VARCHAR(128) NULL,
    row_count BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_execution_retention_archive_run (run_id),
    KEY idx_execution_retention_archive_table (table_name, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Execution retention archive manifest';

INSERT INTO execution_retention_policy
    (table_name, retention_days, action, archive_target, legal_hold_supported, owner, notes)
VALUES
    ('canvas_execution', 180, 'ARCHIVE_THEN_DELETE', 'execution_retention_archive_manifest', 1, 'Runtime platform', 'Keep recent execution ledger online; archive older terminal rows.'),
    ('canvas_execution_trace', 30, 'ARCHIVE_THEN_DELETE', 'execution_retention_archive_manifest', 0, 'Runtime platform', 'Trace rows are high volume and retained online for short incident windows.'),
    ('canvas_execution_dlq', 90, 'DELETE_AFTER_RESOLUTION', NULL, 1, 'Runtime platform', 'Keep failed execution diagnostics through replay and incident review.'),
    ('canvas_execution_request', 90, 'COMPACT_THEN_DELETE', 'execution_retention_archive_manifest', 1, 'Runtime platform', 'Keep request idempotency ledger through retry and audit window.'),
    ('canvas_execution_stats', 730, 'KEEP_AGGREGATE', NULL, 0, 'Data platform', 'Daily aggregates stay online for reporting and trend comparison.'),
    ('event_log', 30, 'ARCHIVE_THEN_DELETE', 'execution_retention_archive_manifest', 1, 'CDP platform', 'Raw event log retention is short because CDP warehouse carries long-term analytics.')
ON DUPLICATE KEY UPDATE
    retention_days = VALUES(retention_days),
    action = VALUES(action),
    archive_target = VALUES(archive_target),
    legal_hold_supported = VALUES(legal_hold_supported),
    owner = VALUES(owner),
    notes = VALUES(notes),
    enabled = 1;
