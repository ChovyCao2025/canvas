package org.chovy.canvas.domain.content;

import org.chovy.canvas.dal.dataobject.MarketingContentTemplateDO;

final class ContentTemplateServiceTestRows {

    private ContentTemplateServiceTestRows() {
    }

    static MarketingContentTemplateDO template(String key, String channel, String subject, String body) {
        MarketingContentTemplateDO row = new MarketingContentTemplateDO();
        row.setTenantId(8L);
        row.setTemplateKey(key);
        row.setDisplayName("Welcome");
        row.setChannel(channel);
        row.setSubject(subject);
        row.setBody(body);
        row.setDesignJson("{}");
        row.setAssetRefsJson("[]");
        row.setVariablesJson("[\"firstName\",\"couponCode\"]");
        row.setStatus("DRAFT");
        row.setCreatedBy("operator-1");
        return row;
    }
}
