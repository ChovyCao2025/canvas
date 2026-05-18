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

    // ── 控制流节点 ────────────────────────────────────────────────────

    /** 条件分支（if/else） */
    public static final String IF_CONDITION       = "IF_CONDITION";
    /** 优先级路由（多分支按优先级选一） */
    public static final String PRIORITY           = "PRIORITY";
    /** A/B 分流 */
    public static final String AB_SPLIT           = "AB_SPLIT";
    /** 汇聚节点（等待多条上游汇入一条） */
    public static final String HUB                = "HUB";
    /** 逻辑关系聚合（AND / OR，等待多条上游满足条件） */
    public static final String LOGIC_RELATION     = "LOGIC_RELATION";
    /** 选择器（从多个候选项中选一执行） */
    public static final String SELECTOR           = "SELECTOR";
    /** 人工审批节点（挂起执行，等待人工操作） */
    public static final String MANUAL_APPROVAL    = "MANUAL_APPROVAL";
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
    /** Groovy 脚本节点 */
    public static final String GROOVY             = "GROOVY";
    /** 实时打标签 */
    public static final String TAGGER_REALTIME    = "TAGGER_REALTIME";
    /** 离线打标签 */
    public static final String TAGGER_OFFLINE     = "TAGGER_OFFLINE";
    /** 通用打标（tagger 合并版） */
    public static final String TAGGER             = "TAGGER";

    private NodeType() {}
}
