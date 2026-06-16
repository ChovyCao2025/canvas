package org.chovy.canvas.risk.api;

import java.util.List;

/**
 * 定义 RiskListFacade 的风控模块职责和数据契约。
 */
public interface RiskListFacade {

    /**
     * 执行 listLists 相关的风控处理逻辑。
     */
    List<RiskListView> listLists(Long tenantId);
}
