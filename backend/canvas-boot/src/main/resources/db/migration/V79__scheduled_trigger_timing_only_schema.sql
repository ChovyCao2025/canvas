-- V79: SCHEDULED_TRIGGER only owns timing. Audience/user selection is handled by downstream TAGGER(audience).
UPDATE `node_type_registry`
SET `config_schema` = '[
  {"key":"scheduleType","label":"触发类型","type":"radio","options":[{"label":"指定时间(ONCE)","value":"ONCE"},{"label":"周期(CRON)","value":"CRON"}],"required":true},
  {"key":"cronExpression","label":"Cron 表达式","type":"cron","visible":"scheduleType==CRON","required":true},
  {"key":"triggerTime","label":"触发时间","type":"datetime","visible":"scheduleType==ONCE","required":true},
  {"key":"timezone","label":"时区","type":"input","defaultValue":"Asia/Shanghai"}
]'
WHERE `type_key` = 'SCHEDULED_TRIGGER';
