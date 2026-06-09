package org.chovy.canvas.domain.conversation;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SandboxConversationReplyAdapter 编排 domain.conversation 场景的领域业务规则。
 */
@Component
public class SandboxConversationReplyAdapter implements ConversationReplyAdapter<SandboxConversationReplyPayload> {

    /**
     * adapterKey 处理 domain.conversation 场景的业务逻辑。
     * @return 返回 adapter key 生成的文本或业务键。
     */
    @Override
    public String adapterKey() {
        return "SANDBOX";
    }

    /**
     * payloadType 处理 domain.conversation 场景的业务逻辑。
     * @return 返回 payloadType 流程生成的业务结果。
     */
    @Override
    public Class<?> payloadType() {
        return SandboxConversationReplyPayload.class;
    }

    /**
     * toIngress 校验或转换 domain.conversation 场景的数据。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回组装或转换后的结果对象。
     */
    @Override
    public ConversationIngressReq toIngress(SandboxConversationReplyPayload payload, ConversationAdapterContext context) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (payload == null) {
            throw new IllegalArgumentException("sandbox conversation reply payload is required");
        }
        Map<String, Object> attributes = new LinkedHashMap<>();
        if (payload.attributes() != null) {
            attributes.putAll(payload.attributes());
        }
        attributes.put("adapter", "SANDBOX");
        if (context != null && context.operator() != null && !context.operator().isBlank()) {
            attributes.put("sandboxOperator", context.operator().trim());
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new ConversationIngressReq(
                payload.canvasId(),
                payload.versionId(),
                payload.executionId(),
                payload.userId(),
                "SANDBOX",
                "DEFAULT",
                payload.externalMessageId(),
                payload.eventId(),
                "TEXT",
                payload.text(),
                payload.intent(),
                attributes,
                null);
    }
}
