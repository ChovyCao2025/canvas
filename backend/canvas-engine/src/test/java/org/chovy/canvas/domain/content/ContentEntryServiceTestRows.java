package org.chovy.canvas.domain.content;

import org.chovy.canvas.dal.dataobject.MarketingContentEntryDO;

final class ContentEntryServiceTestRows {

    private ContentEntryServiceTestRows() {
    }

    static MarketingContentEntryDO entry(String key, String status) {
        MarketingContentEntryDO row = new MarketingContentEntryDO();
        row.setTenantId(8L);
        row.setEntryKey(key);
        row.setContentType("LANDING_PAGE");
        row.setTitle("Summer Landing");
        row.setSlug("summer-sale");
        row.setLocale("en-US");
        row.setSummary("Season offer");
        row.setBodyJson("{\"blocks\":[]}");
        row.setSeoJson("{}");
        row.setAssetRefsJson("[]");
        row.setStatus(status);
        row.setCreatedBy("operator-1");
        return row;
    }
}
