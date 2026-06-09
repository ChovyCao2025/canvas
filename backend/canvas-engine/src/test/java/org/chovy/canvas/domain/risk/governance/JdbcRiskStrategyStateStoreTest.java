package org.chovy.canvas.domain.risk.governance;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.RiskStrategyDO;
import org.chovy.canvas.dal.dataobject.RiskStrategyVersionDO;
import org.chovy.canvas.dal.mapper.RiskStrategyMapper;
import org.chovy.canvas.dal.mapper.RiskStrategyVersionMapper;
import org.chovy.canvas.domain.risk.governance.RiskStrategyService.StrategyState;
import org.chovy.canvas.domain.risk.governance.RiskStrategyService.StrategyVersion;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdbcRiskStrategyStateStoreTest {

    private final RiskStrategyMapper strategyMapper = mock(RiskStrategyMapper.class);
    private final RiskStrategyVersionMapper versionMapper = mock(RiskStrategyVersionMapper.class);
    private final JdbcRiskStrategyStateStore store = new JdbcRiskStrategyStateStore(
            strategyMapper,
            versionMapper,
            new ObjectMapper(),
            Clock.fixed(Instant.parse("2026-06-09T00:00:00Z"), ZoneOffset.UTC));

    @Test
    void insertsStrategyAndVersionLifecycleRows() {
        StrategyState state = new StrategyState(7L, "PAYMENT", "payment-main", "Payment Main", "HIGH", "alice");
        state.status(RiskStrategyLifecycleStatus.APPROVAL_PENDING);
        state.draftVersion(1);
        state.versions().put(1, new StrategyVersion(1, RiskStrategyLifecycleStatus.APPROVAL_PENDING,
                "{\"mode\":\"DUAL_RUN\",\"trafficPercent\":25,\"rules\":[]}",
                "{\"valid\":true}", "alice", "alice", null));

        store.save(state);

        ArgumentCaptor<RiskStrategyDO> strategyCaptor = ArgumentCaptor.forClass(RiskStrategyDO.class);
        ArgumentCaptor<RiskStrategyVersionDO> versionCaptor = ArgumentCaptor.forClass(RiskStrategyVersionDO.class);
        verify(strategyMapper).insert(strategyCaptor.capture());
        verify(versionMapper).insert(versionCaptor.capture());

        RiskStrategyDO strategy = strategyCaptor.getValue();
        assertThat(strategy.getTenantId()).isEqualTo(7L);
        assertThat(strategy.getSceneKey()).isEqualTo("PAYMENT");
        assertThat(strategy.getStatus()).isEqualTo("APPROVAL_PENDING");
        assertThat(strategy.getDraftVersion()).isEqualTo(1);

        RiskStrategyVersionDO version = versionCaptor.getValue();
        assertThat(version.getStatus()).isEqualTo("APPROVAL_PENDING");
        assertThat(version.getMode()).isEqualTo("DUAL_RUN");
        assertThat(version.getTrafficPercent()).isEqualByComparingTo("25");
        assertThat(version.getCompiledHash()).startsWith("sha256:");
        assertThat(version.getSubmittedBy()).isEqualTo("alice");
        assertThat(version.getSubmittedAt()).isNotNull();
        assertThat(version.getApprovedBy()).isNull();
    }

    @Test
    void loadsPersistedActiveStrategyStateWithVersions() {
        RiskStrategyDO strategy = strategyRow();
        RiskStrategyVersionDO version = versionRow();
        when(strategyMapper.selectOne(any())).thenReturn(strategy);
        when(versionMapper.selectList(any())).thenReturn(List.of(version));

        StrategyState state = store.find(7L, "payment-main").orElseThrow();

        assertThat(state.tenantId()).isEqualTo(7L);
        assertThat(state.status()).isEqualTo(RiskStrategyLifecycleStatus.ACTIVE);
        assertThat(state.activeVersion()).isEqualTo(3);
        assertThat(state.versions()).containsKey(3);
        assertThat(state.versions().get(3).approvedBy()).isEqualTo("bob");
    }

    private RiskStrategyDO strategyRow() {
        RiskStrategyDO row = new RiskStrategyDO();
        row.setTenantId(7L);
        row.setSceneKey("PAYMENT");
        row.setStrategyKey("payment-main");
        row.setName("Payment Main");
        row.setStatus("ACTIVE");
        row.setActiveVersion(3);
        row.setDraftVersion(4);
        row.setRiskLevel("HIGH");
        row.setOwner("alice");
        return row;
    }

    private RiskStrategyVersionDO versionRow() {
        RiskStrategyVersionDO row = new RiskStrategyVersionDO();
        row.setTenantId(7L);
        row.setStrategyKey("payment-main");
        row.setVersion(3);
        row.setStatus("ACTIVE");
        row.setDefinitionJson("{\"rules\":[]}");
        row.setValidationJson("{\"valid\":true}");
        row.setCreatedBy("alice");
        row.setSubmittedBy("alice");
        row.setApprovedBy("bob");
        return row;
    }
}
