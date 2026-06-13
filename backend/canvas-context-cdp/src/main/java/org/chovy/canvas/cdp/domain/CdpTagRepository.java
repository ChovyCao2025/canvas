package org.chovy.canvas.cdp.domain;

import java.util.List;

public interface CdpTagRepository {

    CdpTagDefinition findEnabledDefinition(String tagCode);

    CdpUserTag findCurrentTag(Long tenantId, String userId, String tagCode);

    boolean saveHistory(CdpUserTagHistory row);

    CdpUserTag saveCurrentTag(CdpUserTag tag);

    List<CdpUserTag> listCurrentTags(Long tenantId, String userId);

    List<CdpUserTagHistory> listHistory(Long tenantId, String userId);
}
