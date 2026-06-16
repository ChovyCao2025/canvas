package org.chovy.canvas.risk.adapter.persistence;

import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.risk.api.RiskSceneView;
import org.chovy.canvas.risk.domain.governance.RiskSceneRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

/**
 * 定义 MybatisRiskSceneRepository 的风控模块职责和数据契约。
 */
@Repository
public class MybatisRiskSceneRepository implements RiskSceneRepository {

    /**
     * 保存 mapper 对应的风控状态或配置。
     */
    private final RiskSceneMapper mapper;

    public MybatisRiskSceneRepository(RiskSceneMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 执行 listScenes 相关的风控处理逻辑。
     */
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

    /**
     * 执行 saveAll 相关的风控处理逻辑。
     */
    @Override
    public void saveAll(List<RiskSceneView> scenes) {
        scenes.stream()
                .map(this::toRow)
                .forEach(this::insertIfAbsent);
    }

    /**
     * 执行 insertIfAbsent 相关的风控处理逻辑。
     */
    private void insertIfAbsent(RiskSceneDO row) {
        try {
            mapper.insert(row);
        } catch (DuplicateKeyException ignored) {
            // Another first-read request may have seeded this tenant scene concurrently.
        }
    }

    /**
     * 执行 toView 相关的风控处理逻辑。
     */
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

    /**
     * 执行 toRow 相关的风控处理逻辑。
     */
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
