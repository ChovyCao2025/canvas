-- V8: 给节点执行轨迹表增加 duration_ms 列（设计文档 12.10节）
-- 用于展示每个节点的实际耗时，支持性能分析

ALTER TABLE `canvas_execution_trace`
    ADD COLUMN `duration_ms` BIGINT NULL COMMENT '节点执行耗时（毫秒）' AFTER `finished_at`;

-- 按 duration_ms 的分析查询走此索引
CREATE INDEX idx_trace_duration ON `canvas_execution_trace` (`execution_id`, `duration_ms`);
