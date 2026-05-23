-- V44: Persist trigger context needed for DLQ replay.
ALTER TABLE `canvas_execution_dlq`
  ADD COLUMN `trigger_type`      VARCHAR(32)  NULL COMMENT '原始触发类型',
  ADD COLUMN `trigger_node_type` VARCHAR(64)  NULL COMMENT '原始触发节点类型',
  ADD COLUMN `match_key`         VARCHAR(128) NULL COMMENT '原始触发匹配Key';
