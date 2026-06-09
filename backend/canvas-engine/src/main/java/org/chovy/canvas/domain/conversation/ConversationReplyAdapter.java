package org.chovy.canvas.domain.conversation;

/**
 * ConversationReplyAdapter 定义 domain.conversation 场景中的扩展契约。
 */
public interface ConversationReplyAdapter<T> {

    /**
     * 执行 adapterKey 流程，围绕 adapter key 完成校验、计算或结果组装。
     *
     * @return 返回 adapter key 生成的文本或业务键。
     */
    default String adapterKey() {
        return getClass().getSimpleName()
                .replace("ConversationReplyAdapter", "")
                .replace("ConversationAdapter", "")
                .trim()
                .toUpperCase();
    }

    /**
     * 执行 payloadType 流程，围绕 payload type 完成校验、计算或结果组装。
     *
     * @return 返回 payloadType 流程生成的业务结果。
     */
    default Class<?> payloadType() {
        return Object.class;
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回组装或转换后的结果对象。
     */
    ConversationIngressReq toIngress(T payload, ConversationAdapterContext context);
}
