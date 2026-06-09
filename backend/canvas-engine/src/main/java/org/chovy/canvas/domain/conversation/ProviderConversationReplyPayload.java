package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * ProviderConversationReplyPayload 定义 domain.conversation 场景中的扩展契约。
 */
public interface ProviderConversationReplyPayload {

    /**
     * 判断业务条件是否成立。
     *
     * @return 返回布尔判断结果。
     */
    Long canvasId();

    /**
     * 执行 versionId 流程，围绕 version id 完成校验、计算或结果组装。
     *
     * @return 返回 version id 计算得到的数量、金额或指标值。
     */
    Long versionId();

    /**
     * 执行 executionId 流程，围绕 execution id 完成校验、计算或结果组装。
     *
     * @return 返回 execution id 生成的文本或业务键。
     */
    String executionId();

    /**
     * 执行 userId 流程，围绕 user id 完成校验、计算或结果组装。
     *
     * @return 返回 user id 生成的文本或业务键。
     */
    String userId();

    /**
     * 执行 provider 流程，围绕 provider 完成校验、计算或结果组装。
     *
     * @return 返回 provider 生成的文本或业务键。
     */
    String provider();

    /**
     * 执行 externalMessageId 流程，围绕 external message id 完成校验、计算或结果组装。
     *
     * @return 返回 external message id 生成的文本或业务键。
     */
    String externalMessageId();

    /**
     * 执行 eventId 流程，围绕 event id 完成校验、计算或结果组装。
     *
     * @return 返回 event id 生成的文本或业务键。
     */
    String eventId();

    /**
     * 执行 text 流程，围绕 text 完成校验、计算或结果组装。
     *
     * @return 返回 text 生成的文本或业务键。
     */
    String text();

    /**
     * 执行 intent 流程，围绕 intent 完成校验、计算或结果组装。
     *
     * @return 返回 intent 生成的文本或业务键。
     */
    String intent();

    /**
     * 执行 attributes 流程，围绕 attributes 完成校验、计算或结果组装。
     *
     * @return 返回 attributes 流程生成的业务结果。
     */
    Map<String, Object> attributes();

    /**
     * 执行 occurredAt 流程，围绕 occurred at 完成校验、计算或结果组装。
     *
     * @return 返回 occurredAt 流程生成的业务结果。
     */
    LocalDateTime occurredAt();
}
