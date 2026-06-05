package org.chovy.canvas.domain.canvas;

import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.common.enums.VersionStatus;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionMapper;
import org.chovy.canvas.dal.mapper.CanvasExecutionRequestMapper;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.engine.trigger.TriggerPreCheckService;
import org.chovy.canvas.infrastructure.redis.TriggerRouteService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasOpsServiceTenantTest {

    @Test
    void cloneCarriesTenantToCopiedCanvasAndDraftVersion() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        CanvasDO source = new CanvasDO();
        source.setId(10L);
        source.setTenantId(9L);
        source.setName("source");
        source.setDescription("desc");
        when(canvasMapper.selectById(10L)).thenReturn(source);
        doAnswer(invocation -> {
            CanvasDO copy = invocation.getArgument(0);
            copy.setId(20L);
            return 1;
        }).when(canvasMapper).insert(any(CanvasDO.class));

        CanvasVersionDO draft = new CanvasVersionDO();
        draft.setId(100L);
        draft.setCanvasId(10L);
        draft.setTenantId(9L);
        draft.setGraphJson("{\"nodes\":[]}");
        when(versionMapper.selectOne(any())).thenReturn(draft);
        CanvasOpsService service = service(canvasMapper, versionMapper);

        service.clone(10L, "operator");

        ArgumentCaptor<CanvasDO> canvasCaptor = ArgumentCaptor.forClass(CanvasDO.class);
        ArgumentCaptor<CanvasVersionDO> versionCaptor = ArgumentCaptor.forClass(CanvasVersionDO.class);
        verify(canvasMapper).insert(canvasCaptor.capture());
        verify(versionMapper).insert(versionCaptor.capture());
        assertThat(canvasCaptor.getValue().getTenantId()).isEqualTo(9L);
        assertThat(versionCaptor.getValue().getTenantId()).isEqualTo(9L);
        assertThat(versionCaptor.getValue().getCanvasId()).isEqualTo(20L);
    }

    @Test
    void startCanaryCarriesTenantToCanaryVersion() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        CanvasDO canvas = new CanvasDO();
        canvas.setId(10L);
        canvas.setTenantId(9L);
        canvas.setStatus(CanvasStatusEnum.PUBLISHED.getCode());
        when(canvasMapper.selectById(10L)).thenReturn(canvas);
        CanvasVersionDO draft = new CanvasVersionDO();
        draft.setId(100L);
        draft.setCanvasId(10L);
        draft.setTenantId(9L);
        draft.setGraphJson("{\"nodes\":[]}");
        when(versionMapper.selectOne(any())).thenReturn(draft).thenReturn(null);
        CanvasOpsService service = service(canvasMapper, versionMapper);

        service.startCanary(10L, 20, "operator");

        ArgumentCaptor<CanvasVersionDO> versionCaptor = ArgumentCaptor.forClass(CanvasVersionDO.class);
        verify(versionMapper).insert(versionCaptor.capture());
        assertThat(versionCaptor.getValue().getTenantId()).isEqualTo(9L);
        assertThat(versionCaptor.getValue().getStatus()).isEqualTo(VersionStatus.PUBLISHED.getCode());
    }

    private CanvasOpsService service(CanvasMapper canvasMapper, CanvasVersionMapper versionMapper) {
        return new CanvasOpsService(
                canvasMapper,
                versionMapper,
                mock(CanvasExecutionMapper.class),
                mock(CanvasExecutionRequestMapper.class),
                mock(TriggerRouteService.class),
                mock(TriggerPreCheckService.class),
                mock(CanvasTransactionService.class),
                mock(CanvasService.class),
                mock(org.springframework.data.redis.core.StringRedisTemplate.class));
    }
}
