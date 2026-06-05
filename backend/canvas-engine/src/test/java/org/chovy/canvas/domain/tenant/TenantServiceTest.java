package org.chovy.canvas.domain.tenant;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.common.enums.ExecutionStatus;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDO;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDlqDO;
import org.chovy.canvas.dal.dataobject.TenantDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionDlqMapper;
import org.chovy.canvas.dal.mapper.CanvasExecutionMapper;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.TenantMapper;
import org.chovy.canvas.dto.tenant.TenantUsageDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantMapper tenantMapper;

    @Mock
    private CanvasMapper canvasMapper;

    @Mock
    private CanvasExecutionMapper executionMapper;

    @Mock
    private CanvasExecutionDlqMapper dlqMapper;

    @Test
    void createTenantNormalizesKeyAndSetsActiveStatus() {
        TenantService service = new TenantService(tenantMapper, canvasMapper, executionMapper, dlqMapper);

        TenantDO created = service.create(" Acme Co ", " Acme-01 ", null,
                "{\"maxCanvases\":10}", "root");

        ArgumentCaptor<TenantDO> captor = ArgumentCaptor.forClass(TenantDO.class);
        verify(tenantMapper).insert(captor.capture());
        TenantDO inserted = captor.getValue();
        assertThat(inserted).isSameAs(created);
        assertThat(inserted.getName()).isEqualTo("Acme Co");
        assertThat(inserted.getTenantKey()).isEqualTo("acme-01");
        assertThat(inserted.getStatus()).isEqualTo("ACTIVE");
        assertThat(inserted.getPlanCode()).isEqualTo("default");
        assertThat(inserted.getQuotaJson()).isEqualTo("{\"maxCanvases\":10}");
        assertThat(inserted.getCreatedBy()).isEqualTo("root");
    }

    @Test
    void createTenantRejectsInvalidKeyBeforeMapperWrite() {
        TenantService service = new TenantService(tenantMapper, canvasMapper, executionMapper, dlqMapper);

        assertThatThrownBy(() -> service.create("Acme Co", "Acme 01", null, null, "root"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("key");

        verify(tenantMapper, never()).insert(any(TenantDO.class));
    }

    @Test
    void usageRejectsMissingTenantBeforeMapperReads() {
        TenantService service = new TenantService(tenantMapper, canvasMapper, executionMapper, dlqMapper);

        assertThatThrownBy(() -> service.usage(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("租户 ID");

        verify(canvasMapper, never()).selectCount(any());
        verify(executionMapper, never()).selectCount(any());
        verify(dlqMapper, never()).selectCount(any());
    }

    @Test
    void usageAppliesTenantFiltersToCanvasExecutionAndDlqQueries() {
        when(canvasMapper.selectCount(any())).thenReturn(12L, 3L);
        when(executionMapper.selectCount(any())).thenReturn(30L, 4L);
        when(dlqMapper.selectCount(any())).thenReturn(2L);
        TenantService service = new TenantService(tenantMapper, canvasMapper, executionMapper, dlqMapper);

        TenantUsageDTO usage = service.usage(42L);

        assertThat(usage.getTenantId()).isEqualTo(42L);
        assertThat(usage.getCanvasCount()).isEqualTo(12L);
        assertThat(usage.getPublishedCanvasCount()).isEqualTo(3L);
        assertThat(usage.getExecutionCount()).isEqualTo(30L);
        assertThat(usage.getFailedExecutionCount()).isEqualTo(4L);
        assertThat(usage.getDlqCount()).isEqualTo(2L);

        ArgumentCaptor<Wrapper<CanvasDO>> canvasCaptor = wrapperCaptor();
        verify(canvasMapper, org.mockito.Mockito.times(2)).selectCount(canvasCaptor.capture());
        assertThat(canvasCaptor.getAllValues())
                .allSatisfy(wrapper -> assertThat(wrapper.getSqlSegment()).contains("tenant_id"));
        assertThat(canvasCaptor.getAllValues().get(1).getSqlSegment())
                .contains("status");
        assertThat(((QueryWrapper<CanvasDO>) canvasCaptor.getAllValues().get(1)).getParamNameValuePairs())
                .containsValue(CanvasStatusEnum.PUBLISHED.getCode());

        ArgumentCaptor<Wrapper<CanvasExecutionDO>> executionCaptor = wrapperCaptor();
        verify(executionMapper, org.mockito.Mockito.times(2)).selectCount(executionCaptor.capture());
        assertThat(executionCaptor.getAllValues())
                .allSatisfy(wrapper -> assertThat(wrapper.getSqlSegment()).contains("tenant_id"));
        assertThat(executionCaptor.getAllValues().get(1).getSqlSegment())
                .contains("status");
        assertThat(((QueryWrapper<CanvasExecutionDO>) executionCaptor.getAllValues().get(1))
                .getParamNameValuePairs()).containsValue(ExecutionStatus.FAILED.getCode());

        ArgumentCaptor<Wrapper<CanvasExecutionDlqDO>> dlqCaptor = wrapperCaptor();
        verify(dlqMapper).selectCount(dlqCaptor.capture());
        Wrapper<CanvasExecutionDlqDO> dlqWrapper = dlqCaptor.getValue();
        assertThat(dlqWrapper.getSqlSegment())
                .contains("canvas_id IN (SELECT id FROM canvas WHERE tenant_id =");
        assertThat(((QueryWrapper<CanvasExecutionDlqDO>) dlqWrapper).getParamNameValuePairs())
                .containsValue(42L);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> ArgumentCaptor<Wrapper<T>> wrapperCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(Wrapper.class);
    }
}
