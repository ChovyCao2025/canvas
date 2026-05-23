INSERT INTO node_type_registry
  (type_key, type_name, category, handler_class,
   config_schema, output_schema, outlet_schema, summary_template, runtime_policy_schema,
   risk_level, is_trigger, is_terminal, description, enabled)
VALUES
('SEND_EMAIL','发送邮件','消息触达','org.chovy.canvas.engine.handlers.SendEmailHandler',
 '[
   {"key":"templateId","label":"模板","type":"text","required":true},
   {"key":"subject","label":"主题","type":"text"},
   {"key":"previewText","label":"预览文本","type":"text"},
   {"key":"variables","label":"变量映射","type":"key-value"}
 ]',
 '[{"name":"sendRecordId","type":"NUMBER","label":"发送记录ID"},{"name":"sendStatus","type":"STRING","label":"发送状态"},{"name":"externalMessageId","type":"STRING","label":"外部消息ID"}]',
 '[{"id":"success","label":"成功","color":"#52c41a","targetField":"successNodeId"},{"id":"fail","label":"失败","color":"#f5222d","targetField":"failNodeId"}]',
 '发送邮件（{{templateId}}）','[]','HIGH',0,0,'通过统一触达服务发送邮件，记录发送结果并支持幂等。',1),
('SEND_SMS','发送短信','消息触达','org.chovy.canvas.engine.handlers.SendSmsHandler',
 '[
   {"key":"templateId","label":"模板","type":"text","required":true},
   {"key":"content","label":"短信内容","type":"multi-text"},
   {"key":"variables","label":"变量映射","type":"key-value"}
 ]',
 '[{"name":"sendRecordId","type":"NUMBER","label":"发送记录ID"},{"name":"sendStatus","type":"STRING","label":"发送状态"},{"name":"externalMessageId","type":"STRING","label":"外部消息ID"}]',
 '[{"id":"success","label":"成功","color":"#52c41a","targetField":"successNodeId"},{"id":"fail","label":"失败","color":"#f5222d","targetField":"failNodeId"}]',
 '发送短信（{{templateId}}）','[]','HIGH',0,0,'通过统一触达服务发送短信，记录发送结果并支持幂等。',1),
('SEND_PUSH','发送 Push','消息触达','org.chovy.canvas.engine.handlers.SendPushHandler',
 '[
   {"key":"templateId","label":"模板","type":"text"},
   {"key":"title","label":"标题","type":"text","required":true},
   {"key":"body","label":"正文","type":"multi-text","required":true},
   {"key":"imageUrl","label":"图片","type":"text"},
   {"key":"clickUrl","label":"点击链接","type":"text"},
   {"key":"variables","label":"变量映射","type":"key-value"}
 ]',
 '[{"name":"sendRecordId","type":"NUMBER","label":"发送记录ID"},{"name":"sendStatus","type":"STRING","label":"发送状态"},{"name":"externalMessageId","type":"STRING","label":"外部消息ID"}]',
 '[{"id":"success","label":"成功","color":"#52c41a","targetField":"successNodeId"},{"id":"fail","label":"失败","color":"#f5222d","targetField":"failNodeId"}]',
 '发送 Push（{{title}}）','[]','HIGH',0,0,'通过统一触达服务发送 App Push，记录发送结果并支持幂等。',1),
('SEND_IN_APP','发送站内信','消息触达','org.chovy.canvas.engine.handlers.SendInAppHandler',
 '[
   {"key":"templateId","label":"模板","type":"text"},
   {"key":"title","label":"标题","type":"text","required":true},
   {"key":"body","label":"正文","type":"multi-text","required":true},
   {"key":"variables","label":"变量映射","type":"key-value"}
 ]',
 '[{"name":"sendRecordId","type":"NUMBER","label":"发送记录ID"},{"name":"sendStatus","type":"STRING","label":"发送状态"},{"name":"externalMessageId","type":"STRING","label":"外部消息ID"}]',
 '[{"id":"success","label":"成功","color":"#52c41a","targetField":"successNodeId"},{"id":"fail","label":"失败","color":"#f5222d","targetField":"failNodeId"}]',
 '发送站内信（{{title}}）','[]','MEDIUM',0,0,'通过统一触达服务发送站内信，记录发送结果并支持幂等。',1),
('SEND_WECHAT','发送微信消息','消息触达','org.chovy.canvas.engine.handlers.SendWechatHandler',
 '[
   {"key":"templateId","label":"模板","type":"text","required":true},
   {"key":"title","label":"标题","type":"text"},
   {"key":"body","label":"正文","type":"multi-text"},
   {"key":"clickUrl","label":"跳转链接","type":"text"},
   {"key":"variables","label":"变量映射","type":"key-value"}
 ]',
 '[{"name":"sendRecordId","type":"NUMBER","label":"发送记录ID"},{"name":"sendStatus","type":"STRING","label":"发送状态"},{"name":"externalMessageId","type":"STRING","label":"外部消息ID"}]',
 '[{"id":"success","label":"成功","color":"#52c41a","targetField":"successNodeId"},{"id":"fail","label":"失败","color":"#f5222d","targetField":"failNodeId"}]',
 '发送微信消息（{{templateId}}）','[]','HIGH',0,0,'通过统一触达服务发送微信模板或订阅消息，记录发送结果并支持幂等。',1);
