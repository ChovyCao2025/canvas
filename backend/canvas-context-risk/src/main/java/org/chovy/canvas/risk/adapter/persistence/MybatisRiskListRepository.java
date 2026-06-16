package org.chovy.canvas.risk.adapter.persistence;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.risk.api.RiskListView;
import org.chovy.canvas.risk.domain.governance.RiskListRepository;
import org.springframework.stereotype.Repository;

/**
 * 定义 MybatisRiskListRepository 的风控模块职责和数据契约。
 */
@Repository
public class MybatisRiskListRepository implements RiskListRepository {

    /**
     * 保存 mapper 对应的风控状态或配置。
     */
    private final RiskListMapper mapper;

    public MybatisRiskListRepository(RiskListMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 执行 listLists 相关的风控处理逻辑。
     */
    @Override
    public List<RiskListView> listLists(Long tenantId) {
        return mapper.selectList(new LambdaQueryWrapper<RiskListDO>()
                        .eq(RiskListDO::getTenantId, tenantId)
                        .orderByAsc(RiskListDO::getListKey))
                .stream()
                .map(this::toView)
                .toList();
    }

    /**
     * 执行 toView 相关的风控处理逻辑。
     */
    private RiskListView toView(RiskListDO row) {
        return new RiskListView(
                row.getTenantId(),
                row.getListKey(),
                row.getListType(),
                row.getSubjectType(),
                row.getStatus(),
                Boolean.TRUE.equals(row.getRequiresApproval()),
                row.getOwner());
    }
}
