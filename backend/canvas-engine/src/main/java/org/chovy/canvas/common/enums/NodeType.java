package org.chovy.canvas.common.enums;

/**
 * Governed canvas node type keys.
 *
 * <p>This list is intentionally small because the current node catalog has not
 * been released. Duplicated, placeholder, or unsupported node types should be
 * deleted before launch instead of preserved as public compatibility surface.
 */
public final class NodeType {

    // 基础控制
    public static final String START = "START";
    public static final String END = "END";
    public static final String DIRECT_RETURN = "DIRECT_RETURN";

    // 入口触发
    public static final String DIRECT_CALL = "DIRECT_CALL";
    public static final String EVENT_TRIGGER = "EVENT_TRIGGER";
    public static final String MQ_TRIGGER = "MQ_TRIGGER";
    public static final String SCHEDULED_TRIGGER = "SCHEDULED_TRIGGER";

    // 条件与分流
    public static final String IF_CONDITION = "IF_CONDITION";
    public static final String SPLIT = "SPLIT";

    // 等待与汇聚
    public static final String WAIT = "WAIT";
    public static final String HUB = "HUB";
    public static final String AGGREGATE = "AGGREGATE";
    public static final String THRESHOLD = "THRESHOLD";

    // 动作执行
    public static final String API_CALL = "API_CALL";
    public static final String SEND_MQ = "SEND_MQ";
    public static final String GROOVY = "GROOVY";

    // 消息触达
    public static final String SEND_MESSAGE = "SEND_MESSAGE";

    // 数据与权益
    public static final String TAGGER = "TAGGER";
    public static final String COMMIT_ACTION = "COMMIT_ACTION";

    // 流程复用
    public static final String SUB_FLOW_REF = "SUB_FLOW_REF";

    private NodeType() {
    }
}
