package org.chovy.canvas.cdp.api;

import java.util.List;

/**
 * 定义 CdpTagFacade 对外暴露的 CDP 业务能力。
 */
public interface CdpTagFacade {

    /**
     * 设置tag。
     */
    CdpUserTagView setTag(Long tenantId, String userId, CdpTagWriteCommand command);

    /**
     * 执行 removeTag 对应的 CDP 业务操作。
     */
    void removeTag(Long tenantId, String userId, String tagCode, String reason, String operator);

    /**
     * 查询Current Tags列表。
     */
    List<CdpUserTagView> listCurrentTags(Long tenantId, String userId);

    /**
     * 查询History列表。
     */
    List<CdpUserTagHistoryView> listHistory(Long tenantId, String userId);
}
