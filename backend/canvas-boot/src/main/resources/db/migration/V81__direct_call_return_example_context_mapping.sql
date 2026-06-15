-- V81: Make the DIRECT_CALL -> API_CALL -> DIRECT_RETURN example self-contained.
--
-- Runtime context is not created by graph_json itself. DIRECT_CALL contributes
-- trigger payload keys, API_CALL writes response fields using outputPrefix, and
-- DIRECT_RETURN reads those runtime keys back from ExecutionContext.

UPDATE api_definition
SET
  url = 'http://localhost:8089/mock/user/info',
  method = 'POST',
  request_schema = '[{"name":"userId","displayName":"用户ID","type":"STRING","required":true}]',
  response_schema = '[
    {"name":"status","desc":"查询结果状态","type":"STRING"},
    {"name":"nickname","desc":"用户昵称","type":"STRING"},
    {"name":"level","desc":"用户等级","type":"NUMBER"},
    {"name":"isNewUser","desc":"是否新用户","type":"BOOLEAN"}
  ]',
  enabled = 1
WHERE api_key = 'query_user_info';

INSERT INTO context_field
  (field_key, field_name, data_type, source_node_type, description)
VALUES
  ('user.status', '用户查询状态', 'STRING', 'API_CALL', 'query_user_info with outputPrefix=user'),
  ('user.nickname', '用户昵称', 'STRING', 'API_CALL', 'query_user_info with outputPrefix=user'),
  ('user.level', '用户等级', 'NUMBER', 'API_CALL', 'query_user_info with outputPrefix=user'),
  ('user.isNewUser', '是否新用户', 'BOOLEAN', 'API_CALL', 'query_user_info with outputPrefix=user'),
  ('user.httpStatus', '用户查询 HTTP 状态', 'STRING', 'API_CALL', 'API_CALL HTTP status with outputPrefix=user')
ON DUPLICATE KEY UPDATE
  field_name = VALUES(field_name),
  data_type = VALUES(data_type),
  source_node_type = VALUES(source_node_type),
  description = VALUES(description);

UPDATE canvas_template
SET graph_json = CAST(JSON_SET(
        graph_json,
        '$.nodes[1].config.inputParams', CAST('[{"name":"userId","required":true,"desc":"用户ID"}]' AS JSON),
        '$.nodes[1].config.nextNodeId', 'api',
        '$.nodes[1].bizConfig.inputParams', CAST('[{"name":"userId","required":true,"desc":"用户ID"}]' AS JSON),
        '$.nodes[1].bizConfig.nextNodeId', 'api',
        '$.nodes[2].config.apiKey', 'query_user_info',
        '$.nodes[2].config.inputParams', JSON_OBJECT('userId', '$${userId}'),
        '$.nodes[2].config.outputPrefix', 'user',
        '$.nodes[2].config.nextNodeId', 'return',
        '$.nodes[2].bizConfig.apiKey', 'query_user_info',
        '$.nodes[2].bizConfig.inputParams', JSON_OBJECT('userId', '$${userId}'),
        '$.nodes[2].bizConfig.outputPrefix', 'user',
        '$.nodes[2].bizConfig.nextNodeId', 'return',
        '$.nodes[3].config.buildType', 'CUSTOM',
        '$.nodes[3].config.data', CAST('[
          {"name":"userLevel","valueType":"CONTEXT","value":"user.level"},
          {"name":"nickname","valueType":"CONTEXT","value":"user.nickname"},
          {"name":"apiStatus","valueType":"CONTEXT","value":"user.status"}
        ]' AS JSON),
        '$.nodes[3].bizConfig.buildType', 'CUSTOM',
        '$.nodes[3].bizConfig.data', CAST('[
          {"name":"userLevel","valueType":"CONTEXT","value":"user.level"},
          {"name":"nickname","valueType":"CONTEXT","value":"user.nickname"},
          {"name":"apiStatus","valueType":"CONTEXT","value":"user.status"}
        ]' AS JSON)
    ) AS CHAR)
WHERE template_key = 'component_direct_call_return'
  AND JSON_UNQUOTE(JSON_EXTRACT(graph_json, '$.nodes[1].type')) = 'DIRECT_CALL'
  AND JSON_UNQUOTE(JSON_EXTRACT(graph_json, '$.nodes[2].type')) = 'API_CALL'
  AND JSON_UNQUOTE(JSON_EXTRACT(graph_json, '$.nodes[3].type')) = 'DIRECT_RETURN';

UPDATE canvas_version cv
JOIN canvas c ON c.id = cv.canvas_id
SET cv.graph_json = CAST(JSON_SET(
        cv.graph_json,
        '$.nodes[1].config.inputParams', CAST('[{"name":"userId","required":true,"desc":"用户ID"}]' AS JSON),
        '$.nodes[1].config.nextNodeId', 'api',
        '$.nodes[1].bizConfig.inputParams', CAST('[{"name":"userId","required":true,"desc":"用户ID"}]' AS JSON),
        '$.nodes[1].bizConfig.nextNodeId', 'api',
        '$.nodes[2].config.apiKey', 'query_user_info',
        '$.nodes[2].config.inputParams', JSON_OBJECT('userId', '$${userId}'),
        '$.nodes[2].config.outputPrefix', 'user',
        '$.nodes[2].config.nextNodeId', 'return',
        '$.nodes[2].bizConfig.apiKey', 'query_user_info',
        '$.nodes[2].bizConfig.inputParams', JSON_OBJECT('userId', '$${userId}'),
        '$.nodes[2].bizConfig.outputPrefix', 'user',
        '$.nodes[2].bizConfig.nextNodeId', 'return',
        '$.nodes[3].config.buildType', 'CUSTOM',
        '$.nodes[3].config.data', CAST('[
          {"name":"userLevel","valueType":"CONTEXT","value":"user.level"},
          {"name":"nickname","valueType":"CONTEXT","value":"user.nickname"},
          {"name":"apiStatus","valueType":"CONTEXT","value":"user.status"}
        ]' AS JSON),
        '$.nodes[3].bizConfig.buildType', 'CUSTOM',
        '$.nodes[3].bizConfig.data', CAST('[
          {"name":"userLevel","valueType":"CONTEXT","value":"user.level"},
          {"name":"nickname","valueType":"CONTEXT","value":"user.nickname"},
          {"name":"apiStatus","valueType":"CONTEXT","value":"user.status"}
        ]' AS JSON)
    ) AS CHAR)
WHERE c.source_template_key = 'component_direct_call_return'
  AND JSON_UNQUOTE(JSON_EXTRACT(cv.graph_json, '$.nodes[1].type')) = 'DIRECT_CALL'
  AND JSON_UNQUOTE(JSON_EXTRACT(cv.graph_json, '$.nodes[2].type')) = 'API_CALL'
  AND JSON_UNQUOTE(JSON_EXTRACT(cv.graph_json, '$.nodes[3].type')) = 'DIRECT_RETURN';
