-- V28: Fix SEND_MQ config_schema — api-input-params must specify apiKeyField + defsSource
-- Root cause: without these overrides the component defaults to apiKey + /meta/api-definitions,
-- so selecting a message type never populates the params section.
UPDATE node_type_registry SET config_schema = '[{"key":"messageCodeKey","label":"消息类型","type":"select","dataSource":"/meta/mq-definitions","required":true},{"key":"params","label":"消息参数","type":"api-input-params","apiKeyField":"messageCodeKey","defsSource":"/meta/mq-definitions","required":false}]' WHERE type_key = 'SEND_MQ';
