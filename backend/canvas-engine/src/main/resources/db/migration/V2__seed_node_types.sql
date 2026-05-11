-- ============================================================
-- V2 预置节点类型 & 上下文字段
-- ============================================================

-- ---- 行为策略类（触发器）----
INSERT INTO node_type_registry (type_key, type_name, category, handler_class, config_schema, output_schema, is_trigger, is_terminal, description) VALUES
('MQ_TRIGGER', 'MQ消息', '行为策略',
 'com.photon.canvas.engine.handlers.MqTriggerHandler',
 '[{"key":"topicKey","label":"消息主题","type":"select","dataSource":"/meta/mq-topics","required":true},{"key":"validateResult","label":"开启消息校验","type":"toggle"},{"key":"validateRules","label":"校验规则","type":"condition-rule-list","visible":"validateResult==true"}]',
 '[{"fieldKey":"orderId","fieldName":"订单号","dataType":"STRING"},{"fieldKey":"userId","fieldName":"用户ID","dataType":"STRING"}]',
 1, 0, '监听业务MQ消息触发流程'),

('BEHAVIOR_IN_APP', '端内用户行为', '行为策略',
 'com.photon.canvas.engine.handlers.BehaviorInAppHandler',
 '[{"key":"strategyRelation","label":"策略关系","type":"radio","options":[{"label":"且(AND)","value":"AND"},{"label":"或(OR)","value":"OR"}],"required":true},{"key":"strategies","label":"行为策略列表","type":"behavior-strategy-list","required":true}]',
 '[]',
 1, 0, '监听端内用户行为事件'),

('DIRECT_CALL', '业务直调', '行为策略',
 'com.photon.canvas.engine.handlers.DirectCallHandler',
 '[{"key":"inputParams","label":"入参定义","type":"param-define-list"}]',
 '[]',
 1, 0, '业务方HTTP调用直接触发活动'),

('TAGGER_REALTIME', 'Tagger实时标签', '行为策略',
 'com.photon.canvas.engine.handlers.TaggerRealtimeHandler',
 '[{"key":"tagCodeKey","label":"标签","type":"select","dataSource":"/meta/tagger-tags?type=realtime","required":true}]',
 '[]',
 1, 0, '监听Tagger实时标签MQ事件');

-- ---- 逻辑分支类 ----
INSERT INTO node_type_registry (type_key, type_name, category, handler_class, config_schema, output_schema, is_trigger, is_terminal, description) VALUES
('IF_CONDITION', 'IF判断', '逻辑分支',
 'com.photon.canvas.engine.handlers.IfConditionHandler',
 '[{"key":"rules","label":"条件规则","type":"condition-rule-list","required":true}]',
 '[]',
 0, 0, '单条件判断，分成功/失败两路'),

('SELECTOR', '条件选择器', '逻辑分支',
 'com.photon.canvas.engine.handlers.SelectorHandler',
 '[{"key":"branches","label":"分支条件","type":"branch-list","required":true}]',
 '[]',
 0, 0, '多条件分支，按顺序匹配第一个命中项'),

('LOGIC_RELATION', '逻辑关系', '逻辑分支',
 'com.photon.canvas.engine.handlers.LogicRelationHandler',
 '[{"key":"relation","label":"关系类型","type":"radio","options":[{"label":"且(AND)","value":"AND"},{"label":"或(OR)","value":"OR"}],"required":true}]',
 '[]',
 0, 0, '等待多个上游节点，AND/OR组合判断'),

('HUB', '集线器', '逻辑分支',
 'com.photon.canvas.engine.handlers.HubHandler',
 '[{"key":"timeout","label":"等待超时(秒)","type":"number","defaultValue":600,"required":false}]',
 '[]',
 0, 0, '等待所有上游节点完成（不论成功失败）后继续，超时后标记FAILED'),

('PRIORITY', '优先级', '逻辑分支',
 'com.photon.canvas.engine.handlers.PriorityHandler',
 '[{"key":"priorities","label":"优先级列表","type":"priority-list","required":true}]',
 '[]',
 0, 0, '按优先级顺序尝试子节点，成功即止');

-- ---- 人群圈选类 ----
INSERT INTO node_type_registry (type_key, type_name, category, handler_class, config_schema, output_schema, is_trigger, is_terminal, description) VALUES
('AB_SPLIT', 'AB分流', '人群圈选',
 'com.photon.canvas.engine.handlers.AbSplitHandler',
 '[{"key":"experimentKey","label":"实验","type":"select","dataSource":"/meta/ab-experiments","required":true},{"key":"groups","label":"分组路由","type":"ab-group-list","required":true}]',
 '[{"fieldKey":"abGroup","fieldName":"AB分组","dataType":"STRING"}]',
 0, 0, '基于Hash确定性分流到不同实验组'),

('TAGGER_OFFLINE', 'Tagger离线标签', '人群圈选',
 'com.photon.canvas.engine.handlers.TaggerOfflineHandler',
 '[{"key":"tagCodeKey","label":"离线标签","type":"select","dataSource":"/meta/tagger-tags?type=offline","required":true},{"key":"params","label":"标签参数","type":"key-value"}]',
 '[{"fieldKey":"tagValue","fieldName":"标签值","dataType":"STRING"}]',
 0, 0, '获取离线标签，为空则拦截流程');

-- ---- 权益发放类 ----
INSERT INTO node_type_registry (type_key, type_name, category, handler_class, config_schema, output_schema, is_trigger, is_terminal, description) VALUES
('COUPON', '代金券', '权益发放',
 'com.photon.canvas.engine.handlers.CouponHandler',
 '[{"key":"couponTypeKey","label":"券类型","type":"select","dataSource":"/meta/coupon-types","required":true},{"key":"params","label":"券参数","type":"dynamic-params","paramsSource":"couponTypeKey"}]',
 '[{"fieldKey":"couponAmount","fieldName":"券面额","dataType":"NUMBER"},{"fieldKey":"couponId","fieldName":"券ID","dataType":"STRING"}]',
 0, 0, '发放代金券，含防资损保护');

-- ---- 用户触达类 ----
INSERT INTO node_type_registry (type_key, type_name, category, handler_class, config_schema, output_schema, is_trigger, is_terminal, description) VALUES
('IN_APP_NOTIFY', '端内通知', '用户触达',
 'com.photon.canvas.engine.handlers.InAppNotifyHandler',
 '[{"key":"messageCodeKey","label":"消息类型","type":"select","dataSource":"/meta/message-codes?type=IN_APP","required":true},{"key":"bizData","label":"业务数据","type":"context-value-list"}]',
 '[]',
 0, 0, 'MQTT推送端内实时通知'),

('REACH_PLATFORM', '触达平台', '用户触达',
 'com.photon.canvas.engine.handlers.ReachPlatformHandler',
 '[{"key":"serviceSceneKey","label":"触达场景","type":"select","dataSource":"/meta/reach-scenes","required":true},{"key":"bizData","label":"业务数据","type":"context-value-list"}]',
 '[]',
 0, 0, '通过触达平台发Push/短信等'),

('DIRECT_RETURN', '直调返回', '用户触达',
 'com.photon.canvas.engine.handlers.DirectReturnHandler',
 '[{"key":"buildType","label":"构建方式","type":"radio","options":[{"label":"自定义","value":"CUSTOM"}]},{"key":"data","label":"返回数据","type":"context-value-list"}]',
 '[]',
 0, 1, '配合业务直调，定义同步返回数据');

-- ---- 其他类 ----
INSERT INTO node_type_registry (type_key, type_name, category, handler_class, config_schema, output_schema, is_trigger, is_terminal, description) VALUES
('API_CALL', '接口调用', '其他',
 'com.photon.canvas.engine.handlers.ApiCallHandler',
 '[{"key":"bizLineKey","label":"业务线","type":"select","dataSource":"/meta/biz-lines","required":true},{"key":"apiKey","label":"接口","type":"select","dataSource":"/meta/biz-lines/{bizLineKey}/apis","required":true},{"key":"params","label":"请求参数","type":"context-value-list"},{"key":"validateResult","label":"开启结果校验","type":"toggle"},{"key":"validateRules","label":"校验规则","type":"condition-rule-list","visible":"validateResult==true"}]',
 '[]',
 0, 0, '调用业务线内部HTTP接口'),

('DELAY', '延迟器', '其他',
 'com.photon.canvas.engine.handlers.DelayHandler',
 '[{"key":"duration","label":"延迟时长","type":"number","required":true},{"key":"unit","label":"时间单位","type":"select","options":[{"label":"秒","value":"SECOND"},{"label":"分钟","value":"MINUTE"},{"label":"小时","value":"HOUR"}],"required":true}]',
 '[]',
 0, 0, '延迟指定时长后继续执行'),

('SEND_MQ', '发送MQ', '其他',
 'com.photon.canvas.engine.handlers.SendMqHandler',
 '[{"key":"messageCodeKey","label":"消息类型","type":"select","dataSource":"/meta/message-codes?type=MQ","required":true},{"key":"params","label":"消息参数","type":"context-value-list"}]',
 '[]',
 0, 0, '向下游发出MQ消息'),

('GROOVY', 'Groovy脚本', '其他',
 'com.photon.canvas.engine.handlers.GroovyHandler',
 '[{"key":"inputParams","label":"输入参数","type":"context-value-list"},{"key":"code","label":"脚本代码","type":"code-editor","language":"groovy","required":true},{"key":"outputParams","label":"输出参数","type":"param-define-list"},{"key":"validateResult","label":"开启输出校验","type":"toggle"},{"key":"validateRules","label":"校验规则","type":"condition-rule-list","visible":"validateResult==true"}]',
 '[]',
 0, 0, '执行Groovy脚本处理复杂业务逻辑');

-- ---- 预置上下文字段 ----
INSERT INTO context_field (field_key, field_name, data_type, source_node_type, description) VALUES
('orderId',        '订单号',   'STRING',  'MQ_TRIGGER,API_CALL', '业务订单唯一标识'),
('userId',         '用户ID',   'STRING',  'MQ_TRIGGER',          '触发用户ID'),
('departureDate',  '出发日期', 'STRING',  'MQ_TRIGGER',          '机票出发日期'),
('marketIdentity', '市场身份', 'STRING',  'TAGGER_OFFLINE',      '新客/老客等身份标识'),
('couponAmount',   '券面额',   'NUMBER',  'COUPON',              '发放的代金券金额'),
('couponId',       '券ID',     'STRING',  'COUPON',              '发放的券唯一标识'),
('abGroup',        'AB分组',   'STRING',  'AB_SPLIT',            '实验分组标识，如A/B'),
('tagValue',       '标签值',   'STRING',  'TAGGER_OFFLINE',      '离线标签值');
