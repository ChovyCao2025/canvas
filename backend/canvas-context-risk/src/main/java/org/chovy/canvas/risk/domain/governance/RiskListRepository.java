package org.chovy.canvas.risk.domain.governance;

import java.util.List;

import org.chovy.canvas.risk.api.RiskListView;

/**
 * 定义 RiskListRepository 的风控模块职责和数据契约。
 */
public interface RiskListRepository {

    /**
     * 执行 listLists 相关的风控处理逻辑。
     */
    List<RiskListView> listLists(Long tenantId);
}
