package org.chovy.canvas.marketing.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.MessageTemplateFacade;
import org.chovy.canvas.marketing.domain.MessageTemplateCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageTemplateApplicationService implements MessageTemplateFacade {

    private final MessageTemplateCatalog catalog = new MessageTemplateCatalog();

    @Override
    public List<TemplateView> search(Long tenantId, String keyword, String channel) {
        return catalog.search(safeTenantId(tenantId), keyword, channel);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TemplateView create(Long tenantId, String actor, TemplateDraft draft) {
        return catalog.create(safeTenantId(tenantId), actorOrDefault(actor), draft);
    }

    @Override
    public PreviewView preview(Long tenantId, String templateCode, Map<String, Object> context) {
        return catalog.preview(safeTenantId(tenantId), templateCode, context == null ? Map.of() : context);
    }

    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }
}
