-- V89: Repair Canvas42 as an executable START -> DIRECT_CALL -> SELECTOR demo.
--
-- The original V42 audience demo started directly from DIRECT_CALL and modeled
-- multi-way routing as chained TAGGER nodes. This version makes the example
-- usable as a selector demo: invoke it with userTier = VIP | CITY | ACTIVE or
-- any other value to reach the fallback branch.

SET @canvas42_graph = '{
  "nodes": [
    {
      "id": "start",
      "type": "START",
      "name": "开始",
      "category": "流程控制",
      "x": 420, "y": 0,
      "config": {"nextNodeId": "aud_direct"},
      "bizConfig": {"nextNodeId": "aud_direct"}
    },
    {
      "id": "aud_direct",
      "type": "DIRECT_CALL",
      "name": "直调演示入口",
      "category": "入口节点",
      "x": 420, "y": 140,
      "config": {
        "inputParams": [
          {"name": "userTier", "description": "用户分层：VIP / CITY / ACTIVE / OTHER", "dataType": "STRING", "required": true}
        ],
        "nextNodeId": "aud_selector"
      },
      "bizConfig": {
        "inputParams": [
          {"name": "userTier", "description": "用户分层：VIP / CITY / ACTIVE / OTHER", "dataType": "STRING", "required": true}
        ],
        "nextNodeId": "aud_selector"
      }
    },
    {
      "id": "aud_selector",
      "type": "SELECTOR",
      "name": "按用户分层选择",
      "category": "逻辑分支",
      "x": 420, "y": 300,
      "config": {
        "branches": [
          {
            "label": "高价值近30天活跃用户",
            "strategyRelation": "AND",
            "conditions": [{"field": "userTier", "operator": "EQ", "value": "VIP", "isCustom": true}],
            "nextNodeId": "aud_notify_1"
          },
          {
            "label": "高频消费城市用户",
            "strategyRelation": "AND",
            "conditions": [{"field": "userTier", "operator": "EQ", "value": "CITY", "isCustom": true}],
            "nextNodeId": "aud_notify_2"
          },
          {
            "label": "多渠道高活跃用户",
            "strategyRelation": "AND",
            "conditions": [{"field": "userTier", "operator": "EQ", "value": "ACTIVE", "isCustom": true}],
            "nextNodeId": "aud_notify_3"
          }
        ],
        "elseNodeId": "aud_notify_default"
      },
      "bizConfig": {
        "branches": [
          {
            "label": "高价值近30天活跃用户",
            "strategyRelation": "AND",
            "conditions": [{"field": "userTier", "operator": "EQ", "value": "VIP", "isCustom": true}],
            "nextNodeId": "aud_notify_1"
          },
          {
            "label": "高频消费城市用户",
            "strategyRelation": "AND",
            "conditions": [{"field": "userTier", "operator": "EQ", "value": "CITY", "isCustom": true}],
            "nextNodeId": "aud_notify_2"
          },
          {
            "label": "多渠道高活跃用户",
            "strategyRelation": "AND",
            "conditions": [{"field": "userTier", "operator": "EQ", "value": "ACTIVE", "isCustom": true}],
            "nextNodeId": "aud_notify_3"
          }
        ],
        "elseNodeId": "aud_notify_default"
      }
    },
    {
      "id": "aud_notify_1",
      "type": "IN_APP_NOTIFY",
      "name": "命中高价值用户消息",
      "category": "用户触达",
      "x": 80, "y": 500,
      "config": {"messageCodeKey": "vip_message", "nextNodeId": "aud_end"},
      "bizConfig": {"messageCodeKey": "vip_message", "nextNodeId": "aud_end"}
    },
    {
      "id": "aud_notify_2",
      "type": "IN_APP_NOTIFY",
      "name": "命中城市高频用户消息",
      "category": "用户触达",
      "x": 300, "y": 500,
      "config": {"messageCodeKey": "city_message", "nextNodeId": "aud_end"},
      "bizConfig": {"messageCodeKey": "city_message", "nextNodeId": "aud_end"}
    },
    {
      "id": "aud_notify_3",
      "type": "IN_APP_NOTIFY",
      "name": "命中高活跃用户消息",
      "category": "用户触达",
      "x": 520, "y": 500,
      "config": {"messageCodeKey": "active_message", "nextNodeId": "aud_end"},
      "bizConfig": {"messageCodeKey": "active_message", "nextNodeId": "aud_end"}
    },
    {
      "id": "aud_notify_default",
      "type": "IN_APP_NOTIFY",
      "name": "未命中-兜底消息",
      "category": "用户触达",
      "x": 740, "y": 500,
      "config": {"messageCodeKey": "fallback_message", "nextNodeId": "aud_end"},
      "bizConfig": {"messageCodeKey": "fallback_message", "nextNodeId": "aud_end"}
    },
    {
      "id": "aud_end",
      "type": "END",
      "name": "结束",
      "category": "流程控制",
      "x": 420, "y": 680,
      "config": {},
      "bizConfig": {}
    }
  ]
}';

UPDATE canvas c
SET c.description = '直调输入 userTier 后进入 SELECTOR 条件选择器：VIP / CITY / ACTIVE 分别命中三条演示分支，其他值进入兜底分支。',
    c.edit_version = c.edit_version + 1,
    c.updated_at = NOW()
WHERE c.name = '示例：人群圈选三路分流';

UPDATE canvas_version cv
JOIN canvas c ON c.id = cv.canvas_id
SET cv.graph_json = @canvas42_graph
WHERE c.name = '示例：人群圈选三路分流'
  AND cv.status = 0;
