ALTER TABLE `canvas`
  ADD COLUMN `attribution_model` VARCHAR(32) NOT NULL DEFAULT 'LAST_TOUCH' AFTER `attribution_window_days`;

ALTER TABLE `canvas_conversion_attribution`
  ADD COLUMN `attribution_weight` DECIMAL(12,8) NOT NULL DEFAULT 1.00000000 AFTER `attribution_model`,
  ADD COLUMN `touch_created_at` DATETIME NULL AFTER `attribution_weight`;

UPDATE `canvas_conversion_attribution`
SET `send_record_id` = 0
WHERE `send_record_id` IS NULL;

ALTER TABLE `canvas_conversion_attribution`
  MODIFY COLUMN `send_record_id` BIGINT NOT NULL DEFAULT 0,
  DROP INDEX `uk_canvas_attr_event`,
  ADD UNIQUE KEY `uk_canvas_attr_event_model_touch` (`canvas_id`, `event_log_id`, `attribution_model`, `send_record_id`),
  ADD KEY `idx_canvas_attr_model_time` (`canvas_id`, `attribution_model`, `attributed_at`);
