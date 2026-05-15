-- V21：
-- 1. DIRECT_CALL 改造：绑定事件定义，明确入参结构
-- 2. BEHAVIOR_IN_APP 补充事件属性入参展示
-- 3. 示例画布：事件触发 → API 调用完整链路

-- DIRECT_CALL：从"裸直调"升级为"可绑定事件定义的直调触发器"
--   - 可选绑定事件编码（告知调用方应传哪些属性）
--   - 也可不绑定事件，保持原有直调行为（START 节点）
UPDATE `node_type_registry`
SET `config_schema` = '[
  {"key":"eventCode","label":"关联事件（可选）","type":"select","dataSource":"/meta/event-definitions","required":false},
  {"key":"eventParams","label":"入参说明","type":"api-input-params","apiKeyField":"eventCode","defsSource":"/meta/event-definitions","required":false}
]',
    `description` = '业务直调触发器；可选绑定事件定义以明确调用方应传的参数，也可不绑定直接调用',
    `type_name` = '直调触发'
WHERE `type_key` = 'DIRECT_CALL';

-- 示例画布：事件触发 → API 调用
INSERT INTO `canvas`
    (`name`, `description`, `status`, `created_by`, `edit_version`, `created_at`, `updated_at`)
VALUES
    ('示例：事件触发 API 调用',
     '演示完整事件驱动链路：上报 ORDER_COMPLETE 事件 → 带事件属性调用 Echo 接口 → 结束',
     0, 'system', 1, NOW(), NOW());

SET @ev_canvas_id = LAST_INSERT_ID();

INSERT INTO `canvas_version`
    (`canvas_id`, `version`, `status`, `graph_json`, `created_by`, `created_at`)
VALUES (@ev_canvas_id, 1, 0, '{
  "nodes": [
    {
      "id": "ev_trigger",
      "type": "BEHAVIOR_IN_APP",
      "name": "订单完成事件",
      "category": "行为策略",
      "x": 300, "y": 60,
      "config": { "eventCode": "ORDER_COMPLETE", "nextNodeId": "ev_api" },
      "bizConfig": { "eventCode": "ORDER_COMPLETE", "nextNodeId": "ev_api" }
    },
    {
      "id": "ev_api",
      "type": "API_CALL",
      "name": "Echo 接口验证",
      "category": "其他",
      "x": 300, "y": 220,
      "config": {
        "apiKey": "test-echo",
        "inputParams": {
          "orderId": "$${orderId}",
          "amount": "$${amount}"
        },
        "outputPrefix": "echo",
        "nextNodeId": "ev_end"
      },
      "bizConfig": { "nextNodeId": "ev_end" }
    },
    {
      "id": "ev_end",
      "type": "END",
      "name": "结束",
      "category": "流程控制",
      "x": 300, "y": 380,
      "config": {},
      "bizConfig": {}
    }
  ]
}', 'system', NOW());
