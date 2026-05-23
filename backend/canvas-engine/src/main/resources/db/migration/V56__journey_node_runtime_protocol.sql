ALTER TABLE node_type_registry
    ADD COLUMN `outlet_schema` TEXT NULL COMMENT '节点出口定义，驱动前端 handle 和发布校验',
    ADD COLUMN `summary_template` VARCHAR(500) NULL COMMENT '画布卡片摘要模板',
    ADD COLUMN `runtime_policy_schema` TEXT NULL COMMENT '节点通用运行策略配置 schema',
    ADD COLUMN `risk_level` VARCHAR(32) NOT NULL DEFAULT 'LOW' COMMENT 'LOW/MEDIUM/HIGH';

ALTER TABLE canvas_execution_trace
    ADD COLUMN `outcome` VARCHAR(32) NULL COMMENT 'NodeOutcome 执行结果',
    ADD COLUMN `reason_code` VARCHAR(128) NULL COMMENT '节点结果原因码',
    ADD COLUMN `reason_message` VARCHAR(500) NULL COMMENT '节点结果说明',
    ADD COLUMN `route_handle` VARCHAR(64) NULL COMMENT '实际选择的出口 handle';
