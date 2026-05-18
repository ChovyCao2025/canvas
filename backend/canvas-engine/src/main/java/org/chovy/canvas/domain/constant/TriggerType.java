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
    public static final String SUB_FLOW_REF            = "SUB_FLOW_REF";

    private TriggerType() {}
}
