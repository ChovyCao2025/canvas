CREATE TABLE IF NOT EXISTS canvas_project (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  project_key VARCHAR(80) NOT NULL,
  project_name VARCHAR(160) NOT NULL,
  description VARCHAR(500) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  default_settings_json JSON NULL,
  require_review_before_publish TINYINT NOT NULL DEFAULT 0,
  quiet_hours_json JSON NULL,
  created_by VARCHAR(64) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by VARCHAR(64) NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_canvas_project_tenant_key (tenant_id, project_key),
  KEY idx_canvas_project_tenant_status (tenant_id, status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户内项目治理域';

CREATE TABLE IF NOT EXISTS canvas_project_member (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  user_id BIGINT NULL,
  username VARCHAR(64) NOT NULL,
  role VARCHAR(32) NOT NULL,
  source VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_canvas_project_member_project_user (project_id, username),
  KEY idx_canvas_project_member_tenant_user (tenant_id, username),
  KEY idx_canvas_project_member_project_role (project_id, role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目成员与项目角色';

CREATE TABLE IF NOT EXISTS canvas_project_folder (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NULL,
  canvas_id BIGINT NOT NULL,
  project_id BIGINT NULL,
  project_key VARCHAR(80) NULL,
  project_name VARCHAR(160) NULL,
  folder_key VARCHAR(80) NULL,
  folder_name VARCHAR(160) NULL,
  updated_by VARCHAR(64) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_canvas_project_folder_canvas (canvas_id),
  KEY idx_canvas_project_folder_project (tenant_id, project_id, folder_key),
  KEY idx_canvas_project_folder_key (tenant_id, project_key, folder_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='画布项目和文件夹归属';

SET @cpf_tenant_exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'canvas_project_folder'
    AND column_name = 'tenant_id'
);
SET @cpf_tenant_sql := IF(
  @cpf_tenant_exists = 0,
  "ALTER TABLE canvas_project_folder ADD COLUMN tenant_id BIGINT NULL AFTER id",
  "SELECT 1"
);
PREPARE cpf_tenant_stmt FROM @cpf_tenant_sql;
EXECUTE cpf_tenant_stmt;
DEALLOCATE PREPARE cpf_tenant_stmt;

SET @cpf_project_exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'canvas_project_folder'
    AND column_name = 'project_id'
);
SET @cpf_project_sql := IF(
  @cpf_project_exists = 0,
  "ALTER TABLE canvas_project_folder ADD COLUMN project_id BIGINT NULL AFTER canvas_id",
  "SELECT 1"
);
PREPARE cpf_project_stmt FROM @cpf_project_sql;
EXECUTE cpf_project_stmt;
DEALLOCATE PREPARE cpf_project_stmt;

UPDATE canvas_project_folder cpf
JOIN canvas c ON c.id = cpf.canvas_id
SET cpf.tenant_id = c.tenant_id
WHERE cpf.tenant_id IS NULL;

INSERT INTO canvas_project (tenant_id, project_key, project_name, created_by)
SELECT DISTINCT
  cpf.tenant_id,
  cpf.project_key,
  COALESCE(NULLIF(cpf.project_name, ''), cpf.project_key),
  'migration'
FROM canvas_project_folder cpf
WHERE cpf.tenant_id IS NOT NULL
  AND cpf.project_key IS NOT NULL
  AND cpf.project_key <> ''
ON DUPLICATE KEY UPDATE
  project_name = VALUES(project_name);

UPDATE canvas_project_folder cpf
JOIN canvas_project p ON p.tenant_id = cpf.tenant_id AND p.project_key = cpf.project_key
SET cpf.project_id = p.id
WHERE cpf.project_id IS NULL
  AND cpf.project_key IS NOT NULL
  AND cpf.project_key <> '';
