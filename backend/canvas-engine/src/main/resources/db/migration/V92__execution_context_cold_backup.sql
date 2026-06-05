ALTER TABLE `canvas_execution`
    ADD COLUMN `context_snapshot_json` MEDIUMTEXT NULL COMMENT '挂起执行上下文快照JSON，用于Redis丢失后的冷恢复',
    ADD INDEX `idx_execution_paused_context` (`canvas_id`, `user_id`, `status`, `updated_at`);
