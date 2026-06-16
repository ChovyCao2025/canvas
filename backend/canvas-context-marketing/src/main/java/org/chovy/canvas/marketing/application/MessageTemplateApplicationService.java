package org.chovy.canvas.marketing.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.MessageTemplateFacade;
import org.chovy.canvas.marketing.domain.MessageTemplateCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 编排MessageTemplate相关的应用层用例。
 */
@Service
public class MessageTemplateApplicationService implements MessageTemplateFacade {

    private final MessageTemplateCatalog catalog = new MessageTemplateCatalog();

    /**
     * 执行search业务操作。
     */
    @Override
    public List<TemplateView> search(Long tenantId, String keyword, String channel) {
        return catalog.search(safeTenantId(tenantId), keyword, channel);
    }

    /**
     * 创建业务对象。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TemplateView create(Long tenantId, String actor, TemplateDraft draft) {
        return catalog.create(safeTenantId(tenantId), actorOrDefault(actor), draft);
    }

    /**
     * 执行preview业务操作。
     */
    @Override
    public PreviewView preview(Long tenantId, String templateCode, Map<String, Object> context) {
        return catalog.preview(safeTenantId(tenantId), templateCode, context == null ? Map.of() : context);
    }

    /**
     * 执行safeTenantId业务操作。
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    /**
     * 执行actorOrDefault业务操作。
     */
    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }
}
