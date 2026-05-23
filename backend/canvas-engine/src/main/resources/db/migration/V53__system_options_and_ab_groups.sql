-- V53: configurable system options and AB experiment groups

CREATE TABLE IF NOT EXISTS system_option (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  category VARCHAR(80) NOT NULL,
  option_key VARCHAR(120) NOT NULL,
  label VARCHAR(200) NOT NULL,
  description VARCHAR(500) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  enabled TINYINT NOT NULL DEFAULT 1,
  system_builtin TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_system_option_category_key (category, option_key),
  KEY idx_system_option_category_enabled_sort (category, enabled, sort_order, id)
);

CREATE TABLE IF NOT EXISTS ab_experiment_group (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  experiment_id BIGINT NOT NULL,
  group_key VARCHAR(64) NOT NULL,
  label VARCHAR(200) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_ab_experiment_group_key (experiment_id, group_key),
  KEY idx_ab_experiment_group_enabled_sort (experiment_id, enabled, sort_order, id)
);

INSERT INTO system_option
  (category, option_key, label, description, sort_order, enabled, system_builtin)
VALUES
  ('condition_operator', 'EQ', '等于', '条件规则操作符', 10, 1, 1),
  ('condition_operator', 'NEQ', '不等于', '条件规则操作符', 20, 1, 1),
  ('condition_operator', 'CONTAINS', '包含', '条件规则操作符', 30, 1, 1),
  ('condition_operator', 'GT', '大于', '条件规则操作符', 40, 1, 1),
  ('condition_operator', 'LT', '小于', '条件规则操作符', 50, 1, 1),
  ('condition_operator', 'GTE', '大于等于', '条件规则操作符', 60, 1, 1),
  ('condition_operator', 'LTE', '小于等于', '条件规则操作符', 70, 1, 1),
  ('audience_condition_operator', '=', '等于', '人群规则操作符', 10, 1, 1),
  ('audience_condition_operator', '!=', '不等于', '人群规则操作符', 20, 1, 1),
  ('audience_condition_operator', '>', '大于', '人群规则操作符', 30, 1, 1),
  ('audience_condition_operator', '>=', '大于等于', '人群规则操作符', 40, 1, 1),
  ('audience_condition_operator', '<', '小于', '人群规则操作符', 50, 1, 1),
  ('audience_condition_operator', '<=', '小于等于', '人群规则操作符', 60, 1, 1),
  ('audience_condition_operator', 'in', '包含于', '人群规则操作符', 70, 1, 1),
  ('logic_relation', 'AND', '且(AND)', '逻辑关系', 10, 1, 1),
  ('logic_relation', 'OR', '或(OR)', '逻辑关系', 20, 1, 1),
  ('query_combinator', 'and', '且（AND）', '人群规则组合关系', 10, 1, 1),
  ('query_combinator', 'or', '或（OR）', '人群规则组合关系', 20, 1, 1),
  ('param_type', 'STRING', '字符型', '参数类型', 10, 1, 1),
  ('param_type', 'NUMBER', '数值型', '参数类型', 20, 1, 1),
  ('param_type', 'TEXT', '文本型', '参数类型', 30, 1, 1),
  ('param_type', 'DATE', '日期型', '参数类型', 40, 1, 1),
  ('param_type', 'STRING_PARAM', '字符型（参数调用）', '参数类型', 50, 1, 1),
  ('param_type', 'BOOLEAN', '布尔型', '参数类型', 60, 1, 1),
  ('param_type', 'LIST', '列表', '参数类型', 70, 1, 1),
  ('event_attr_type', 'STRING', '字符型', '事件属性类型', 10, 1, 1),
  ('event_attr_type', 'NUMBER', '数值型', '事件属性类型', 20, 1, 1),
  ('event_attr_type', 'DATE', '日期型', '事件属性类型', 30, 1, 1),
  ('http_method', 'GET', 'GET', 'API 请求方法', 10, 1, 1),
  ('http_method', 'POST', 'POST', 'API 请求方法', 20, 1, 1),
  ('tag_type', 'offline', '离线标签', '标签类型', 10, 1, 1),
  ('tag_type', 'realtime', '实时标签', '标签类型', 20, 1, 1),
  ('audience_data_source_type', 'TAGGER_API', 'Tagger API', '人群数据源类型', 10, 1, 1),
  ('audience_data_source_type', 'JDBC', 'JDBC', '人群数据源类型', 20, 1, 1),
  ('audience_evaluation_strategy', 'OFFLINE_BATCH', '离线批量', '人群计算策略', 10, 1, 1),
  ('audience_evaluation_strategy', 'ONLINE', '实时计算', '人群计算策略', 20, 1, 1),
  ('audience_evaluation_strategy', 'HYBRID', '混合', '人群计算策略', 30, 1, 1),
  ('audience_engine_type', 'AVIATOR', 'AviatorScript', '人群规则引擎', 10, 1, 1),
  ('audience_engine_type', 'QL', 'QLExpress', '人群规则引擎', 20, 1, 1),
  ('user_role', 'ADMIN', 'ADMIN（管理员）', '用户角色', 10, 1, 1),
  ('user_role', 'OPERATOR', 'OPERATOR（运营）', '用户角色', 20, 1, 1),
  ('context_value_type', 'CUSTOM', '自定义', '上下文值类型', 10, 1, 1),
  ('context_value_type', 'CONTEXT', '上下文', '上下文值类型', 20, 1, 1),
  ('delay_unit', 'SECOND', '秒', '延迟单位', 10, 1, 1),
  ('delay_unit', 'MINUTE', '分钟', '延迟单位', 20, 1, 1),
  ('delay_unit', 'HOUR', '小时', '延迟单位', 30, 1, 1),
  ('cron_frequency', 'daily', '每天', 'Cron 频率', 10, 1, 1),
  ('cron_frequency', 'weekly', '每周', 'Cron 频率', 20, 1, 1),
  ('cron_frequency', 'monthly', '每月', 'Cron 频率', 30, 1, 1),
  ('cron_frequency', 'hourly', '每小时', 'Cron 频率', 40, 1, 1),
  ('weekday', '1', '周一', '周几', 10, 1, 1),
  ('weekday', '2', '周二', '周几', 20, 1, 1),
  ('weekday', '3', '周三', '周几', 30, 1, 1),
  ('weekday', '4', '周四', '周几', 40, 1, 1),
  ('weekday', '5', '周五', '周几', 50, 1, 1),
  ('weekday', '6', '周六', '周几', 60, 1, 1),
  ('weekday', '0', '周日', '周几', 70, 1, 1),
  ('schedule_type', 'ONCE', '指定时间(ONCE)', '定时触发类型', 10, 1, 1),
  ('schedule_type', 'CRON', '周期(CRON)', '定时触发类型', 20, 1, 1),
  ('tagger_mode', 'realtime', '实时触发（监听 MQ 事件）', 'Tagger 模式', 10, 1, 1),
  ('tagger_mode', 'offline', '离线打标（流程内执行）', 'Tagger 模式', 20, 1, 1),
  ('tagger_mode', 'audience', '人群圈选', 'Tagger 模式', 30, 1, 1),
  ('threshold_mode', 'min_success', '成功数 ≥ N（K-of-N 投票）', '阈值触发条件', 10, 1, 1),
  ('threshold_mode', 'min_done', '完成数 ≥ N（SUCCESS+FAILED 均计）', '阈值触发条件', 20, 1, 1),
  ('threshold_mode', 'any_fail', '任意上游失败立刻触发', '阈值触发条件', 30, 1, 1),
  ('aggregate_evaluate_mode', 'count', '成功数 ≥ N', '聚合评估方式', 10, 1, 1),
  ('aggregate_evaluate_mode', 'rate', '成功率 ≥ N%', '聚合评估方式', 20, 1, 1),
  ('aggregate_evaluate_mode', 'script', '自定义脚本', '聚合评估方式', 30, 1, 1),
  ('approval_timeout_action', 'REJECT', '拒绝', '审批超时动作', 10, 1, 1),
  ('approval_timeout_action', 'APPROVE', '通过', '审批超时动作', 20, 1, 1),
  ('approval_timeout_action', 'KEEP_WAITING', '持续等待', '审批超时动作', 30, 1, 1),
  ('canvas_invoke_mode', 'SYNC', '同步等待', '画布调用模式', 10, 1, 1),
  ('canvas_invoke_mode', 'ASYNC', '异步触发', '画布调用模式', 20, 1, 1),
  ('direct_return_build_type', 'CUSTOM', '自定义', '直调返回构建方式', 10, 1, 1),
  ('coupon_type', 'flight_coupon', '机票代金券', '券类型', 10, 1, 1),
  ('coupon_type', 'hotel_coupon', '酒店代金券', '券类型', 20, 1, 1),
  ('coupon_type', 'train_coupon', '火车票代金券', '券类型', 30, 1, 1),
  ('reach_scene', 'quick_booking_push', '急速预订Push', '触达场景', 10, 1, 1),
  ('reach_scene', 'hotel_recommend_push', '酒店推荐Push', '触达场景', 20, 1, 1),
  ('reach_scene', 'coupon_reminder_sms', '领券提醒短信', '触达场景', 30, 1, 1),
  ('biz_line', 'FLIGHT', '机票', '业务线', 10, 1, 1),
  ('biz_line', 'HOTEL', '酒店', '业务线', 20, 1, 1),
  ('biz_line', 'TRAIN_TICKET', '火车票', '业务线', 30, 1, 1),
  ('biz_line_api', 'check_good_seat', '查询好坐席', '业务线接口', 10, 1, 1),
  ('biz_line_api', 'query_user_info', '查询用户信息', '业务线接口', 20, 1, 1),
  ('biz_line_api', 'query_order_detail', '查询订单详情', '业务线接口', 30, 1, 1),
  ('behavior_strategy_type', 'BROWSE_DURATION', '浏览时长', '行为策略类型', 10, 1, 1),
  ('behavior_strategy_type', 'BROWSE_COUNT', '浏览次数', '行为策略类型', 20, 1, 1),
  ('behavior_strategy_type', 'CLICK_COUNT', '点击次数', '行为策略类型', 30, 1, 1),
  ('message_code_in_app', 'international_hotel_coupon_popup', '国际酒店领券弹窗', '端内消息编码', 10, 1, 1),
  ('message_code_in_app', 'flight_coupon_banner', '机票优惠Banner', '端内消息编码', 20, 1, 1),
  ('message_code_mq', 'ivr_project', 'IVR项目消息', 'MQ 消息编码', 10, 1, 1),
  ('message_code_mq', 'reward_notify', '奖励通知消息', 'MQ 消息编码', 20, 1, 1),
  ('mq_topic_legacy', 'flight_order_status_change', '机票订单状态变化', 'MQ 主题兼容选项', 10, 1, 1),
  ('mq_topic_legacy', 'hotel_order_status_change', '酒店订单状态变化', 'MQ 主题兼容选项', 20, 1, 1),
  ('mq_topic_legacy', 'train_order_status_change', '火车票订单状态变化', 'MQ 主题兼容选项', 30, 1, 1),
  ('canvas_trigger_type', 'REALTIME', '实时触发', '画布触发类型', 10, 1, 1),
  ('canvas_trigger_type', 'SCHEDULED', '定时触发', '画布触发类型', 20, 1, 1),
  ('start_trigger_type', 'DIRECT', '手动直调', 'START 节点触发方式', 10, 1, 1),
  ('start_trigger_type', 'EVENT', '事件触发', 'START 节点触发方式', 20, 1, 1),
  ('start_trigger_type', 'SCHEDULED', '定时触发', 'START 节点触发方式', 30, 1, 1),
  ('start_trigger_type', 'MQ', 'MQ消息', 'START 节点触发方式', 40, 1, 1),
  ('behavior_trigger_type', 'inapp', '端内行为事件（监听 MQ）', '行为触发方式', 10, 1, 1),
  ('behavior_trigger_type', 'direct', '业务直调（HTTP 推送）', '行为触发方式', 20, 1, 1)
ON DUPLICATE KEY UPDATE
  label = VALUES(label),
  description = VALUES(description),
  sort_order = VALUES(sort_order),
  system_builtin = VALUES(system_builtin);

INSERT INTO ab_experiment_group (experiment_id, group_key, label, sort_order, enabled)
SELECT id, 'A', 'A组', 10, 1 FROM ab_experiment
ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order);

INSERT INTO ab_experiment_group (experiment_id, group_key, label, sort_order, enabled)
SELECT id, 'B', 'B组', 20, 1 FROM ab_experiment
ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order);

UPDATE node_type_registry
SET config_schema = JSON_REMOVE(JSON_SET(config_schema, '$[0].optionCategory', 'schedule_type'), '$[0].options')
WHERE type_key = 'SCHEDULED_TRIGGER';

UPDATE node_type_registry
SET config_schema = JSON_REMOVE(JSON_SET(config_schema, '$[2].optionCategory', 'approval_timeout_action'), '$[2].options')
WHERE type_key = 'MANUAL_APPROVAL';

UPDATE node_type_registry
SET config_schema = JSON_REMOVE(JSON_SET(config_schema, '$[1].optionCategory', 'canvas_invoke_mode'), '$[1].options')
WHERE type_key = 'CANVAS_TRIGGER';

UPDATE node_type_registry
SET config_schema = JSON_REMOVE(JSON_SET(config_schema, '$[0].optionCategory', 'logic_relation'), '$[0].options')
WHERE type_key = 'LOGIC_RELATION';

UPDATE node_type_registry
SET config_schema = JSON_REMOVE(JSON_SET(config_schema, '$[0].optionCategory', 'direct_return_build_type'), '$[0].options')
WHERE type_key = 'DIRECT_RETURN';

UPDATE node_type_registry
SET config_schema = JSON_REMOVE(JSON_SET(config_schema, '$[0].optionCategory', 'tagger_mode'), '$[0].options')
WHERE type_key = 'TAGGER';

UPDATE node_type_registry
SET config_schema = JSON_REMOVE(JSON_SET(config_schema, '$[0].optionCategory', 'threshold_mode'), '$[0].options')
WHERE type_key = 'THRESHOLD';

UPDATE node_type_registry
SET config_schema = JSON_REMOVE(JSON_SET(config_schema, '$[0].optionCategory', 'aggregate_evaluate_mode'), '$[0].options')
WHERE type_key = 'AGGREGATE';

UPDATE node_type_registry
SET config_schema = JSON_REMOVE(JSON_SET(config_schema, '$[1].optionCategory', 'delay_unit'), '$[1].options')
WHERE type_key = 'DELAY'
  AND JSON_UNQUOTE(JSON_EXTRACT(config_schema, '$[1].key')) = 'unit';

UPDATE node_type_registry
SET config_schema = JSON_REMOVE(JSON_SET(config_schema, '$[0].optionCategory', 'start_trigger_type'), '$[0].options')
WHERE type_key = 'START';

UPDATE node_type_registry
SET config_schema = JSON_REMOVE(JSON_SET(config_schema, '$[0].optionCategory', 'behavior_trigger_type'), '$[0].options')
WHERE type_key = 'BEHAVIOR_TRIGGER';

UPDATE node_type_registry
SET config_schema = JSON_REMOVE(JSON_SET(config_schema, '$[0].optionCategory', 'logic_relation'), '$[0].options')
WHERE type_key = 'BEHAVIOR_STRATEGY';
