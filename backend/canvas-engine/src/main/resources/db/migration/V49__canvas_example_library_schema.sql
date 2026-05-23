-- V49: official canvas example library metadata.

ALTER TABLE canvas_template
  ADD COLUMN template_key VARCHAR(100) NULL COMMENT '官方模板稳定唯一键',
  ADD COLUMN company_type VARCHAR(50) NULL COMMENT '公司类型',
  ADD COLUMN marketing_scenario VARCHAR(50) NULL COMMENT '营销场景',
  ADD COLUMN difficulty VARCHAR(20) NULL COMMENT '入门/进阶/复杂',
  ADD COLUMN covered_node_types VARCHAR(1000) NULL COMMENT '覆盖的节点类型，逗号分隔',
  ADD COLUMN sort_order INT NOT NULL DEFAULT 0 COMMENT '官方模板排序',
  ADD COLUMN enabled TINYINT NOT NULL DEFAULT 1 COMMENT '1=模板可用',
  ADD UNIQUE KEY uk_canvas_template_key (template_key);

ALTER TABLE canvas
  ADD COLUMN is_example TINYINT NOT NULL DEFAULT 0 COMMENT '1=官方示例画布',
  ADD COLUMN source_template_key VARCHAR(100) NULL COMMENT '来源官方模板 key',
  ADD INDEX idx_example_template (is_example, source_template_key);

UPDATE canvas
SET is_example = 1
WHERE name LIKE '示例：%'
  AND created_by = 'system';
