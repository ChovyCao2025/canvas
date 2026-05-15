-- V14: 为 canvas_execution_trace 补充 duration_ms 列（节点实际耗时）
ALTER TABLE `canvas_execution_trace`
    ADD COLUMN `duration_ms` BIGINT NULL COMMENT '节点执行耗时（毫秒）' AFTER `output_data`;
