package org.chovy.canvas.domain.constant;

/** canvas_execution.trigger_type：记录本次执行由哪种方式触发。 */
public final class TriggerType {

    public static final String EVENT                   = "EVENT";
    public static final String MQ                      = "MQ";
    public static final String DIRECT_CALL             = "DIRECT_CALL";
    public static final String SCHEDULED               = "SCHEDULED";
    public static final String DRY_RUN                 = "DRY_RUN";
    public static final String DLQ_REPLAY              = "DLQ_REPLAY";
    public static final String MANUAL_APPROVAL_TIMEOUT = "MANUAL_APPROVAL_TIMEOUT";
    public static final String HUB_TIMEOUT             = "HUB_TIMEOUT";
    public static final String LOGIC_RELATION_TIMEOUT  = "LOGIC_RELATION_TIMEOUT";
    public static final String AGGREGATE_TIMEOUT       = "AGGREGATE_TIMEOUT";
    public static final String THRESHOLD_TIMEOUT       = "THRESHOLD_TIMEOUT";
    public static final String WAIT_RESUME             = "WAIT_RESUME";
    public static final String WAIT_TIMEOUT            = "WAIT_TIMEOUT";
    public static final String GOAL_CHECK_RESUME       = "GOAL_CHECK_RESUME";
    public static final String GOAL_CHECK_TIMEOUT      = "GOAL_CHECK_TIMEOUT";
    public static final String TRANSFER_JOURNEY        = "TRANSFER_JOURNEY";
    public static final String SUB_FLOW_REF            = "SUB_FLOW_REF";

    private TriggerType() {}
}
