package org.chovy.canvas.common.enums;

/** canvas_execution.trigger_type: records how an execution is started or resumed. */
public final class TriggerType {

    public static final String EVENT = "EVENT";
    public static final String MQ = "MQ";
    public static final String DIRECT_CALL = "DIRECT_CALL";
    public static final String SCHEDULED = "SCHEDULED";
    public static final String DRY_RUN = "DRY_RUN";
    public static final String DLQ_REPLAY = "DLQ_REPLAY";

    public static final String HUB_TIMEOUT = "HUB_TIMEOUT";
    public static final String AGGREGATE_TIMEOUT = "AGGREGATE_TIMEOUT";
    public static final String THRESHOLD_TIMEOUT = "THRESHOLD_TIMEOUT";
    public static final String WAIT_RESUME = "WAIT_RESUME";
    public static final String WAIT_TIMEOUT = "WAIT_TIMEOUT";
    public static final String APPROVAL_RESUME = "APPROVAL_RESUME";
    public static final String APPROVAL_TIMEOUT = "APPROVAL_TIMEOUT";
    public static final String SUB_FLOW_REF = "SUB_FLOW_REF";
    public static final String TRANSFER_JOURNEY = "TRANSFER_JOURNEY";

    /**
     * 初始化 TriggerType 实例。
     */
    private TriggerType() {
    }
}
