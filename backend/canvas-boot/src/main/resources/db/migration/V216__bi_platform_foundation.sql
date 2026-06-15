CREATE TABLE IF NOT EXISTS bi_workspace (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  workspace_key VARCHAR(128) NOT NULL,
  name VARCHAR(128) NOT NULL,
  description VARCHAR(1000) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_by VARCHAR(128) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bi_workspace_key (tenant_id, workspace_key),
  INDEX idx_bi_workspace_tenant_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bi_workspace_member (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  workspace_id BIGINT NOT NULL,
  user_id VARCHAR(128) NOT NULL,
  role_key VARCHAR(64) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bi_workspace_member (tenant_id, workspace_id, user_id),
  INDEX idx_bi_workspace_member_role (tenant_id, workspace_id, role_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bi_data_source_ref (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  workspace_id BIGINT NOT NULL,
  source_key VARCHAR(128) NOT NULL,
  source_type VARCHAR(64) NOT NULL,
  data_source_config_id BIGINT NULL,
  connection_json JSON NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_by VARCHAR(128) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bi_data_source_ref (tenant_id, workspace_id, source_key),
  INDEX idx_bi_data_source_ref_config (tenant_id, data_source_config_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bi_dataset (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  workspace_id BIGINT NOT NULL,
  dataset_key VARCHAR(128) NOT NULL,
  name VARCHAR(128) NOT NULL,
  dataset_type VARCHAR(32) NOT NULL,
  source_ref_id BIGINT NULL,
  table_expression VARCHAR(500) NOT NULL,
  tenant_column VARCHAR(128) NOT NULL DEFAULT 'tenant_id',
  model_json JSON NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  created_by VARCHAR(128) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bi_dataset_key (tenant_id, workspace_id, dataset_key),
  INDEX idx_bi_dataset_workspace_status (tenant_id, workspace_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bi_dataset_field (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  dataset_id BIGINT NOT NULL,
  field_key VARCHAR(128) NOT NULL,
  display_name VARCHAR(128) NOT NULL,
  column_expression VARCHAR(500) NOT NULL,
  role_key VARCHAR(32) NOT NULL,
  data_type VARCHAR(32) NOT NULL,
  semantic_type VARCHAR(64) NULL,
  default_aggregation VARCHAR(32) NULL,
  format_pattern VARCHAR(64) NULL,
  unit VARCHAR(32) NULL,
  visible TINYINT NOT NULL DEFAULT 1,
  sensitive_level VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bi_dataset_field (tenant_id, dataset_id, field_key),
  INDEX idx_bi_dataset_field_role (tenant_id, dataset_id, role_key, visible)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bi_dataset_relation (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  dataset_id BIGINT NOT NULL,
  relation_key VARCHAR(128) NOT NULL,
  left_field_key VARCHAR(128) NOT NULL,
  right_dataset_key VARCHAR(128) NOT NULL,
  right_field_key VARCHAR(128) NOT NULL,
  join_type VARCHAR(20) NOT NULL DEFAULT 'LEFT',
  relation_json JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bi_dataset_relation (tenant_id, dataset_id, relation_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bi_metric (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  workspace_id BIGINT NOT NULL,
  dataset_id BIGINT NOT NULL,
  metric_key VARCHAR(128) NOT NULL,
  display_name VARCHAR(128) NOT NULL,
  expression VARCHAR(1000) NOT NULL,
  aggregation VARCHAR(32) NOT NULL,
  data_type VARCHAR(32) NOT NULL,
  unit VARCHAR(32) NULL,
  format_pattern VARCHAR(64) NULL,
  allowed_dimensions_json JSON NULL,
  owner VARCHAR(128) NULL,
  description VARCHAR(1000) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bi_metric_key (tenant_id, dataset_id, metric_key),
  INDEX idx_bi_metric_workspace_status (tenant_id, workspace_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bi_chart (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  workspace_id BIGINT NOT NULL,
  chart_key VARCHAR(128) NOT NULL,
  name VARCHAR(128) NOT NULL,
  chart_type VARCHAR(64) NOT NULL,
  dataset_id BIGINT NOT NULL,
  query_json JSON NOT NULL,
  style_json JSON NULL,
  interaction_json JSON NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  created_by VARCHAR(128) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bi_chart_key (tenant_id, workspace_id, chart_key),
  INDEX idx_bi_chart_dataset (tenant_id, dataset_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bi_dashboard (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  workspace_id BIGINT NOT NULL,
  dashboard_key VARCHAR(128) NOT NULL,
  name VARCHAR(128) NOT NULL,
  description VARCHAR(1000) NULL,
  theme_json JSON NULL,
  filter_json JSON NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  version INT NOT NULL DEFAULT 1,
  created_by VARCHAR(128) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bi_dashboard_key (tenant_id, workspace_id, dashboard_key),
  INDEX idx_bi_dashboard_status (tenant_id, workspace_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bi_dashboard_widget (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  dashboard_id BIGINT NOT NULL,
  widget_key VARCHAR(128) NOT NULL,
  chart_id BIGINT NULL,
  widget_type VARCHAR(64) NOT NULL,
  title VARCHAR(128) NULL,
  layout_json JSON NOT NULL,
  query_override_json JSON NULL,
  interaction_json JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bi_dashboard_widget (tenant_id, dashboard_id, widget_key),
  INDEX idx_bi_dashboard_widget_chart (tenant_id, chart_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bi_portal (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  workspace_id BIGINT NOT NULL,
  portal_key VARCHAR(128) NOT NULL,
  name VARCHAR(128) NOT NULL,
  theme_json JSON NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  created_by VARCHAR(128) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bi_portal_key (tenant_id, workspace_id, portal_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bi_portal_menu (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  portal_id BIGINT NOT NULL,
  menu_key VARCHAR(128) NOT NULL,
  parent_menu_key VARCHAR(128) NULL,
  title VARCHAR(128) NOT NULL,
  resource_type VARCHAR(64) NOT NULL,
  resource_id BIGINT NULL,
  external_url VARCHAR(1000) NULL,
  visibility_json JSON NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bi_portal_menu (tenant_id, portal_id, menu_key),
  INDEX idx_bi_portal_menu_order (tenant_id, portal_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bi_query_history (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  workspace_id BIGINT NULL,
  dataset_id BIGINT NULL,
  user_id VARCHAR(128) NULL,
  request_json JSON NOT NULL,
  compiled_sql_hash VARCHAR(128) NOT NULL,
  row_count INT NULL,
  duration_ms BIGINT NULL,
  status VARCHAR(20) NOT NULL,
  error_message VARCHAR(1000) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_bi_query_history_dataset (tenant_id, dataset_id, created_at),
  INDEX idx_bi_query_history_user (tenant_id, user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bi_export_job (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  workspace_id BIGINT NOT NULL,
  resource_type VARCHAR(64) NOT NULL,
  resource_id BIGINT NULL,
  export_format VARCHAR(32) NOT NULL,
  request_json JSON NOT NULL,
  row_limit INT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
  file_url VARCHAR(1000) NULL,
  error_message VARCHAR(1000) NULL,
  created_by VARCHAR(128) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_bi_export_job_status (tenant_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bi_subscription (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  workspace_id BIGINT NOT NULL,
  subscription_key VARCHAR(128) NOT NULL,
  name VARCHAR(128) NOT NULL,
  resource_type VARCHAR(64) NOT NULL,
  resource_id BIGINT NOT NULL,
  schedule_json JSON NOT NULL,
  receiver_json JSON NOT NULL,
  delivery_json JSON NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_by VARCHAR(128) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bi_subscription_key (tenant_id, workspace_id, subscription_key),
  INDEX idx_bi_subscription_enabled (tenant_id, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bi_alert_rule (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  workspace_id BIGINT NOT NULL,
  alert_key VARCHAR(128) NOT NULL,
  name VARCHAR(128) NOT NULL,
  dataset_id BIGINT NOT NULL,
  metric_key VARCHAR(128) NOT NULL,
  condition_json JSON NOT NULL,
  receiver_json JSON NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_by VARCHAR(128) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bi_alert_rule_key (tenant_id, workspace_id, alert_key),
  INDEX idx_bi_alert_rule_metric (tenant_id, dataset_id, metric_key, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bi_resource_permission (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  workspace_id BIGINT NOT NULL,
  resource_type VARCHAR(64) NOT NULL,
  resource_id BIGINT NOT NULL,
  subject_type VARCHAR(32) NOT NULL,
  subject_id VARCHAR(128) NOT NULL,
  action_key VARCHAR(64) NOT NULL,
  effect VARCHAR(16) NOT NULL DEFAULT 'ALLOW',
  created_by VARCHAR(128) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bi_resource_permission (tenant_id, resource_type, resource_id, subject_type, subject_id, action_key),
  INDEX idx_bi_resource_permission_subject (tenant_id, subject_type, subject_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bi_row_permission (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  dataset_id BIGINT NOT NULL,
  rule_key VARCHAR(128) NOT NULL,
  subject_type VARCHAR(32) NOT NULL,
  subject_id VARCHAR(128) NOT NULL,
  filter_json JSON NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bi_row_permission (tenant_id, dataset_id, rule_key),
  INDEX idx_bi_row_permission_subject (tenant_id, subject_type, subject_id, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bi_column_permission (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  dataset_id BIGINT NOT NULL,
  field_key VARCHAR(128) NOT NULL,
  subject_type VARCHAR(32) NOT NULL,
  subject_id VARCHAR(128) NOT NULL,
  policy VARCHAR(32) NOT NULL,
  mask_json JSON NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bi_column_permission (tenant_id, dataset_id, field_key, subject_type, subject_id),
  INDEX idx_bi_column_permission_subject (tenant_id, subject_type, subject_id, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bi_embed_token (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  token_hash VARCHAR(128) NOT NULL,
  resource_type VARCHAR(64) NOT NULL,
  resource_id BIGINT NOT NULL,
  user_id VARCHAR(128) NOT NULL,
  scope_json JSON NOT NULL,
  expires_at DATETIME NOT NULL,
  revoked TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bi_embed_token_hash (token_hash),
  INDEX idx_bi_embed_token_resource (tenant_id, resource_type, resource_id),
  INDEX idx_bi_embed_token_expiry (expires_at, revoked)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bi_audit_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  workspace_id BIGINT NULL,
  actor_id VARCHAR(128) NULL,
  action_key VARCHAR(128) NOT NULL,
  resource_type VARCHAR(64) NULL,
  resource_id BIGINT NULL,
  detail_json JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_bi_audit_resource (tenant_id, resource_type, resource_id, created_at),
  INDEX idx_bi_audit_actor (tenant_id, actor_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO bi_workspace (tenant_id, workspace_key, name, description, status, created_by)
SELECT 0, 'marketing_canvas', 'Marketing Canvas 分析空间', '面向营销画布的预置 BI 工作空间。', 'ACTIVE', 'system'
WHERE NOT EXISTS (
  SELECT 1 FROM bi_workspace WHERE tenant_id = 0 AND workspace_key = 'marketing_canvas'
);

INSERT INTO bi_dataset (tenant_id, workspace_id, dataset_key, name, dataset_type, table_expression, tenant_column, model_json, status, created_by)
SELECT 0, w.id, 'canvas_daily_stats', '画布每日统计', 'TABLE', 'canvas_dws.canvas_daily_stats', 'tenant_id',
       JSON_OBJECT('source', 'DORIS', 'category', 'CANVAS', 'preset', true),
       'PUBLISHED', 'system'
FROM bi_workspace w
WHERE w.tenant_id = 0
  AND w.workspace_key = 'marketing_canvas'
  AND NOT EXISTS (
    SELECT 1 FROM bi_dataset d
    WHERE d.tenant_id = 0
      AND d.workspace_id = w.id
      AND d.dataset_key = 'canvas_daily_stats'
  );

INSERT INTO bi_dataset_field (tenant_id, dataset_id, field_key, display_name, column_expression, role_key, data_type, semantic_type, default_aggregation, format_pattern, unit, visible, sensitive_level, sort_order)
SELECT 0, d.id, seed.field_key, seed.display_name, seed.column_expression, seed.role_key, seed.data_type, seed.semantic_type,
       seed.default_aggregation, seed.format_pattern, seed.unit, 1, 'NORMAL', seed.sort_order
FROM bi_dataset d
JOIN (
  SELECT 'stat_date' AS field_key, '日期' AS display_name, 'stat_date' AS column_expression, 'DIMENSION' AS role_key, 'DATE' AS data_type, 'DATE' AS semantic_type, NULL AS default_aggregation, 'yyyy-MM-dd' AS format_pattern, NULL AS unit, 10 AS sort_order
  UNION ALL SELECT 'canvas_id', '画布ID', 'canvas_id', 'DIMENSION', 'NUMBER', 'ID', NULL, NULL, NULL, 20
  UNION ALL SELECT 'canvas_name', '画布名称', 'canvas_name', 'DIMENSION', 'STRING', 'NAME', NULL, NULL, NULL, 30
  UNION ALL SELECT 'trigger_type', '触发类型', 'trigger_type', 'DIMENSION', 'STRING', 'CATEGORY', NULL, NULL, NULL, 40
  UNION ALL SELECT 'total_executions', '执行次数', 'total_executions', 'MEASURE', 'NUMBER', 'COUNT', 'SUM', '#,##0', '次', 100
  UNION ALL SELECT 'success_count', '成功次数', 'success_count', 'MEASURE', 'NUMBER', 'COUNT', 'SUM', '#,##0', '次', 110
  UNION ALL SELECT 'fail_count', '失败次数', 'fail_count', 'MEASURE', 'NUMBER', 'COUNT', 'SUM', '#,##0', '次', 120
  UNION ALL SELECT 'running_count', '运行中次数', 'running_count', 'MEASURE', 'NUMBER', 'COUNT', 'SUM', '#,##0', '次', 130
  UNION ALL SELECT 'unique_users', '去重用户数', 'unique_users', 'MEASURE', 'NUMBER', 'COUNT', 'SUM', '#,##0', '人', 140
  UNION ALL SELECT 'avg_duration_ms', '平均耗时', 'avg_duration_ms', 'MEASURE', 'NUMBER', 'DURATION', 'AVG', '#,##0', 'ms', 150
) seed
WHERE d.tenant_id = 0
  AND d.dataset_key = 'canvas_daily_stats'
  AND NOT EXISTS (
    SELECT 1 FROM bi_dataset_field f
    WHERE f.tenant_id = 0
      AND f.dataset_id = d.id
      AND f.field_key = seed.field_key
  );

INSERT INTO bi_metric (tenant_id, workspace_id, dataset_id, metric_key, display_name, expression, aggregation, data_type, unit, format_pattern, allowed_dimensions_json, owner, description, status)
SELECT 0, d.workspace_id, d.id, seed.metric_key, seed.display_name, seed.expression, seed.aggregation, seed.data_type,
       seed.unit, seed.format_pattern, seed.allowed_dimensions_json, 'system', seed.description, 'ACTIVE'
FROM bi_dataset d
JOIN (
  SELECT 'total_executions' AS metric_key, '执行次数' AS display_name, 'SUM(total_executions)' AS expression, 'SUM' AS aggregation, 'NUMBER' AS data_type, '次' AS unit, '#,##0' AS format_pattern, JSON_ARRAY('stat_date', 'canvas_id', 'canvas_name', 'trigger_type') AS allowed_dimensions_json, '画布在统计周期内的触发执行总次数。' AS description
  UNION ALL SELECT 'success_count', '成功次数', 'SUM(success_count)', 'SUM', 'NUMBER', '次', '#,##0', JSON_ARRAY('stat_date', 'canvas_id', 'canvas_name', 'trigger_type'), '画布在统计周期内成功完成的执行次数。'
  UNION ALL SELECT 'fail_count', '失败次数', 'SUM(fail_count)', 'SUM', 'NUMBER', '次', '#,##0', JSON_ARRAY('stat_date', 'canvas_id', 'canvas_name', 'trigger_type'), '画布在统计周期内失败的执行次数。'
  UNION ALL SELECT 'unique_users', '去重用户数', 'SUM(unique_users)', 'SUM', 'NUMBER', '人', '#,##0', JSON_ARRAY('stat_date', 'canvas_id', 'canvas_name', 'trigger_type'), '画布在统计周期内触达或执行涉及的去重用户数。'
  UNION ALL SELECT 'avg_duration_ms', '平均耗时', 'CASE WHEN SUM(total_executions) > 0 THEN SUM(total_duration_ms) / SUM(total_executions) ELSE 0 END', 'AVG', 'NUMBER', 'ms', '#,##0', JSON_ARRAY('stat_date', 'canvas_id', 'canvas_name', 'trigger_type'), '按执行次数加权计算的画布平均执行耗时。'
  UNION ALL SELECT 'success_rate', '执行成功率', 'CASE WHEN SUM(total_executions) > 0 THEN SUM(success_count) / SUM(total_executions) ELSE 0 END', 'RATIO', 'PERCENT', '%', '0.00%', JSON_ARRAY('stat_date', 'canvas_id', 'canvas_name', 'trigger_type'), '成功次数除以执行次数。'
) seed
WHERE d.tenant_id = 0
  AND d.dataset_key = 'canvas_daily_stats'
  AND NOT EXISTS (
    SELECT 1 FROM bi_metric m
    WHERE m.tenant_id = 0
      AND m.dataset_id = d.id
      AND m.metric_key = seed.metric_key
  );
