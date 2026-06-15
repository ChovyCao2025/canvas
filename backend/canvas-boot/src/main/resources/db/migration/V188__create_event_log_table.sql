CREATE TABLE IF NOT EXISTS event_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  event_code VARCHAR(128) NOT NULL,
  user_id VARCHAR(64) NOT NULL,
  perf_run_id VARCHAR(80) NULL COMMENT '压测批次ID',
  attributes TEXT NULL,
  canvas_triggered INT NOT NULL DEFAULT 0,
  canvas_count INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_event_log_event_user_time (event_code, user_id, created_at),
  KEY idx_event_log_created (created_at),
  KEY idx_event_log_perf_run (perf_run_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件上报日志';
