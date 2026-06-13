package org.chovy.canvas.risk.adapter.persistence;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.risk.api.RiskListView;
import org.chovy.canvas.risk.domain.governance.RiskListRepository;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisRiskListRepository implements RiskListRepository {

    private final RiskListMapper mapper;

    public MybatisRiskListRepository(RiskListMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<RiskListView> listLists(Long tenantId) {
        return mapper.selectList(new LambdaQueryWrapper<RiskListDO>()
                        .eq(RiskListDO::getTenantId, tenantId)
                        .orderByAsc(RiskListDO::getListKey))
                .stream()
                .map(this::toView)
                .toList();
    }

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
