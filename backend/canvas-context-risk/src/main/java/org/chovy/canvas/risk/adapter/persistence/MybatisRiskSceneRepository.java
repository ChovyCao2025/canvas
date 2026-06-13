package org.chovy.canvas.risk.adapter.persistence;

import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.risk.api.RiskSceneView;
import org.chovy.canvas.risk.domain.governance.RiskSceneRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisRiskSceneRepository implements RiskSceneRepository {

    private final RiskSceneMapper mapper;

    public MybatisRiskSceneRepository(RiskSceneMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<RiskSceneView> listScenes(Long tenantId) {
        return mapper.selectList(new LambdaQueryWrapper<RiskSceneDO>()
                        .eq(RiskSceneDO::getTenantId, tenantId)
                        .eq(RiskSceneDO::getStatus, "ACTIVE")
                        .orderByAsc(RiskSceneDO::getId))
                .stream()
                .map(this::toView)
                .toList();
    }

    @Override
    public void saveAll(List<RiskSceneView> scenes) {
        scenes.stream()
                .map(this::toRow)
                .forEach(this::insertIfAbsent);
    }

    private void insertIfAbsent(RiskSceneDO row) {
        try {
            mapper.insert(row);
        } catch (DuplicateKeyException ignored) {
            // Another first-read request may have seeded this tenant scene concurrently.
        }
    }

    private RiskSceneView toView(RiskSceneDO row) {
        return new RiskSceneView(
                row.getTenantId(),
                row.getSceneKey(),
                row.getName(),
                row.getEventSchemaKey(),
                row.getStatus(),
                row.getDefaultMode(),
                row.getFailPolicy(),
                row.getLatencyBudgetMs(),
                row.getOwner());
    }

    private RiskSceneDO toRow(RiskSceneView view) {
        LocalDateTime now = LocalDateTime.now();
        RiskSceneDO row = new RiskSceneDO();
        row.setTenantId(view.tenantId());
        row.setSceneKey(view.sceneKey());
        row.setName(view.displayName());
        row.setEventSchemaKey(view.eventSchemaKey());
        row.setStatus(view.status());
        row.setDefaultMode(view.defaultMode());
        row.setFailPolicy(view.failPolicy());
        row.setLatencyBudgetMs(view.latencyBudgetMs());
        row.setOwner(view.owner());
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        return row;
    }
}
