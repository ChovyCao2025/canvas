package org.chovy.canvas.risk.application;

import java.util.List;
import java.util.Objects;

import org.chovy.canvas.risk.api.RiskListFacade;
import org.chovy.canvas.risk.api.RiskListView;
import org.chovy.canvas.risk.domain.governance.RiskListRepository;
import org.springframework.stereotype.Service;

/**
 * 定义 RiskListApplicationService 的风控模块职责和数据契约。
 */
@Service
public class RiskListApplicationService implements RiskListFacade {

    /**
     * 保存 repository 对应的风控状态或配置。
     */
    private final RiskListRepository repository;

    public RiskListApplicationService(RiskListRepository repository) {
        this.repository = repository;
    }

    /**
     * 执行 listLists 相关的风控处理逻辑。
     */
    @Override
    public List<RiskListView> listLists(Long tenantId) {
        Objects.requireNonNull(tenantId, "tenantId");
        return repository.listLists(tenantId);
    }
}
