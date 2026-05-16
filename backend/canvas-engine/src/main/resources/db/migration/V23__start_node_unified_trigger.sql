-- V23: START 节点升级为统一入口，支持配置触发方式
UPDATE `node_type_registry`
SET `config_schema` = '[
  {"key":"triggerType","label":"触发方式","type":"radio","required":true,"defaultValue":"DIRECT","options":[
    {"label":"手动直调","value":"DIRECT"},
    {"label":"事件触发","value":"EVENT"},
    {"label":"定时触发","value":"SCHEDULED"},
    {"label":"MQ消息",  "value":"MQ"}
  ]},
  {"key":"eventCode",      "label":"触发事件",   "type":"select","dataSource":"/meta/event-definitions","required":true, "visible":"triggerType==EVENT"},
  {"key":"cronExpression",  "label":"Cron表达式", "type":"cron",  "required":true,  "visible":"triggerType==SCHEDULED"},
  {"key":"triggerTime",     "label":"触发时间",   "type":"datetime","required":false,"visible":"triggerType==SCHEDULED"},
  {"key":"timezone",        "label":"时区",       "type":"input", "defaultValue":"Asia/Shanghai","visible":"triggerType==SCHEDULED"},
  {"key":"topicKey",        "label":"消息类型",   "type":"select","dataSource":"/meta/mq-definitions","required":true,"visible":"triggerType==MQ"},
  {"key":"userSource",      "label":"用户来源",   "type":"user-source-config","required":true,"visible":"triggerType==SCHEDULED"}
]',
    `description` = '统一入口节点：选择触发方式（手动直调/事件触发/定时触发/MQ消息），配置完成后连接下游节点'
WHERE `type_key` = 'START';
