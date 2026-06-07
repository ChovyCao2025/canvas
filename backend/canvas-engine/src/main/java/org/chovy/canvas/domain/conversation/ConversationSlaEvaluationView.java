package org.chovy.canvas.domain.conversation;

import java.util.List;

public record ConversationSlaEvaluationView(Long tenantId,
                                            int scanned,
                                            int created,
                                            int skippedExisting,
                                            List<ConversationSlaBreachView> breaches) {

    public ConversationSlaEvaluationView {
        breaches = breaches == null ? List.of() : List.copyOf(breaches);
    }
}
