package org.chovy.canvas.marketing.api;

import java.util.List;
import java.util.Map;

public interface MessageTemplateFacade {

    List<TemplateView> search(Long tenantId, String keyword, String channel);

    TemplateView create(Long tenantId, String actor, TemplateDraft draft);

    PreviewView preview(Long tenantId, String templateCode, Map<String, Object> context);

    record TemplateDraft(String templateCode, String displayName, String channel, String body) {
    }

    record TemplateView(
            Long tenantId,
            String templateCode,
            String displayName,
            String channel,
            String body,
            List<String> variables,
            String status,
            String createdBy) {
    }

    record PreviewView(String renderedBody, List<String> missingVariables) {
    }
}
