-- V42: audience demo canvas seed
-- Demonstrates TAGGER audience mode with the three seeded demo audiences.

INSERT INTO `canvas`
    (`name`, `description`, `status`, `created_by`, `edit_version`, `created_at`, `updated_at`)
VALUES
    ('示例：人群圈选三路分流',
     '直调触发后依次判断三个演示人群：高价值近30天活跃用户 / 高频消费城市用户 / 多渠道高活跃用户，并走不同的站内信分支。',
     0, 'system', 1, NOW(), NOW());

SET @aud_demo_canvas_id = LAST_INSERT_ID();

INSERT INTO `canvas_version`
    (`canvas_id`, `version`, `status`, `graph_json`, `created_by`, `created_at`)
VALUES (@aud_demo_canvas_id, 1, 0, '{
  "nodes": [
    {
      "id": "aud_start",
      "type": "DIRECT_CALL",
      "name": "直调触发",
      "category": "触发器",
      "x": 420, "y": 40,
      "config": {},
      "bizConfig": {"nextNodeId": "aud_group_1"}
    },
    {
      "id": "aud_group_1",
      "type": "TAGGER",
      "name": "是否高价值近30天活跃用户",
      "category": "人群圈选",
      "x": 420, "y": 180,
      "config": {
        "mode": "audience",
        "audienceId": 90001,
        "hitNextNodeId": "aud_notify_1",
        "missNextNodeId": "aud_group_2"
      },
      "bizConfig": {
        "mode": "audience",
        "audienceId": 90001,
        "hitNextNodeId": "aud_notify_1",
        "missNextNodeId": "aud_group_2"
      }
    },
    {
      "id": "aud_group_2",
      "type": "TAGGER",
      "name": "是否高频消费城市用户",
      "category": "人群圈选",
      "x": 420, "y": 320,
      "config": {
        "mode": "audience",
        "audienceId": 90002,
        "hitNextNodeId": "aud_notify_2",
        "missNextNodeId": "aud_group_3"
      },
      "bizConfig": {
        "mode": "audience",
        "audienceId": 90002,
        "hitNextNodeId": "aud_notify_2",
        "missNextNodeId": "aud_group_3"
      }
    },
    {
      "id": "aud_group_3",
      "type": "TAGGER",
      "name": "是否多渠道高活跃用户",
      "category": "人群圈选",
      "x": 420, "y": 460,
      "config": {
        "mode": "audience",
        "audienceId": 90003,
        "hitNextNodeId": "aud_notify_3",
        "missNextNodeId": "aud_notify_default"
      },
      "bizConfig": {
        "mode": "audience",
        "audienceId": 90003,
        "hitNextNodeId": "aud_notify_3",
        "missNextNodeId": "aud_notify_default"
      }
    },
    {
      "id": "aud_notify_1",
      "type": "IN_APP_NOTIFY",
      "name": "命中人群1-发送高价值用户消息",
      "category": "用户触达",
      "x": 140, "y": 620,
      "config": {"messageCode": "flight_promo_push", "nextNodeId": "aud_end"},
      "bizConfig": {"nextNodeId": "aud_end"}
    },
    {
      "id": "aud_notify_2",
      "type": "IN_APP_NOTIFY",
      "name": "命中人群2-发送高频消费消息",
      "category": "用户触达",
      "x": 330, "y": 620,
      "config": {"messageCode": "hotel_promo_push", "nextNodeId": "aud_end"},
      "bizConfig": {"nextNodeId": "aud_end"}
    },
    {
      "id": "aud_notify_3",
      "type": "IN_APP_NOTIFY",
      "name": "命中人群3-发送高活跃消息",
      "category": "用户触达",
      "x": 520, "y": 620,
      "config": {"messageCode": "flight_promo_push", "nextNodeId": "aud_end"},
      "bizConfig": {"nextNodeId": "aud_end"}
    },
    {
      "id": "aud_notify_default",
      "type": "IN_APP_NOTIFY",
      "name": "未命中任何人群-发送兜底消息",
      "category": "用户触达",
      "x": 710, "y": 620,
      "config": {"messageCode": "hotel_promo_push", "nextNodeId": "aud_end"},
      "bizConfig": {"nextNodeId": "aud_end"}
    },
    {
      "id": "aud_end",
      "type": "END",
      "name": "结束",
      "category": "流程控制",
      "x": 420, "y": 780,
      "config": {},
      "bizConfig": {}
    }
  ]
}', 'system', NOW());
