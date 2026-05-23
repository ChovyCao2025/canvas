# 组合套路

## 实时事件触发

结构：`START -> EVENT_TRIGGER -> IF_CONDITION -> ACTION -> END`
适合：下单、支付、注册、激活后的即时运营。
关键点：事件字段要和条件规则一致，失败分支必须连到兜底动作或 `END`。
模板：`component_event_if_coupon`、`ecommerce_new_user_coupon`、`travel_flight_delay_care`。

## MQ 消息校验路由

结构：`START -> MQ_TRIGGER -> IF_CONDITION -> SEND_MQ -> END`
适合：业务系统只通过 MQ 通知画布，且需要校验消息状态后再通知下游。
关键点：`topicKey` 决定消费主题，`validateRules` 做入流前校验。
模板：`component_mq_validate_route`。

## 定时批量运营

结构：`START -> SCHEDULED_TRIGGER -> TAGGER(audience) -> REACH_PLATFORM -> END`
适合：每日促活、还款提醒、沉睡用户召回、会员纪念日。
关键点：Cron、时区、人群 ID 三者要一起确认，避免误触达。
模板：`component_scheduled_audience_push`、`fintech_loan_repay_reminder`、`local_service_reactivation`。

## 延迟二次触达

结构：`EVENT_TRIGGER -> DELAY -> REACH_PLATFORM -> END`
适合：加购未支付、试听课后跟进、注册后未完成资料。
关键点：延迟节点要和业务动作的有效期匹配，例如券有效期、订单支付窗口。
模板：`component_delay_followup`、`ecommerce_cart_recall`、`education_course_followup`。

## 多分支人群运营

结构：`SELECTOR` 或 `PRIORITY` 分层，再连接不同权益或触达。
适合：会员等级、线索评分、客户生命周期运营。
关键点：分支顺序就是优先级，最后要配置兜底分支。
模板：`component_selector_multi_branch`、`component_priority_offer`、`fintech_wealth_cross_sell`。

## 实验分流

结构：`AB_SPLIT -> A/B 触达 -> END`
适合：文案、渠道、权益金额、落地页的对照实验。
关键点：实验组 key 要稳定，避免用户跨次触达分组漂移。
模板：`component_ab_split_compare`、`content_subscription_trial`。

## 并行评估

结构：`API_CALL A/B -> AGGREGATE 或 THRESHOLD -> SEND_MQ`
适合：多渠道投票、风控信号聚合、任一信号达标即触发。
关键点：`AGGREGATE` 等全部上游完成，`THRESHOLD` 更适合快速通过或快速拦截。
模板：`component_hub_wait_all`、`component_aggregate_kpi`、`component_threshold_fast_win`、`b2b_lead_scoring`。

## 人工审批保护

结构：`IF_CONDITION -> MANUAL_APPROVAL -> SEND_MQ 或 COUPON`
适合：金融风控、大额权益、异常交易复核。
关键点：审批超时时间、拒绝分支、审批人列表必须有业务兜底。
模板：`component_manual_approval`、`fintech_risk_review`。

## 同步直调返回

结构：`DIRECT_CALL -> API_CALL -> DIRECT_RETURN`
适合：业务系统需要同步拿到画布决策结果。
关键点：返回字段要来自上下文中确定存在的字段，避免同步接口返回空值。
模板：`component_direct_call_return`、`fintech_card_activation`。

## 子流程复用

结构：`CANVAS_TRIGGER -> SUB_FLOW_REF -> END`
适合：把用户资格校验、风控检查、统一通知等公共逻辑抽成可复用流程。
关键点：父流程引用的子流程必须是已发布版本。
模板：`component_sub_flow_ref`。
