package org.chovy.canvas.domain.constant;

/**
 * 画布节点类型键（对应 node_type_registry.type_key 及 DAG node.type 字段）。
 */
public final class NodeType {

    // ── 触发器节点 ────────────────────────────────────────────────────

    /** 流程入口节点，每个画布必须有且仅有一个，无业务配置 */
    public static final String START              = "START";
    /** 事件触发节点，配置 eventCode 订阅业务事件 */
    public static final String EVENT_TRIGGER      = "EVENT_TRIGGER";
    /** MQ 消息触发节点 */
    public static final String MQ_TRIGGER         = "MQ_TRIGGER";
    /** 定时触发节点 */
    public static final String SCHEDULED_TRIGGER  = "SCHEDULED_TRIGGER";
    /** 业务直调触发节点 */
    public static final String DIRECT_CALL        = "DIRECT_CALL";
    /** 子画布触发节点（被父画布调用时的入口） */
    public static final String CANVAS_TRIGGER     = "CANVAS_TRIGGER";
    /** 产品化 API 触发节点 */
    public static final String API_TRIGGER        = "API_TRIGGER";
    /** 受众进入/离开触发节点 */
    public static final String AUDIENCE_TRIGGER   = "AUDIENCE_TRIGGER";

    // ── 控制流节点 ────────────────────────────────────────────────────

    /** 条件分支（if/else） */
    public static final String IF_CONDITION       = "IF_CONDITION";
    /** 优先级路由（多分支按优先级选一） */
    public static final String PRIORITY           = "PRIORITY";
    /** A/B 分流 */
    public static final String AB_SPLIT           = "AB_SPLIT";
    /** 随机分流 */
    public static final String RANDOM_SPLIT       = "RANDOM_SPLIT";
    /** 实验节点 */
    public static final String EXPERIMENT         = "EXPERIMENT";
    /** 评分节点 */
    public static final String SCORING            = "SCORING";
    /** 推荐节点 */
    public static final String RECOMMENDATION     = "RECOMMENDATION";
    /** AI 下一步最佳动作 */
    public static final String AI_NEXT_BEST_ACTION = "AI_NEXT_BEST_ACTION";
    /** 汇聚节点（等待多条上游汇入一条） */
    public static final String HUB                = "HUB";
    /** 聚合评估节点：等待所有上游完成，基于上游结果评估条件，路由到成功或失败分支 */
    public static final String AGGREGATE          = "AGGREGATE";
    /**
     * 阈值触发节点：不等所有上游完成，每个上游完成都触发一次评估。
     * 达到阈值（如成功数≥K）立刻路由，无需等待其余上游。
     * 这是 repeat 机制真正有语义价值的节点类型。
     */
    public static final String THRESHOLD          = "THRESHOLD";
    /** 逻辑关系聚合（AND / OR，等待多条上游满足条件） */
    public static final String LOGIC_RELATION     = "LOGIC_RELATION";
    /** 选择器（从多个候选项中选一执行） */
    public static final String SELECTOR           = "SELECTOR";
    /** 人工审批节点（挂起执行，等待人工操作） */
    public static final String MANUAL_APPROVAL    = "MANUAL_APPROVAL";
    /** 增强等待节点 */
    public static final String WAIT               = "WAIT";
    /** 目标检测节点 */
    public static final String GOAL_CHECK         = "GOAL_CHECK";
    /** 营销抑制/授权检查 */
    public static final String SUPPRESSION_CHECK  = "SUPPRESSION_CHECK";
    /** 静默时段检查 */
    public static final String QUIET_HOURS        = "QUIET_HOURS";
    /** 渠道可达性检查 */
    public static final String CHANNEL_AVAILABILITY = "CHANNEL_AVAILABILITY";
    /** 频率限制检查 */
    public static final String FREQUENCY_CAP      = "FREQUENCY_CAP";
    /** 延时节点 */
    public static final String DELAY              = "DELAY";
    /** 流程终止节点 */
    public static final String END                = "END";
    /** 直接返回节点（立即结束当前路径） */
    public static final String DIRECT_RETURN      = "DIRECT_RETURN";

    // ── 业务动作节点 ──────────────────────────────────────────────────

    /** 子流程引用（嵌套调用另一个画布） */
    public static final String SUB_FLOW_REF       = "SUB_FLOW_REF";
    /** 发送应用内通知 */
    public static final String IN_APP_NOTIFY      = "IN_APP_NOTIFY";
    /** 发送 MQ 消息 */
    public static final String SEND_MQ            = "SEND_MQ";
    /** HTTP API 调用 */
    public static final String API_CALL           = "API_CALL";
    /** 优惠券发放 */
    public static final String COUPON             = "COUPON";
    /** 触达平台（短信/Push 等） */
    public static final String REACH_PLATFORM     = "REACH_PLATFORM";
    /** 产品化邮件触达 */
    public static final String SEND_EMAIL         = "SEND_EMAIL";
    /** 产品化短信触达 */
    public static final String SEND_SMS           = "SEND_SMS";
    /** 产品化 Push 触达 */
    public static final String SEND_PUSH          = "SEND_PUSH";
    /** 产品化站内信触达 */
    public static final String SEND_IN_APP        = "SEND_IN_APP";
    /** 产品化微信触达 */
    public static final String SEND_WECHAT        = "SEND_WECHAT";
    /** Groovy 脚本节点 */
    public static final String GROOVY             = "GROOVY";
    /** 实时打标签 */
    public static final String TAGGER_REALTIME    = "TAGGER_REALTIME";
    /** 离线打标签 */
    public static final String TAGGER_OFFLINE     = "TAGGER_OFFLINE";
    /** 通用打标（tagger 合并版） */
    public static final String TAGGER             = "TAGGER";
    /** 更新用户属性 */
    public static final String UPDATE_PROFILE     = "UPDATE_PROFILE";
    /** 本地标签操作 */
    public static final String TAG_OPERATION      = "TAG_OPERATION";
    /** 积分操作 */
    public static final String POINTS_OPERATION   = "POINTS_OPERATION";
    /** 创建人工任务 */
    public static final String CREATE_TASK        = "CREATE_TASK";
    /** 记录事件 */
    public static final String TRACK_EVENT        = "TRACK_EVENT";

    private NodeType() {}
}
