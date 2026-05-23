ALTER TABLE `api_definition`
  ADD COLUMN `receipt_enabled` TINYINT NOT NULL DEFAULT 0 COMMENT '是否开启回执等待' AFTER `include_context_payload`,
  ADD COLUMN `receipt_expire_minutes` INT NOT NULL DEFAULT 1440 COMMENT '回执等待过期时间（分钟）' AFTER `receipt_enabled`,
  ADD COLUMN `receipt_statuses` JSON NULL COMMENT '视为回执完成的状态列表' AFTER `receipt_expire_minutes`;
