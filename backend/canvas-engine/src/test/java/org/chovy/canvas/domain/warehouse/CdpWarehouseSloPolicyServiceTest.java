package org.chovy.canvas.domain.warehouse;

import org.chovy.canvas.dal.dataobject.CdpWarehouseSloPolicyDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseSloPolicyMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseSloPolicyServiceTest {

    @Test
    void upsertPolicyAppliesDefaultsAndPersistsTenantPolicy() {
        CdpWarehouseSloPolicyMapper mapper = mock(CdpWarehouseSloPolicyMapper.class);
        CdpWarehouseSloPolicyService service = new CdpWarehouseSloPolicyService(mapper);

        CdpWarehouseSloPolicyService.SloPolicyView view = service.upsertPolicy(9L,
                new CdpWarehouseSloPolicyService.SloPolicyCommand(
                        null, null, null, null, null, null, null, null,
                        null, "operator", "tenant readiness policy"));

        ArgumentCaptor<CdpWarehouseSloPolicyDO> row = ArgumentCaptor.forClass(CdpWarehouseSloPolicyDO.class);
        verify(mapper).upsert(row.capture());
        assertThat(row.getValue().getTenantId()).isEqualTo(9L);
        assertThat(row.getValue().getPolicyKey()).isEqualTo(CdpWarehouseSloPolicyService.DEFAULT_POLICY_KEY);
        assertThat(row.getValue().getOfflineWarnRunGapMinutes()).isEqualTo(120);
        assertThat(row.getValue().getOfflineFailWatermarkLagMinutes()).isEqualTo(120);
        assertThat(row.getValue().getAudienceFailRunGapMinutes()).isEqualTo(4320);
        assertThat(view.tenantId()).isEqualTo(9L);
        assertThat(view.status()).isEqualTo("ACTIVE");
    }

    @Test
    void effectivePolicyUsesTenantOverrideWhenPresent() {
        CdpWarehouseSloPolicyMapper mapper = mock(CdpWarehouseSloPolicyMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(policy(0L, 120), policy(9L, 15)));
        CdpWarehouseSloPolicyService service = new CdpWarehouseSloPolicyService(mapper);

        CdpWarehouseSloPolicyService.SloPolicyView view = service.effectivePolicy(9L);

        assertThat(view.tenantId()).isEqualTo(9L);
        assertThat(view.offlineWarnRunGapMinutes()).isEqualTo(15);
    }

    @Test
    void effectivePolicyFallsBackToInCodeDefaultWhenRowsAreMissing() {
        CdpWarehouseSloPolicyMapper mapper = mock(CdpWarehouseSloPolicyMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of());
        CdpWarehouseSloPolicyService service = new CdpWarehouseSloPolicyService(mapper);

        CdpWarehouseSloPolicyService.SloPolicyView view = service.effectivePolicy(9L);

        assertThat(view.tenantId()).isEqualTo(9L);
        assertThat(view.policyKey()).isEqualTo(CdpWarehouseSloPolicyService.DEFAULT_POLICY_KEY);
        assertThat(view.offlineFailRunGapMinutes()).isEqualTo(360);
    }

    @Test
    void invalidWarnFailOrderingIsRejected() {
        CdpWarehouseSloPolicyMapper mapper = mock(CdpWarehouseSloPolicyMapper.class);
        CdpWarehouseSloPolicyService service = new CdpWarehouseSloPolicyService(mapper);

        assertThatThrownBy(() -> service.upsertPolicy(9L,
                new CdpWarehouseSloPolicyService.SloPolicyCommand(
                        "warehouse_readiness_default", "bad", 60, 30, null, null,
                        null, null, "ACTIVE", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("offline run gap warn threshold");
        verify(mapper, never()).upsert(any());
    }

    private CdpWarehouseSloPolicyDO policy(Long tenantId, int offlineWarnRunGapMinutes) {
        CdpWarehouseSloPolicyDO row = new CdpWarehouseSloPolicyDO();
        row.setId(tenantId + 1);
        row.setTenantId(tenantId);
        row.setPolicyKey(CdpWarehouseSloPolicyService.DEFAULT_POLICY_KEY);
        row.setDisplayName("policy " + tenantId);
        row.setOfflineWarnRunGapMinutes(offlineWarnRunGapMinutes);
        row.setOfflineFailRunGapMinutes(360);
        row.setOfflineWarnWatermarkLagMinutes(30);
        row.setOfflineFailWatermarkLagMinutes(120);
        row.setAudienceWarnRunGapMinutes(1440);
        row.setAudienceFailRunGapMinutes(4320);
        row.setStatus("ACTIVE");
        row.setOwnerName("data-platform");
        row.setDescription("policy");
        return row;
    }
}
