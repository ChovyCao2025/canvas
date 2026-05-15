-- V17: DELAY 节点改为 delay-input 复合控件；MANUAL_APPROVAL 配置说明优化
UPDATE `node_type_registry`
SET `config_schema` = '[
  {"key":"duration","label":"延迟时长","type":"delay-input","required":true}
]'
WHERE `type_key` = 'DELAY';

-- MANUAL_APPROVAL：approveNodeId/rejectNodeId 改为只读提示（连线自动填充）
UPDATE `node_type_registry`
SET `config_schema` = '[
  {"key":"approvers","label":"审批人","type":"multi-user","required":true},
  {"key":"timeoutHours","label":"超时时间(小时)","type":"number","defaultValue":24},
  {"key":"onTimeout","label":"超时处理","type":"radio","options":[{"label":"拒绝","value":"REJECT"},{"label":"通过","value":"APPROVE"},{"label":"持续等待","value":"KEEP_WAITING"}],"required":true},
  {"key":"approveNodeId","label":"通过后节点","type":"edge-hint","hint":"从节点下方「通过」连接点拖线连接","icon":"check"},
  {"key":"rejectNodeId","label":"拒绝后节点","type":"edge-hint","hint":"从节点下方「拒绝」连接点拖线连接","icon":"close"}
]'
WHERE `type_key` = 'MANUAL_APPROVAL';
