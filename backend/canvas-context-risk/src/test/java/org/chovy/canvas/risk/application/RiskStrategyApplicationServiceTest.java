package org.chovy.canvas.risk.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.chovy.canvas.risk.adapter.persistence.MybatisRiskStrategyRepository;
import org.chovy.canvas.risk.adapter.persistence.RiskStrategyDO;
import org.chovy.canvas.risk.adapter.persistence.RiskStrategyMapper;
import org.chovy.canvas.risk.api.RiskStrategyView;
import org.chovy.canvas.risk.domain.governance.RiskStrategyRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.Objects;

/**
 * 定义 RiskStrategyApplicationServiceTest 的风控模块职责和数据契约。
 */
class RiskStrategyApplicationServiceTest {

    /**
     * 执行 initMyBatisPlusTableInfo 相关的风控处理逻辑。
     */
    @BeforeAll
    static void initMyBatisPlusTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), RiskStrategyDO.class);
    }

    /**
     * 执行 listStrategiesRejectsNullTenantId 相关的风控处理逻辑。
     */
    @Test
    void listStrategiesRejectsNullTenantId() {
        RiskStrategyApplicationService service = new RiskStrategyApplicationService(
                new FakeRepository(List.of(strategy(42L, "scene_a", "alpha"))));

        assertThatNullPointerException()
                .isThrownBy(() -> service.listStrategies(null, "scene_a"))
                .withMessage("tenantId");
    }

    /**
     * 执行 listStrategiesPassesNullBlankAndExactSceneKeyToRepository 相关的风控处理逻辑。
     */
    @Test
    void listStrategiesPassesNullBlankAndExactSceneKeyToRepository() {
        FakeRepository repository = new FakeRepository(List.of(strategy(42L, "scene_a", "alpha")));
        RiskStrategyApplicationService service = new RiskStrategyApplicationService(repository);

        service.listStrategies(42L, null);
        service.listStrategies(42L, "");
        service.listStrategies(42L, "   ");
        service.listStrategies(42L, "scene_a");

        assertThat(repository.requests).containsExactly(
                new Request(42L, null),
                new Request(42L, ""),
                new Request(42L, "   "),
                new Request(42L, "scene_a"));
    }

    /**
     * 执行 mybatisRepositoryQueriesTenantOnlyWhenSceneKeyIsNullOrBlankAndOrdersByStrategyKey 相关的风控处理逻辑。
     */
    @Test
    void mybatisRepositoryQueriesTenantOnlyWhenSceneKeyIsNullOrBlankAndOrdersByStrategyKey() {
        RiskStrategyMapper mapper = mock(RiskStrategyMapper.class);
        RiskStrategyDO row = row(42L, "scene_a", "alpha");
        when(mapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(row));
        MybatisRiskStrategyRepository repository = new MybatisRiskStrategyRepository(mapper);

        List<RiskStrategyView> nullSceneRows = repository.listStrategies(42L, null);
        List<RiskStrategyView> blankSceneRows = repository.listStrategies(42L, "   ");

        ArgumentCaptor<LambdaQueryWrapper<RiskStrategyDO>> wrapperCaptor = ArgumentCaptor.captor();
        verify(mapper, org.mockito.Mockito.times(2)).selectList(wrapperCaptor.capture());
        assertThat(wrapperCaptor.getAllValues()).allSatisfy(wrapper -> {
            assertThat(wrapper.getSqlSegment())
                    .contains("tenant_id", "ORDER BY", "strategy_key")
                    .doesNotContain("scene_key");
            assertThat(wrapper.getParamNameValuePairs().values()).containsExactly(42L);
        });
        assertThat(nullSceneRows).containsExactly(strategy(42L, "scene_a", "alpha"));
        assertThat(blankSceneRows).containsExactly(strategy(42L, "scene_a", "alpha"));
    }

    /**
     * 执行 mybatisRepositoryFiltersExactSceneKeyWhenSceneKeyIsNonBlankAndOrdersByStrategyKey 相关的风控处理逻辑。
     */
    @Test
    void mybatisRepositoryFiltersExactSceneKeyWhenSceneKeyIsNonBlankAndOrdersByStrategyKey() {
        RiskStrategyMapper mapper = mock(RiskStrategyMapper.class);
        RiskStrategyDO row = row(42L, "scene_a", "alpha");
        when(mapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(row));
        MybatisRiskStrategyRepository repository = new MybatisRiskStrategyRepository(mapper);

        List<RiskStrategyView> strategies = repository.listStrategies(42L, "scene_a");

        ArgumentCaptor<LambdaQueryWrapper<RiskStrategyDO>> wrapperCaptor = ArgumentCaptor.captor();
        verify(mapper).selectList(wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getSqlSegment())
                .contains("tenant_id", "scene_key", "ORDER BY", "strategy_key");
        assertThat(wrapperCaptor.getValue().getParamNameValuePairs().values())
                .containsExactlyInAnyOrder(42L, "scene_a");
        assertThat(strategies).containsExactly(strategy(42L, "scene_a", "alpha"));
    }

    /**
     * 执行 strategy 相关的风控处理逻辑。
     */
    private static RiskStrategyView strategy(Long tenantId, String sceneKey, String strategyKey) {
        return new RiskStrategyView(
                tenantId,
                sceneKey,
                strategyKey,
                "Benefit Default",
                "ACTIVE",
                3,
                4,
                "MEDIUM",
                "risk-ops");
    }

    /**
     * 执行 row 相关的风控处理逻辑。
     */
    private static RiskStrategyDO row(Long tenantId, String sceneKey, String strategyKey) {
        RiskStrategyDO row = new RiskStrategyDO();
        row.setTenantId(tenantId);
        row.setSceneKey(sceneKey);
        row.setStrategyKey(strategyKey);
        row.setName("Benefit Default");
        row.setStatus("ACTIVE");
        row.setActiveVersion(3);
        row.setDraftVersion(4);
        row.setRiskLevel("MEDIUM");
        row.setOwner("risk-ops");
        return row;
    }

    /**
     * 定义 Request 的风控模块职责和数据契约。
     */
    private static final class Request {

        /**
         * Request 的 tenantId 字段。
         */
        private final Long tenantId;


        /**
         * Request 的 sceneKey 字段。
         */
        private final String sceneKey;


        /**
         * 创建 Request。
         *
         * @param tenantId Request 的 tenantId 字段
         * @param sceneKey Request 的 sceneKey 字段
         */
        public Request(Long tenantId, String sceneKey) {
            this.tenantId = tenantId;
            this.sceneKey = sceneKey;
        }

        /**
         * 返回 Request 的 tenantId 字段。
         *
         * @return tenantId 字段值
         */
        public Long tenantId() {
            return tenantId;
        }

        /**
         * 返回 Request 的 sceneKey 字段。
         *
         * @return sceneKey 字段值
         */
        public String sceneKey() {
            return sceneKey;
        }

        /**
         * 比较当前 Request 与其他对象是否相等。
         *
         * @param o 待比较对象
         * @return 相等时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Request other)) {
                return false;
            }
            return Objects.equals(tenantId, other.tenantId)
                    && Objects.equals(sceneKey, other.sceneKey);
        }

        /**
         * 计算 Request 的哈希值。
         *
         * @return 哈希值
         */
        @Override
        public int hashCode() {
            return Objects.hash(tenantId, sceneKey);
        }

        /**
         * 返回 Request 的调试字符串。
         *
         * @return 调试字符串
         */
        @Override
        public String toString() {
            return "Request[tenantId=" + tenantId + ", sceneKey=" + sceneKey + "]";
        }
    }

    /**
     * 定义 FakeRepository 的风控模块职责和数据契约。
     */
    private static final class FakeRepository implements RiskStrategyRepository {
        /**
         * 保存 rows 对应的风控状态或配置。
         */
        private final List<RiskStrategyView> rows;
        private final List<Request> requests = new ArrayList<>();

        private FakeRepository(List<RiskStrategyView> rows) {
            this.rows = rows;
        }

        /**
         * 执行 listStrategies 相关的风控处理逻辑。
         */
        @Override
        public List<RiskStrategyView> listStrategies(Long tenantId, String sceneKey) {
            requests.add(new Request(tenantId, sceneKey));
            return rows;
        }
    }
}
