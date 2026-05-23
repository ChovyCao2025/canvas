ALTER TABLE `event_log`
    ADD COLUMN `perf_run_id` VARCHAR(80) NULL COMMENT '压测批次ID',
    ADD INDEX `idx_event_log_perf_run` (`perf_run_id`, `created_at`);

ALTER TABLE `canvas_execution`
    ADD COLUMN `perf_run_id` VARCHAR(80) NULL COMMENT '压测批次ID',
    ADD INDEX `idx_execution_perf_run` (`perf_run_id`, `created_at`);

ALTER TABLE `canvas_execution_request`
    ADD COLUMN `perf_run_id` VARCHAR(80) NULL COMMENT '压测批次ID',
    ADD INDEX `idx_execution_request_perf_run` (`perf_run_id`, `status`, `updated_at`);

ALTER TABLE `canvas_execution_dlq`
    ADD COLUMN `perf_run_id` VARCHAR(80) NULL COMMENT '压测批次ID',
    ADD INDEX `idx_execution_dlq_perf_run` (`perf_run_id`, `failed_at`);
