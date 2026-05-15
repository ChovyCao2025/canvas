-- V15: 更新 API_CALL 节点配置 schema，inputParams 改为 api-input-params 类型
UPDATE `node_type_registry`
SET `config_schema` = '[
  {"key":"apiKey","label":"选择接口","type":"select","dataSource":"/meta/api-definitions","required":true},
  {"key":"inputParams","label":"入参映射","type":"api-input-params","required":false},
  {"key":"outputPrefix","label":"出参前缀","type":"input","required":false},
  {"key":"nextNodeId","label":"后继节点","type":"input","required":false}
]'
WHERE `type_key` = 'API_CALL';
