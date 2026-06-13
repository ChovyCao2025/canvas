package org.chovy.canvas.risk.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.chovy.canvas.risk.adapter.persistence.MybatisRiskListRepository;
import org.chovy.canvas.risk.adapter.persistence.RiskListDO;
import org.chovy.canvas.risk.adapter.persistence.RiskListMapper;
import org.chovy.canvas.risk.api.RiskListView;
import org.chovy.canvas.risk.domain.governance.RiskListRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RiskListApplicationServiceTest {

    @BeforeAll
    static void initMyBatisPlusTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), RiskListDO.class);
    }

    @Test
    void listListsReturnsRepositoryRowsForTenantWithoutSeedingDefaults() {
        FakeRepository repository = new FakeRepository(List.of(list("coupon_abuse")));
        RiskListApplicationService service = new RiskListApplicationService(repository);

        List<RiskListView> lists = service.listLists(42L);

        assertThat(repository.tenantIds).containsExactly(42L);
        assertThat(lists)
                .extracting(
                        RiskListView::tenantId,
                        RiskListView::listKey,
                        RiskListView::listType,
                        RiskListView::subjectType,
                        RiskListView::status,
                        RiskListView::requiresApproval,
                        RiskListView::owner)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(
                        42L,
                        "coupon_abuse",
                        "BLACK",
                        "USER_ID",
                        "ACTIVE",
                        true,
                        "risk-ops"));
    }

    @Test
    void listListsReturnsEmptyRepositoryResultWithoutSyntheticSeedRows() {
        FakeRepository repository = new FakeRepository(List.of());
        RiskListApplicationService service = new RiskListApplicationService(repository);

        List<RiskListView> lists = service.listLists(42L);

        assertThat(repository.tenantIds).containsExactly(42L);
        assertThat(lists).isEmpty();
    }

    @Test
    void mybatisRepositoryQueriesRequestedTenantOrderedByListKey() {
        RiskListMapper mapper = mock(RiskListMapper.class);
        RiskListDO row = new RiskListDO();
        row.setTenantId(42L);
        row.setListKey("coupon_abuse");
        row.setListType("BLACK");
        row.setSubjectType("USER_ID");
        row.setStatus("ACTIVE");
        row.setRequiresApproval(Boolean.TRUE);
        row.setOwner("risk-ops");
        when(mapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(row));
        MybatisRiskListRepository repository = new MybatisRiskListRepository(mapper);

        List<RiskListView> lists = repository.listLists(42L);

        ArgumentCaptor<LambdaQueryWrapper<RiskListDO>> wrapperCaptor = ArgumentCaptor.captor();
        verify(mapper).selectList(wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getSqlSegment())
                .contains("tenant_id", "ORDER BY", "list_key");
        assertThat(wrapperCaptor.getValue().getParamNameValuePairs().values())
                .containsExactly(42L);
        assertThat(lists).containsExactly(list("coupon_abuse"));
    }

    private static RiskListView list(String listKey) {
        return new RiskListView(
                42L,
                listKey,
                "BLACK",
                "USER_ID",
                "ACTIVE",
                true,
                "risk-ops");
    }

    private static final class FakeRepository implements RiskListRepository {
        private final List<RiskListView> rows;
        private final java.util.ArrayList<Long> tenantIds = new java.util.ArrayList<>();

        private FakeRepository(List<RiskListView> rows) {
            this.rows = rows;
        }

        @Override
        public List<RiskListView> listLists(Long tenantId) {
            tenantIds.add(tenantId);
            return rows;
        }
    }
}
