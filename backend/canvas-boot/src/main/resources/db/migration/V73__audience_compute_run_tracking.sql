CREATE TABLE IF NOT EXISTS `audience_compute_run` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `audience_id` BIGINT NOT NULL,
    `perf_run_id` VARCHAR(80) NULL COMMENT '压测批次ID',
    `perf_input_id` VARCHAR(160) NULL COMMENT '压测输入ID',
    `status` VARCHAR(20) NOT NULL COMMENT 'COMPUTING/READY/FAILED/SKIPPED_LOCK',
    `estimated_size` BIGINT NULL,
    `bitmap_size_kb` INT NULL,
    `error_msg` VARCHAR(500) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_audience_compute_run_perf` (`perf_run_id`, `audience_id`, `updated_at`),
    KEY `idx_audience_compute_run_input` (`perf_input_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人群计算压测运行账本';
