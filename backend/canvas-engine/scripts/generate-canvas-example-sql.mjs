import fs from 'node:fs'
import path from 'node:path'

const out = path.resolve('src/main/resources/db/migration/V50__canvas_example_templates.sql')

const templates = [
  ['component_event_if_coupon', '组件教学', '拉新转化', '入门', '示例：事件触发新客领券', 'EVENT_TRIGGER,IF_CONDITION,COUPON', 'event_if_coupon'],
  ['component_mq_validate_route', '组件教学', '消息触发', '入门', '示例：MQ 消息校验后路由', 'MQ_TRIGGER,IF_CONDITION,SEND_MQ', 'mq_if_mq'],
  ['component_scheduled_audience_push', '组件教学', '定时运营', '入门', '示例：定时人群 Push', 'SCHEDULED_TRIGGER,TAGGER,REACH_PLATFORM', 'schedule_audience_reach'],
  ['component_direct_call_return', '组件教学', '直调返回', '进阶', '示例：直调查询并同步返回', 'DIRECT_CALL,API_CALL,DIRECT_RETURN', 'direct_api_return'],
  ['component_selector_multi_branch', '组件教学', '多分支路由', '进阶', '示例：条件选择器多分支', 'SELECTOR,IN_APP_NOTIFY', 'selector_notify'],
  ['component_priority_offer', '组件教学', '权益匹配', '进阶', '示例：优先级权益匹配', 'PRIORITY,COUPON', 'priority_coupon'],
  ['component_ab_split_compare', '组件教学', '实验分流', '入门', '示例：AB 分流触达实验', 'AB_SPLIT,IN_APP_NOTIFY,REACH_PLATFORM', 'ab_touch'],
  ['component_hub_wait_all', '组件教学', '并行等待', '复杂', '示例：集线器等待并行完成', 'HUB,API_CALL', 'hub_parallel'],
  ['component_aggregate_kpi', '组件教学', '聚合评估', '复杂', '示例：聚合评估成功率', 'AGGREGATE,IF_CONDITION', 'aggregate'],
  ['component_threshold_fast_win', '组件教学', '快速决策', '复杂', '示例：阈值触发快速决策', 'THRESHOLD,SEND_MQ', 'threshold'],
  ['component_logic_relation', '组件教学', '逻辑组合', '进阶', '示例：逻辑关系组合判断', 'LOGIC_RELATION,IF_CONDITION', 'logic_relation'],
  ['component_manual_approval', '组件教学', '人工审批', '进阶', '示例：人工审批后发券', 'MANUAL_APPROVAL,COUPON', 'approval_coupon'],
  ['component_delay_followup', '组件教学', '延迟触达', '入门', '示例：延迟二次触达', 'DELAY,REACH_PLATFORM', 'delay_reach'],
  ['component_groovy_transform', '组件教学', '字段加工', '复杂', '示例：Groovy 字段加工', 'GROOVY,API_CALL', 'groovy_api'],
  ['component_tagger_offline', '组件教学', '离线标签', '入门', '示例：离线标签判断', 'TAGGER,IF_CONDITION', 'tagger_offline'],
  ['component_tagger_realtime', '组件教学', '实时标签', '进阶', '示例：实时标签触发流程', 'TAGGER,IN_APP_NOTIFY', 'tagger_realtime'],
  ['component_sub_flow_ref', '组件教学', '子流程复用', '复杂', '示例：子流程引用', 'SUB_FLOW_REF,CANVAS_TRIGGER', 'subflow'],
  ['component_send_mq_receipt', '组件教学', '消息通知', '入门', '示例：发送 MQ 通知下游', 'SEND_MQ,END', 'send_mq'],
  ['ecommerce_new_user_coupon', '电商', '拉新转化', '入门', '示例：新客首单券发放', 'EVENT_TRIGGER,IF_CONDITION,COUPON', 'event_if_coupon'],
  ['ecommerce_cart_recall', '电商', '弃购召回', '入门', '示例：加购未支付召回', 'EVENT_TRIGGER,DELAY,REACH_PLATFORM', 'delay_reach'],
  ['ecommerce_vip_tier_offer', '电商', '会员运营', '进阶', '示例：会员等级差异化权益', 'TAGGER,PRIORITY,COUPON', 'priority_coupon'],
  ['ecommerce_cross_sell', '电商', '交叉销售', '进阶', '示例：订单完成后关联推荐', 'EVENT_TRIGGER,API_CALL,IN_APP_NOTIFY', 'event_api_notify'],
  ['travel_flight_delay_care', '出行', '服务关怀', '进阶', '示例：航班延误补偿触达', 'EVENT_TRIGGER,IF_CONDITION,COUPON,REACH_PLATFORM', 'event_if_coupon_reach'],
  ['travel_hotel_bundle', '出行', '复购提升', '入门', '示例：机票成交后酒店联售', 'EVENT_TRIGGER,DELAY,REACH_PLATFORM', 'delay_reach'],
  ['travel_high_value_route', '出行', '高价值客户', '进阶', '示例：高价值用户专属活动', 'TAGGER,IF_CONDITION,COUPON', 'tagger_audience_coupon'],
  ['travel_pre_departure_reminder', '出行', '行前提醒', '入门', '示例：出行前多渠道提醒', 'SCHEDULED_TRIGGER,REACH_PLATFORM,SEND_MQ', 'schedule_reach_mq'],
  ['fintech_card_activation', '金融', '激活转化', '入门', '示例：信用卡开卡激活', 'DIRECT_CALL,API_CALL,REACH_PLATFORM', 'direct_api_reach'],
  ['fintech_risk_review', '金融', '风控拦截', '复杂', '示例：大额交易人工复核', 'EVENT_TRIGGER,IF_CONDITION,MANUAL_APPROVAL', 'risk_approval'],
  ['fintech_loan_repay_reminder', '金融', '还款提醒', '入门', '示例：贷款还款分层提醒', 'SCHEDULED_TRIGGER,TAGGER,REACH_PLATFORM', 'schedule_audience_reach'],
  ['fintech_wealth_cross_sell', '金融', '交叉销售', '进阶', '示例：理财产品适配推荐', 'TAGGER,SELECTOR,IN_APP_NOTIFY', 'selector_notify'],
  ['saas_trial_nurture', 'SaaS', '试用转化', '进阶', '示例：试用期行为培育', 'EVENT_TRIGGER,DELAY,IN_APP_NOTIFY', 'delay_reach'],
  ['saas_onboarding_steps', 'SaaS', '新手引导', '入门', '示例：新账号上手路径', 'DIRECT_CALL,SELECTOR,IN_APP_NOTIFY', 'selector_notify'],
  ['saas_churn_risk_save', 'SaaS', '流失挽回', '进阶', '示例：低活跃客户挽留', 'SCHEDULED_TRIGGER,TAGGER,COUPON,REACH_PLATFORM', 'tagger_audience_coupon'],
  ['saas_expansion_signal', 'SaaS', '增购扩容', '复杂', '示例：高用量客户扩容推荐', 'API_CALL,AGGREGATE,SEND_MQ', 'aggregate'],
  ['local_food_coupon', '本地生活', '到店转化', '入门', '示例：餐饮券包发放', 'TAGGER,COUPON,REACH_PLATFORM', 'tagger_audience_coupon'],
  ['local_service_reactivation', '本地生活', '沉睡召回', '入门', '示例：本地服务沉睡用户召回', 'SCHEDULED_TRIGGER,TAGGER,REACH_PLATFORM', 'schedule_audience_reach'],
  ['local_weather_push', '本地生活', '场景营销', '进阶', '示例：天气触发即时权益', 'API_CALL,IF_CONDITION,COUPON', 'event_if_coupon'],
  ['retail_store_lbs', '零售', '到店引流', '进阶', '示例：门店附近用户触达', 'EVENT_TRIGGER,TAGGER,REACH_PLATFORM', 'tagger_audience_coupon'],
  ['retail_inventory_clearance', '零售', '清仓促销', '入门', '示例：库存清仓定向触达', 'SCHEDULED_TRIGGER,TAGGER,IN_APP_NOTIFY', 'schedule_audience_reach'],
  ['retail_member_anniversary', '零售', '会员纪念日', '入门', '示例：会员周年礼', 'SCHEDULED_TRIGGER,COUPON,IN_APP_NOTIFY', 'event_if_coupon'],
  ['content_subscription_trial', '内容平台', '订阅转化', '进阶', '示例：内容试读后订阅转化', 'EVENT_TRIGGER,AB_SPLIT,REACH_PLATFORM', 'ab_touch'],
  ['content_inactive_reader', '内容平台', '活跃提升', '入门', '示例：沉默读者唤醒', 'SCHEDULED_TRIGGER,TAGGER,IN_APP_NOTIFY', 'schedule_audience_reach'],
  ['gaming_level_reward', '游戏', '成长激励', '入门', '示例：等级达成奖励', 'EVENT_TRIGGER,COUPON,IN_APP_NOTIFY', 'event_if_coupon'],
  ['gaming_lost_user_winback', '游戏', '回流召回', '进阶', '示例：流失玩家回流礼包', 'SCHEDULED_TRIGGER,TAGGER,COUPON', 'tagger_audience_coupon'],
  ['education_course_followup', '教育', '课程转化', '入门', '示例：试听课后跟进', 'EVENT_TRIGGER,DELAY,REACH_PLATFORM', 'delay_reach'],
  ['education_learning_reminder', '教育', '学习促活', '入门', '示例：学习计划提醒', 'SCHEDULED_TRIGGER,REACH_PLATFORM,END', 'schedule_reach_mq'],
  ['b2b_lead_scoring', 'B2B', '线索培育', '复杂', '示例：线索评分后分配', 'API_CALL,THRESHOLD,SEND_MQ', 'threshold'],
  ['logistics_delivery_care', '物流', '服务通知', '进阶', '示例：异常配送关怀', 'EVENT_TRIGGER,IF_CONDITION,REACH_PLATFORM', 'event_if_coupon_reach'],
]

function node(id, type, name, category, x, y, config = {}) {
  return { id, type, name, category, x, y, config, bizConfig: config }
}

const graphs = {
  event_if_coupon: () => [
    node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'event' }),
    node('event', 'EVENT_TRIGGER', '订单完成事件', '行为策略', 400, 140, { eventCode: 'ORDER_COMPLETE', nextNodeId: 'if_new' }),
    node('if_new', 'IF_CONDITION', '是否目标用户', '逻辑分支', 400, 280, { rules: [{ field: 'isNewUser', operator: 'EQ', value: 'true' }], successNodeId: 'coupon', failNodeId: 'end' }),
    node('coupon', 'COUPON', '发放权益券', '权益发放', 180, 420, { couponTypeKey: 'flight_coupon', nextNodeId: 'end' }),
    node('end', 'END', '结束', '流程控制', 400, 560, {}),
  ],
  mq_if_mq: () => [
    node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'mq' }),
    node('mq', 'MQ_TRIGGER', '订单状态 MQ', '行为策略', 400, 140, { topicKey: 'CANVAS_MQ_TRIGGER', validateResult: true, validateRules: [{ field: 'orderStatus', operator: 'EQ', value: 'PAID' }], nextNodeId: 'if_paid' }),
    node('if_paid', 'IF_CONDITION', '是否支付成功', '逻辑分支', 400, 280, { rules: [{ field: 'orderStatus', operator: 'EQ', value: 'PAID' }], successNodeId: 'send_mq', failNodeId: 'end' }),
    node('send_mq', 'SEND_MQ', '通知下游系统', '其他', 180, 420, { messageCodeKey: 'order_paid_notice', nextNodeId: 'end' }),
    node('end', 'END', '结束', '流程控制', 400, 560, {}),
  ],
  schedule_audience_reach: () => [
    node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'schedule' }),
    node('schedule', 'SCHEDULED_TRIGGER', '每日定时触发', '行为策略', 400, 140, { scheduleType: 'CRON', cronExpression: '0 0 9 * * ?', timezone: 'Asia/Shanghai', nextNodeId: 'audience' }),
    node('audience', 'TAGGER', '判断目标人群', '人群圈选', 400, 280, { mode: 'audience', audienceId: 90001, hitNextNodeId: 'reach', missNextNodeId: 'end' }),
    node('reach', 'REACH_PLATFORM', '发送营销触达', '用户触达', 180, 420, { serviceSceneKey: 'promo_push', nextNodeId: 'end' }),
    node('end', 'END', '结束', '流程控制', 400, 560, {}),
  ],
  direct_api_return: () => [
    node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'direct' }),
    node('direct', 'DIRECT_CALL', '业务直调触发', '行为策略', 400, 140, { eventCode: 'ORDER_COMPLETE', nextNodeId: 'api' }),
    node('api', 'API_CALL', '查询用户信息', '其他', 400, 280, { apiKey: 'query_user_info', outputPrefix: 'user', nextNodeId: 'return' }),
    node('return', 'DIRECT_RETURN', '同步返回结果', '用户触达', 400, 420, { buildType: 'CUSTOM', data: [{ key: 'userLevel', value: '${user.level}' }] }),
  ],
  selector_notify: () => [
    node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'selector' }),
    node('selector', 'SELECTOR', '按用户分层选择', '逻辑分支', 400, 160, { branches: [{ label: '高价值', rules: [{ field: 'score', operator: 'GT', value: '80' }], nextNodeId: 'notify_a' }, { label: '普通', rules: [{ field: 'score', operator: 'GTE', value: '40' }], nextNodeId: 'notify_b' }], elseNodeId: 'notify_c' }),
    node('notify_a', 'IN_APP_NOTIFY', '高价值消息', '用户触达', 120, 340, { messageCodeKey: 'vip_message', nextNodeId: 'end' }),
    node('notify_b', 'IN_APP_NOTIFY', '普通消息', '用户触达', 400, 340, { messageCodeKey: 'normal_message', nextNodeId: 'end' }),
    node('notify_c', 'IN_APP_NOTIFY', '兜底消息', '用户触达', 680, 340, { messageCodeKey: 'fallback_message', nextNodeId: 'end' }),
    node('end', 'END', '结束', '流程控制', 400, 520, {}),
  ],
  priority_coupon: () => [
    node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'priority' }),
    node('priority', 'PRIORITY', '权益优先级', '逻辑分支', 400, 160, { priorities: [{ order: 1, nextNodeId: 'coupon_a' }, { order: 2, nextNodeId: 'coupon_b' }], nextNodeId: 'end' }),
    node('coupon_a', 'COUPON', '高价值券', '权益发放', 180, 340, { couponTypeKey: 'vip_coupon', nextNodeId: 'end' }),
    node('coupon_b', 'COUPON', '普通券', '权益发放', 620, 340, { couponTypeKey: 'flight_coupon', nextNodeId: 'end' }),
    node('end', 'END', '结束', '流程控制', 400, 520, {}),
  ],
  ab_touch: () => [
    node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'ab' }),
    node('ab', 'AB_SPLIT', 'AB 分流', '人群圈选', 400, 160, { experimentKey: 'exp_push_campaign', groups: [{ groupKey: 'A', nextNodeId: 'push_a' }, { groupKey: 'B', nextNodeId: 'push_b' }] }),
    node('push_a', 'IN_APP_NOTIFY', 'A 组站内信', '用户触达', 200, 340, { messageCodeKey: 'flight_promo_push', nextNodeId: 'end' }),
    node('push_b', 'REACH_PLATFORM', 'B 组触达平台', '用户触达', 600, 340, { serviceSceneKey: 'promo_push', nextNodeId: 'end' }),
    node('end', 'END', '结束', '流程控制', 400, 520, {}),
  ],
  delay_reach: () => [
    node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'event' }),
    node('event', 'EVENT_TRIGGER', '行为事件', '行为策略', 400, 140, { eventCode: 'ORDER_COMPLETE', nextNodeId: 'delay' }),
    node('delay', 'DELAY', '等待 30 分钟', '其他', 400, 280, { duration: 30, unit: 'MINUTE', nextNodeId: 'reach' }),
    node('reach', 'REACH_PLATFORM', '二次触达', '用户触达', 400, 420, { serviceSceneKey: 'promo_push', nextNodeId: 'end' }),
    node('end', 'END', '结束', '流程控制', 400, 560, {}),
  ],
}

graphs.event_api_notify = () => [
  node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'event' }),
  node('event', 'EVENT_TRIGGER', '订单完成事件', '行为策略', 400, 140, { eventCode: 'ORDER_COMPLETE', nextNodeId: 'api' }),
  node('api', 'API_CALL', '查询推荐内容', '其他', 400, 280, { apiKey: 'query_user_info', outputPrefix: 'rec', nextNodeId: 'notify' }),
  node('notify', 'IN_APP_NOTIFY', '发送推荐消息', '用户触达', 400, 420, { messageCodeKey: 'flight_promo_push', nextNodeId: 'end' }),
  node('end', 'END', '结束', '流程控制', 400, 560, {}),
]

graphs.event_if_coupon_reach = () => {
  const nodes = graphs.event_if_coupon()
  nodes.splice(4, 0, node('reach', 'REACH_PLATFORM', '补偿触达', '用户触达', 180, 560, { serviceSceneKey: 'promo_push', nextNodeId: 'end' }))
  nodes.find(n => n.id === 'coupon').config.nextNodeId = 'reach'
  nodes.find(n => n.id === 'coupon').bizConfig.nextNodeId = 'reach'
  nodes.find(n => n.id === 'end').y = 700
  return nodes
}

graphs.schedule_reach_mq = () => [
  node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'schedule' }),
  node('schedule', 'SCHEDULED_TRIGGER', '定时触发', '行为策略', 400, 140, { scheduleType: 'CRON', cronExpression: '0 0 9 * * ?', timezone: 'Asia/Shanghai', nextNodeId: 'reach' }),
  node('reach', 'REACH_PLATFORM', '发送提醒', '用户触达', 400, 280, { serviceSceneKey: 'promo_push', nextNodeId: 'send_mq' }),
  node('send_mq', 'SEND_MQ', '通知业务系统', '其他', 400, 420, { messageCodeKey: 'reminder_sent', nextNodeId: 'end' }),
  node('end', 'END', '结束', '流程控制', 400, 560, {}),
]

graphs.direct_api_reach = () => [
  node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'direct' }),
  node('direct', 'DIRECT_CALL', '业务直调', '行为策略', 400, 140, { eventCode: 'USER_ACTIVE', nextNodeId: 'api' }),
  node('api', 'API_CALL', '查询状态', '其他', 400, 280, { apiKey: 'query_user_info', outputPrefix: 'user', nextNodeId: 'reach' }),
  node('reach', 'REACH_PLATFORM', '发送激活提醒', '用户触达', 400, 420, { serviceSceneKey: 'promo_push', nextNodeId: 'end' }),
  node('end', 'END', '结束', '流程控制', 400, 560, {}),
]

graphs.risk_approval = () => [
  node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'event' }),
  node('event', 'EVENT_TRIGGER', '交易事件', '行为策略', 400, 140, { eventCode: 'ORDER_COMPLETE', nextNodeId: 'if_risk' }),
  node('if_risk', 'IF_CONDITION', '是否大额风险', '逻辑分支', 400, 280, { rules: [{ field: 'amount', operator: 'GT', value: '1000' }], successNodeId: 'approval', failNodeId: 'end' }),
  node('approval', 'MANUAL_APPROVAL', '人工复核', '其他', 180, 420, { approvers: ['risk_ops'], timeoutHours: 24, onTimeout: 'REJECT', approveNodeId: 'send_mq', rejectNodeId: 'end' }),
  node('send_mq', 'SEND_MQ', '通知通过结果', '其他', 180, 560, { messageCodeKey: 'risk_passed', nextNodeId: 'end' }),
  node('end', 'END', '结束', '流程控制', 400, 700, {}),
]

graphs.tagger_audience_coupon = () => [
  node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'audience' }),
  node('audience', 'TAGGER', '圈选目标人群', '人群圈选', 400, 160, { mode: 'audience', audienceId: 90001, hitNextNodeId: 'coupon', missNextNodeId: 'end' }),
  node('coupon', 'COUPON', '发放定向权益', '权益发放', 200, 340, { couponTypeKey: 'flight_coupon', nextNodeId: 'reach' }),
  node('reach', 'REACH_PLATFORM', '权益到账通知', '用户触达', 200, 500, { serviceSceneKey: 'promo_push', nextNodeId: 'end' }),
  node('end', 'END', '结束', '流程控制', 400, 660, {}),
]

graphs.aggregate = () => [
  node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'api_a' }),
  node('api_a', 'API_CALL', '渠道 A 调用', '其他', 180, 160, { apiKey: 'query_user_info', nextNodeId: 'aggregate' }),
  node('api_b', 'API_CALL', '渠道 B 调用', '其他', 620, 160, { apiKey: 'query_user_info', nextNodeId: 'aggregate' }),
  node('aggregate', 'AGGREGATE', '聚合评估成功率', '逻辑分支', 400, 340, { evaluateMode: 'rate', minRate: 50, successNodeId: 'send_mq', failNodeId: 'end' }),
  node('send_mq', 'SEND_MQ', '同步成功结论', '其他', 200, 500, { messageCodeKey: 'aggregate_passed', nextNodeId: 'end' }),
  node('end', 'END', '结束', '流程控制', 400, 660, {}),
]

graphs.threshold = () => [
  node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'api_a' }),
  node('api_a', 'API_CALL', '信号 A', '其他', 180, 160, { apiKey: 'query_user_info', nextNodeId: 'threshold' }),
  node('api_b', 'API_CALL', '信号 B', '其他', 620, 160, { apiKey: 'query_user_info', nextNodeId: 'threshold' }),
  node('threshold', 'THRESHOLD', '达到阈值即触发', '逻辑分支', 400, 340, { thresholdMode: 'min_success', threshold: 1, successNodeId: 'send_mq', failNodeId: 'end' }),
  node('send_mq', 'SEND_MQ', '通知达标结果', '其他', 200, 500, { messageCodeKey: 'threshold_passed', nextNodeId: 'end' }),
  node('end', 'END', '结束', '流程控制', 400, 660, {}),
]

graphs.hub_parallel = () => [
  node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'api_a' }),
  node('api_a', 'API_CALL', '并行任务 A', '其他', 180, 160, { apiKey: 'query_user_info', nextNodeId: 'hub' }),
  node('api_b', 'API_CALL', '并行任务 B', '其他', 620, 160, { apiKey: 'query_user_info', nextNodeId: 'hub' }),
  node('hub', 'HUB', '等待并行完成', '逻辑分支', 400, 340, { timeout: 600, nextNodeId: 'send_mq' }),
  node('send_mq', 'SEND_MQ', '同步完成结果', '其他', 400, 500, { messageCodeKey: 'hub_done', nextNodeId: 'end' }),
  node('end', 'END', '结束', '流程控制', 400, 660, {}),
]

graphs.logic_relation = () => [
  node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'api_a' }),
  node('api_a', 'API_CALL', '条件来源 A', '其他', 180, 160, { apiKey: 'query_user_info', nextNodeId: 'logic' }),
  node('api_b', 'API_CALL', '条件来源 B', '其他', 620, 160, { apiKey: 'query_user_info', nextNodeId: 'logic' }),
  node('logic', 'LOGIC_RELATION', 'AND 逻辑关系', '逻辑分支', 400, 340, { relation: 'AND', nextNodeId: 'if_passed' }),
  node('if_passed', 'IF_CONDITION', '是否满足组合条件', '逻辑分支', 400, 500, { rules: [{ field: 'logicPassed', operator: 'EQ', value: 'true' }], successNodeId: 'send_mq', failNodeId: 'end' }),
  node('send_mq', 'SEND_MQ', '通知满足条件', '其他', 200, 660, { messageCodeKey: 'logic_passed', nextNodeId: 'end' }),
  node('end', 'END', '结束', '流程控制', 400, 820, {}),
]

graphs.approval_coupon = () => [
  node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'approval' }),
  node('approval', 'MANUAL_APPROVAL', '人工审批', '其他', 400, 160, { approvers: ['ops_owner'], timeoutHours: 24, onTimeout: 'REJECT', approveNodeId: 'coupon', rejectNodeId: 'end' }),
  node('coupon', 'COUPON', '审批通过发券', '权益发放', 180, 340, { couponTypeKey: 'flight_coupon', nextNodeId: 'end' }),
  node('end', 'END', '结束', '流程控制', 400, 520, {}),
]

graphs.groovy_api = () => [
  node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'groovy' }),
  node('groovy', 'GROOVY', '加工字段', '其他', 400, 160, { inputParams: [], code: 'return [score: 88]', outputParams: [{ key: 'score', dataType: 'NUMBER' }], nextNodeId: 'api' }),
  node('api', 'API_CALL', '提交加工结果', '其他', 400, 320, { apiKey: 'query_user_info', outputPrefix: 'api', nextNodeId: 'end' }),
  node('end', 'END', '结束', '流程控制', 400, 480, {}),
]

graphs.tagger_offline = () => [
  node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'tagger' }),
  node('tagger', 'TAGGER', '读取离线标签', '人群圈选', 400, 160, { mode: 'offline', tagCodeKey: 'market_identity', nextNodeId: 'if_tag' }),
  node('if_tag', 'IF_CONDITION', '是否目标标签', '逻辑分支', 400, 320, { rules: [{ field: 'tagValue', operator: 'EQ', value: 'VIP' }], successNodeId: 'notify', failNodeId: 'end' }),
  node('notify', 'IN_APP_NOTIFY', '发送标签消息', '用户触达', 180, 480, { messageCodeKey: 'vip_message', nextNodeId: 'end' }),
  node('end', 'END', '结束', '流程控制', 400, 640, {}),
]

graphs.tagger_realtime = () => [
  node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'tagger' }),
  node('tagger', 'TAGGER', '实时标签触发', '行为策略', 400, 160, { mode: 'realtime', tagCodeKey: 'high_value_user', nextNodeId: 'notify' }),
  node('notify', 'IN_APP_NOTIFY', '实时标签消息', '用户触达', 400, 320, { messageCodeKey: 'vip_message', nextNodeId: 'end' }),
  node('end', 'END', '结束', '流程控制', 400, 480, {}),
]

graphs.subflow = () => [
  node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'canvas_trigger' }),
  node('canvas_trigger', 'CANVAS_TRIGGER', '触发子画布', '其他', 400, 160, { targetCanvasId: 1, invokeMode: 'ASYNC', nextNodeId: 'subflow' }),
  node('subflow', 'SUB_FLOW_REF', '引用标准子流程', '其他', 400, 320, { subFlowId: 1, subFlowVersion: -1, outputPrefix: 'sub', nextNodeId: 'end' }),
  node('end', 'END', '结束', '流程控制', 400, 480, {}),
]

graphs.send_mq = () => [
  node('start', 'START', '开始', '流程控制', 400, 0, { nextNodeId: 'send_mq' }),
  node('send_mq', 'SEND_MQ', '发送业务消息', '其他', 400, 160, { messageCodeKey: 'example_notice', nextNodeId: 'end' }),
  node('end', 'END', '结束', '流程控制', 400, 320, {}),
]

function sqlString(value) {
  return `'${String(value).replaceAll('\\', '\\\\').replaceAll("'", "''")}'`
}

const rows = templates.map((item, index) => {
  const [key, companyType, scenario, difficulty, name, covered, pattern] = item
  const graph = { nodes: graphs[pattern]().map(n => ({ ...n, bizConfig: n.config })) }
  const description = `${companyType} / ${scenario} 示例，展示 ${covered} 的配置和组合方式。`
  return `(${[
    sqlString(key),
    sqlString(name),
    sqlString(description),
    sqlString(scenario),
    sqlString(JSON.stringify(graph)),
    'NULL',
    '1',
    '0',
    sqlString('example-seed'),
    'NOW()',
    sqlString(companyType),
    sqlString(scenario),
    sqlString(difficulty),
    sqlString(covered),
    String(index + 1),
    '1',
  ].join(', ')})`
})

const sql = `-- V50: official canvas example templates.

INSERT INTO canvas_template
  (template_key, name, description, category, graph_json, thumbnail,
   is_official, use_count, created_by, created_at,
   company_type, marketing_scenario, difficulty, covered_node_types,
   sort_order, enabled)
VALUES
${rows.join(',\n')}
ON DUPLICATE KEY UPDATE
  template_key = VALUES(template_key),
  name = VALUES(name),
  description = VALUES(description),
  category = VALUES(category),
  graph_json = VALUES(graph_json),
  thumbnail = VALUES(thumbnail),
  is_official = VALUES(is_official),
  created_by = VALUES(created_by),
  company_type = VALUES(company_type),
  marketing_scenario = VALUES(marketing_scenario),
  difficulty = VALUES(difficulty),
  covered_node_types = VALUES(covered_node_types),
  sort_order = VALUES(sort_order),
  enabled = VALUES(enabled);
`

fs.mkdirSync(path.dirname(out), { recursive: true })
fs.writeFileSync(out, sql)
console.log(`Wrote ${templates.length} canvas example templates to ${out}`)
