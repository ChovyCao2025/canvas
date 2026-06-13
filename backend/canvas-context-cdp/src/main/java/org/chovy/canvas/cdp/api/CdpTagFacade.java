package org.chovy.canvas.cdp.api;

import java.util.List;

public interface CdpTagFacade {

    CdpUserTagView setTag(Long tenantId, String userId, CdpTagWriteCommand command);

    void removeTag(Long tenantId, String userId, String tagCode, String reason, String operator);

    List<CdpUserTagView> listCurrentTags(Long tenantId, String userId);

    List<CdpUserTagHistoryView> listHistory(Long tenantId, String userId);
}
