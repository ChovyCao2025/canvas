package org.chovy.canvas.domain.conversation;

import java.util.List;

/**
 * ConversationSlaEvaluationView 承载 domain.conversation 场景中的不可变数据快照。
 * @param tenantId tenantId 字段。
 * @param scanned scanned 字段。
 * @param created created 字段。
 * @param skippedExisting skippedExisting 字段。
 * @param breaches breaches 字段。
 */
public record ConversationSlaEvaluationView(Long tenantId,
                                            int scanned,
                                            int created,
                                            int skippedExisting,
                                            List<ConversationSlaBreachView> breaches) {

    public ConversationSlaEvaluationView {
        breaches = breaches == null ? List.of() : List.copyOf(breaches);
    }
}
