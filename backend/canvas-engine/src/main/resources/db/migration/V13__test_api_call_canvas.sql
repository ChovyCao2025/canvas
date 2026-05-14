-- V13: 测试用 API 定义 + 含 API_CALL 的最简示例画布

-- 1. 测试接口定义（httpbin.org echo，无需本地 mock）
INSERT INTO `api_definition`
    (`name`, `api_key`, `url`, `method`, `description`, `enabled`, `created_by`, `created_at`, `updated_at`)
VALUES
    ('Echo 测试接口', 'test-echo', 'https://httpbin.org/post', 'POST',
     '将请求体原样返回，用于验证 API_CALL 节点是否可以正常发起调用', 1, 'system', NOW(), NOW());

-- 2. 最简示例画布：START → API_CALL → END
INSERT INTO `canvas`
    (`name`, `description`, `status`, `created_by`, `edit_version`, `created_at`, `updated_at`)
VALUES
    ('示例：API 接口调用', 'START 节点触发，调用 Echo 测试接口，将 userId 作为入参传入，结果写入 echo.* 上下文', 0, 'system', 1, NOW(), NOW());

SET @canvas_api_id = LAST_INSERT_ID();

INSERT INTO `canvas_version`
    (`canvas_id`, `version`, `status`, `graph_json`, `created_by`, `created_at`)
VALUES (@canvas_api_id, 1, 0, '{
  "nodes": [
    {
      "id": "n_start",
      "type": "START",
      "name": "开始",
      "category": "流程控制",
      "x": 300, "y": 60,
      "config": {},
      "bizConfig": {"nextNodeId": "n_api"}
    },
    {
      "id": "n_api",
      "type": "API_CALL",
      "name": "调用 Echo 接口",
      "category": "其他",
      "x": 300, "y": 200,
      "config": {
        "apiKey": "test-echo",
        "inputParams": {
          "userId": "$${userId}",
          "source": "canvas-test"
        },
        "outputPrefix": "echo",
        "nextNodeId": "n_end"
      },
      "bizConfig": {"nextNodeId": "n_end"}
    },
    {
      "id": "n_end",
      "type": "END",
      "name": "结束",
      "category": "流程控制",
      "x": 300, "y": 340,
      "config": {},
      "bizConfig": {}
    }
  ]
}', 'system', NOW());
