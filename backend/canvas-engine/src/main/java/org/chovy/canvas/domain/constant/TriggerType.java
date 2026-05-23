package org.chovy.canvas.domain.constant;

/** canvas_execution.trigger_type：记录本次执行由哪种方式触发。 */
public final class TriggerType {

    /** 事件上报触发。 */
    public static final String EVENT                   = "EVENT";

    /** MQ 消息触发。 */
    public static final String MQ                      = "MQ";

    /** 外部系统同步直调触发。 */
    public static final String DIRECT_CALL             = "DIRECT_CALL";

    /** 定时调度触发。 */
    public static final String SCHEDULED               = "SCHEDULED";

    /** 预演/调试触发。 */
    public static final String DRY_RUN                 = "DRY_RUN";

    /** 死信回放触发。 */
    public static final String DLQ_REPLAY              = "DLQ_REPLAY";

    /** 人工审批超时兜底触发。 */
    public static final String MANUAL_APPROVAL_TIMEOUT = "MANUAL_APPROVAL_TIMEOUT";

    /** HUB 节点超时兜底触发。 */
    public static final String HUB_TIMEOUT             = "HUB_TIMEOUT";

    /** 逻辑关系节点超时兜底触发。 */
    public static final String LOGIC_RELATION_TIMEOUT  = "LOGIC_RELATION_TIMEOUT";

    /** 聚合节点超时兜底触发。 */
    public static final String AGGREGATE_TIMEOUT       = "AGGREGATE_TIMEOUT";

    /** 阈值节点超时兜底触发。 */
    public static final String THRESHOLD_TIMEOUT       = "THRESHOLD_TIMEOUT";

    /** 子流程引用节点触发。 */
    public static final String SUB_FLOW_REF            = "SUB_FLOW_REF";

    private TriggerType() {}
}
