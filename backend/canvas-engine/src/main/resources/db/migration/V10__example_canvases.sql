-- V10: 示例画布种子数据（2 个可直接发布的参考画布）

-- 示例 1：行为触发 → 条件判断 → 发券
INSERT INTO `canvas`
    (`name`, `description`, `status`, `created_by`, `edit_version`, `created_at`, `updated_at`)
VALUES
    ('示例：新用户领券', '用户完成首次订单行为后，判断是否为新用户，是则发放新人券', 0, 'system', 1, NOW(), NOW());

SET @canvas1_id = LAST_INSERT_ID();

INSERT INTO `canvas_version`
    (`canvas_id`, `version`, `graph_json`, `created_by`, `created_at`)
VALUES (@canvas1_id, 1, '{
  "nodes": [
    {
      "id": "node_start",
      "type": "BEHAVIOR_IN_APP",
      "name": "用户行为触发",
      "category": "触发器",
      "x": 300, "y": 50,
      "config": {"eventCode": "ORDER_COMPLETE"},
      "bizConfig": {"nextNodeId": "node_check"}
    },
    {
      "id": "node_check",
      "type": "IF_CONDITION",
      "name": "是否新用户",
      "category": "逻辑分支",
      "x": 300, "y": 180,
      "config": {
        "rules": [{"field": "isNewUser", "operator": "EQ", "value": "true"}],
        "successNodeId": "node_coupon",
        "failNodeId": "node_end"
      },
      "bizConfig": {"successNodeId": "node_coupon", "failNodeId": "node_end"}
    },
    {
      "id": "node_coupon",
      "type": "COUPON",
      "name": "发放新人券",
      "category": "权益发放",
      "x": 150, "y": 310,
      "config": {"couponType": "flight_coupon", "nextNodeId": "node_end"},
      "bizConfig": {"nextNodeId": "node_end"}
    },
    {
      "id": "node_end",
      "type": "END",
      "name": "结束",
      "category": "流程控制",
      "x": 300, "y": 440,
      "config": {},
      "bizConfig": {}
    }
  ]
}', 'system', NOW());

SET @version1_id = LAST_INSERT_ID();

-- 示例 2：定时触发 → AB 分流 → 消息推送
INSERT INTO `canvas`
    (`name`, `description`, `status`, `created_by`, `edit_version`, `created_at`, `updated_at`)
VALUES
    ('示例：AB 分组推送', '每日定时对用户进行 AB 分流，A 组推送机票优惠，B 组推送酒店优惠', 0, 'system', 1, NOW(), NOW());

SET @canvas2_id = LAST_INSERT_ID();

INSERT INTO `canvas_version`
    (`canvas_id`, `version`, `graph_json`, `created_by`, `created_at`)
VALUES (@canvas2_id, 1, '{
  "nodes": [
    {
      "id": "node_timer",
      "type": "SCHEDULED_TRIGGER",
      "name": "每日9点定时",
      "category": "触发器",
      "x": 300, "y": 50,
      "config": {
        "scheduleType": "CRON",
        "cronExpression": "0 9 * * *",
        "timezone": "Asia/Shanghai",
        "userSource": {"type": "USER_LIST", "userIds": []},
        "nextNodeId": "node_ab"
      },
      "bizConfig": {"nextNodeId": "node_ab"}
    },
    {
      "id": "node_ab",
      "type": "AB_SPLIT",
      "name": "AB 分流",
      "category": "人群圈选",
      "x": 300, "y": 180,
      "config": {
        "experimentKey": "exp_push_campaign",
        "groups": [
          {"groupKey": "A", "nextNodeId": "node_push_flight"},
          {"groupKey": "B", "nextNodeId": "node_push_hotel"}
        ]
      },
      "bizConfig": {
        "groups": [
          {"groupKey": "A", "nextNodeId": "node_push_flight"},
          {"groupKey": "B", "nextNodeId": "node_push_hotel"}
        ]
      }
    },
    {
      "id": "node_push_flight",
      "type": "IN_APP_NOTIFY",
      "name": "推送机票优惠",
      "category": "用户触达",
      "x": 100, "y": 320,
      "config": {"messageCode": "flight_promo_push", "nextNodeId": "node_end"},
      "bizConfig": {"nextNodeId": "node_end"}
    },
    {
      "id": "node_push_hotel",
      "type": "IN_APP_NOTIFY",
      "name": "推送酒店优惠",
      "category": "用户触达",
      "x": 500, "y": 320,
      "config": {"messageCode": "hotel_promo_push", "nextNodeId": "node_end"},
      "bizConfig": {"nextNodeId": "node_end"}
    },
    {
      "id": "node_end",
      "type": "END",
      "name": "结束",
      "category": "流程控制",
      "x": 300, "y": 450,
      "config": {},
      "bizConfig": {}
    }
  ]
}', 'system', NOW());
