-- V36: 示例画布 — 事件触发 → API 调用 → 金额分级路由
--
-- 流程：ORDER_COMPLETE 事件触发 → 查询订单详情（API）
--       → IF 金额 > 1000 → 大额审核接口 / 小额自动处理接口 → END
--
-- 事件上下文字段：orderId、amount、userId
-- IF 条件基于事件携带的 amount 字段直接判断

-- ── 1. API 定义 ──────────────────────────────────────────────────────────────

INSERT INTO `api_definition`
    (`name`, `api_key`, `url`, `method`, `description`, `enabled`, `created_by`, `created_at`, `updated_at`)
VALUES
    ('查询订单详情',   'order-detail',
     'http://localhost:8099/mock/order/detail', 'POST',
     '根据 orderId 查询订单详情（riskLevel、currency 等），结果写入 detail.* 上下文',
     1, 'system', NOW(), NOW()),

    ('大额订单处理',   'order-large-handler',
     'http://localhost:8099/mock/order/large',  'POST',
     '金额 > 1000 时提交人工审核，返回 reviewId',
     1, 'system', NOW(), NOW()),

    ('小额订单处理',   'order-small-handler',
     'http://localhost:8099/mock/order/small',  'POST',
     '金额 ≤ 1000 时自动处理，返回 processId',
     1, 'system', NOW(), NOW());

-- ── 2. 示例画布 ──────────────────────────────────────────────────────────────

INSERT INTO `canvas`
    (`name`, `description`, `status`, `created_by`, `edit_version`, `created_at`, `updated_at`)
VALUES (
    '示例：事件触发 → 金额分级 API 路由',
    'ORDER_COMPLETE 事件触发，先调用订单详情接口，再根据 amount > 1000 路由至不同的处理接口',
    0, 'system', 1, NOW(), NOW()
);

SET @canvas_id = LAST_INSERT_ID();

INSERT INTO `canvas_version`
    (`canvas_id`, `version`, `status`, `graph_json`, `created_by`, `created_at`)
VALUES (@canvas_id, 1, 0, '{
  "nodes": [
    {
      "id": "n_start",
      "type": "START",
      "name": "开始",
      "category": "流程控制",
      "x": 400, "y": 0,
      "config": {"nextNodeId": "n_event"}
    },
    {
      "id": "n_event",
      "type": "EVENT_TRIGGER",
      "name": "订单完成事件",
      "category": "行为策略",
      "x": 400, "y": 130,
      "config": {
        "eventCode": "ORDER_COMPLETE",
        "nextNodeId": "n_detail"
      }
    },
    {
      "id": "n_detail",
      "type": "API_CALL",
      "name": "查询订单详情",
      "category": "其他",
      "x": 400, "y": 260,
      "config": {
        "apiKey": "order-detail",
        "inputParams": {"orderId": "$${orderId}"},
        "outputPrefix": "detail",
        "nextNodeId": "n_check"
      }
    },
    {
      "id": "n_check",
      "type": "IF_CONDITION",
      "name": "金额是否 > 1000",
      "category": "逻辑分支",
      "x": 400, "y": 390,
      "config": {
        "rules": [{"field": "amount", "operator": "GT", "value": "1000"}],
        "successNodeId": "n_large",
        "failNodeId": "n_small"
      }
    },
    {
      "id": "n_large",
      "type": "API_CALL",
      "name": "大额订单处理",
      "category": "其他",
      "x": 180, "y": 520,
      "config": {
        "apiKey": "order-large-handler",
        "inputParams": {
          "orderId": "$${orderId}",
          "amount": "$${amount}",
          "userId": "$${userId}"
        },
        "outputPrefix": "large",
        "nextNodeId": "n_end"
      }
    },
    {
      "id": "n_small",
      "type": "API_CALL",
      "name": "小额订单处理",
      "category": "其他",
      "x": 620, "y": 520,
      "config": {
        "apiKey": "order-small-handler",
        "inputParams": {
          "orderId": "$${orderId}",
          "amount": "$${amount}",
          "userId": "$${userId}"
        },
        "outputPrefix": "small",
        "nextNodeId": "n_end"
      }
    },
    {
      "id": "n_end",
      "type": "END",
      "name": "结束",
      "category": "流程控制",
      "x": 400, "y": 650,
      "config": {}
    }
  ]
}', 'system', NOW());
