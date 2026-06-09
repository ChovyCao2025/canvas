ALTER TABLE `marketing_monitor_alert`
  ADD COLUMN `dedupe_key` VARCHAR(256) NULL AFTER `scope_key`,
  ADD UNIQUE KEY `uk_marketing_monitor_alert_open_dedupe` (`tenant_id`, `dedupe_key`);
