package org.chovy.canvas.domain.risk.governance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.RiskSceneDO;
import org.chovy.canvas.dal.mapper.RiskSceneMapper;
import org.chovy.canvas.domain.risk.governance.RiskSceneService.StateStore;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * JDBC-backed 风控场景目录仓储，使租户场景可治理、可恢复。
 */
public class JdbcRiskSceneStore implements StateStore {

    private final RiskSceneMapper mapper;
    private final Clock clock;

    /**
     * 创建使用系统 UTC 时钟的场景仓储。
     */
    public JdbcRiskSceneStore(RiskSceneMapper mapper) {
        this(mapper, Clock.systemUTC());
    }

    /**
     * 创建可注入时钟的场景仓储。
     */
    public JdbcRiskSceneStore(RiskSceneMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    @Override
    public List<RiskSceneView> listScenes(Long tenantId) {
        List<RiskSceneView> persisted = load(tenantId);
        if (!persisted.isEmpty()) {
            return persisted;
        }
        List<RiskSceneView> defaults = RiskSceneService.defaultScenes(tenantId);
        defaults.forEach(scene -> mapper.insert(toRow(scene)));
        return defaults;
    }

    private List<RiskSceneView> load(Long tenantId) {
        return mapper.selectList(new LambdaQueryWrapper<RiskSceneDO>()
                        .eq(RiskSceneDO::getTenantId, tenantId)
                        .eq(RiskSceneDO::getStatus, "ACTIVE")
                        .orderByAsc(RiskSceneDO::getId))
                .stream()
                .map(this::toView)
                .toList();
    }

    private RiskSceneDO toRow(RiskSceneView view) {
        LocalDateTime now = LocalDateTime.now(clock);
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
}
