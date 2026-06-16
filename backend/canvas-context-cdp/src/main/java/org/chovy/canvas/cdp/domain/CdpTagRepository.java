package org.chovy.canvas.cdp.domain;

import java.util.List;

/**
 * 定义 CdpTag 的持久化访问契约。
 */
public interface CdpTagRepository {

    /**
     * 查找Enabled Definition。
     */
    CdpTagDefinition findEnabledDefinition(String tagCode);

    /**
     * 查找Current Tag。
     */
    CdpUserTag findCurrentTag(Long tenantId, String userId, String tagCode);

    /**
     * 保存History。
     */
    boolean saveHistory(CdpUserTagHistory row);

    /**
     * 保存Current Tag。
     */
    CdpUserTag saveCurrentTag(CdpUserTag tag);

    /**
     * 查询Current Tags列表。
     */
    List<CdpUserTag> listCurrentTags(Long tenantId, String userId);

    /**
     * 查询History列表。
     */
    List<CdpUserTagHistory> listHistory(Long tenantId, String userId);
}
