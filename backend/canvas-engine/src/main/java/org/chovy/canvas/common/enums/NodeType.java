package org.chovy.canvas.common.enums;

/**
 * 受治理的画布节点类型键。
 *
 * <p>当前节点目录尚未正式发布，因此只保留已经支持的节点类型；重复、占位或未支持类型应在发布前删除，
 * 不作为公开兼容面保留。
 */
public final class NodeType {

    // 基础控制
    public static final String START = "START";
    public static final String END = "END";
    public static final String DIRECT_RETURN = "DIRECT_RETURN";
    public static final String LOOP = "LOOP";

    // 入口触发
    public static final String DIRECT_CALL = "DIRECT_CALL";
    public static final String EVENT_TRIGGER = "EVENT_TRIGGER";
    public static final String MQ_TRIGGER = "MQ_TRIGGER";
    public static final String SCHEDULED_TRIGGER = "SCHEDULED_TRIGGER";

    // 条件与分流
    public static final String IF_CONDITION = "IF_CONDITION";
    public static final String LOGIC_RELATION = "LOGIC_RELATION";
    public static final String SPLIT = "SPLIT";

    // 等待与汇聚
    public static final String WAIT = "WAIT";
    public static final String USER_INPUT = "USER_INPUT";
    public static final String MANUAL_APPROVAL = "MANUAL_APPROVAL";
    public static final String HUB = "HUB";
    public static final String AGGREGATE = "AGGREGATE";
    public static final String THRESHOLD = "THRESHOLD";

    // 动作执行
    public static final String API_CALL = "API_CALL";
    public static final String SEND_MQ = "SEND_MQ";
    public static final String GROOVY = "GROOVY";

    // 消息触达
    public static final String SEND_MESSAGE = "SEND_MESSAGE";

    // AI 智能
    public static final String AI_LLM = "AI_LLM";

    // 数据与权益
    public static final String TAGGER = "TAGGER";
    public static final String COMMIT_ACTION = "COMMIT_ACTION";
    public static final String RISK_DECISION = "RISK_DECISION";

    // 流程复用
    public static final String SUB_FLOW_REF = "SUB_FLOW_REF";
    public static final String TRANSFER_JOURNEY = "TRANSFER_JOURNEY";

    /**
     * 工具类禁止实例化。
     */
    private NodeType() {
    }
}
