package org.chovy.canvas.domain.risk.governance;

import org.chovy.canvas.dal.dataobject.RiskSceneDO;
import org.chovy.canvas.dal.mapper.RiskSceneMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdbcRiskSceneStoreTest {

    private final RiskSceneMapper mapper = mock(RiskSceneMapper.class);
    private final JdbcRiskSceneStore store = new JdbcRiskSceneStore(mapper);

    @Test
    void readsPersistedScenesForTenant() {
        RiskSceneDO row = new RiskSceneDO();
        row.setTenantId(7L);
        row.setSceneKey("CHECKOUT_PRECHECK");
        row.setName("Checkout precheck");
        row.setEventSchemaKey("risk.checkout.v1");
        row.setStatus("ACTIVE");
        row.setDefaultMode("ENFORCE");
        row.setFailPolicy("FAIL_CLOSED");
        row.setLatencyBudgetMs(40);
        row.setOwner("risk-payment");
        when(mapper.selectList(any())).thenReturn(List.of(row));

        assertThat(store.listScenes(7L)).containsExactly(new RiskSceneView(
                7L,
                "CHECKOUT_PRECHECK",
                "Checkout precheck",
                "risk.checkout.v1",
                "ACTIVE",
                "ENFORCE",
                "FAIL_CLOSED",
                40,
                "risk-payment"));
    }

    @Test
    void seedsDefaultScenesWhenTenantHasNoRows() {
        when(mapper.selectList(any())).thenReturn(List.of());

        List<RiskSceneView> scenes = store.listScenes(7L);

        ArgumentCaptor<RiskSceneDO> captor = ArgumentCaptor.forClass(RiskSceneDO.class);
        verify(mapper, atLeast(5)).insert(captor.capture());
        assertThat(scenes).hasSizeGreaterThanOrEqualTo(5);
        assertThat(scenes).allSatisfy(scene -> assertThat(scene.tenantId()).isEqualTo(7L));
        assertThat(captor.getAllValues()).extracting(RiskSceneDO::getTenantId).containsOnly(7L);
        assertThat(captor.getAllValues()).extracting(RiskSceneDO::getSceneKey).contains("MARKETING_BENEFIT_ISSUE");
        assertThat(captor.getAllValues()).allSatisfy(row -> {
            assertThat(row.getCreatedAt()).isNotNull();
            assertThat(row.getUpdatedAt()).isNotNull();
        });
    }
}
