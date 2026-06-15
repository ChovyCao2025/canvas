-- V37: 补全 API response_schema
--
-- INSERT IGNORE：如果 api_key 已存在则跳过，保持幂等（不修改现有 url）

-- ── 1. 补充现有 API 的 response_schema ───────────────────────────

UPDATE `api_definition`
SET `response_schema` = '[
  {"name":"json","desc":"请求体原样回传","type":"OBJECT"},
  {"name":"url","desc":"请求 URL","type":"STRING"}
]'
WHERE `api_key` = 'test-echo';

UPDATE `api_definition`
SET `response_schema` = '[
  {"name":"status","desc":"处理状态","type":"STRING"},
  {"name":"riskLevel","desc":"风险等级(LOW/MEDIUM/HIGH)","type":"STRING"},
  {"name":"currency","desc":"货币类型","type":"STRING"},
  {"name":"channel","desc":"渠道来源","type":"STRING"}
]'
WHERE `api_key` = 'order-detail';

UPDATE `api_definition`
SET `response_schema` = '[
  {"name":"status","desc":"提交结果","type":"STRING"},
  {"name":"message","desc":"处理说明","type":"STRING"},
  {"name":"reviewId","desc":"审核单号","type":"STRING"}
]'
WHERE `api_key` = 'order-large-handler';

UPDATE `api_definition`
SET `response_schema` = '[
  {"name":"status","desc":"处理结果","type":"STRING"},
  {"name":"message","desc":"处理说明","type":"STRING"},
  {"name":"processId","desc":"处理流水号","type":"STRING"}
]'
WHERE `api_key` = 'order-small-handler';

-- ── 2. query_user_info：补全 response_schema（保留现有 url 不动）──

UPDATE `api_definition`
SET
  `request_schema`  = '[{"name":"userId","displayName":"用户ID","type":"STRING","required":true}]',
  `response_schema` = '[
    {"name":"nickname","desc":"用户昵称","type":"STRING"},
    {"name":"level","desc":"用户等级(1-5)","type":"NUMBER"},
    {"name":"isNewUser","desc":"是否新用户","type":"BOOLEAN"},
    {"name":"status","desc":"查询结果状态","type":"STRING"}
  ]'
WHERE `api_key` = 'query_user_info';
