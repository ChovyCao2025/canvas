package org.chovy.canvas.domain.canvas;

import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.common.enums.VersionStatus;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.dto.CanvasUpdateReq;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.handlers.GroovyHandler;
import org.chovy.canvas.engine.rule.CanvasRuleGraphValidator;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.chovy.canvas.engine.trigger.CanvasSchedulerService;
import org.chovy.canvas.engine.trigger.TriggerPreCheckService;
import org.chovy.canvas.infrastructure.cache.CanvasConfigCache;
import org.chovy.canvas.infrastructure.redis.TriggerRouteService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasServiceDraftUpdateStateTest {

    @Test
    void updateDraftPreservesPublishedRuntimePolicyAndCreatesNewDraftVersionWhenEditingGraph() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        CanvasDO canvas = publishedCanvas();
        CanvasVersionDO draft = draftVersion();
        when(canvasMapper.selectById(10L)).thenReturn(canvas);
        when(versionMapper.selectOne(any())).thenReturn(draft);
        CanvasService service = service(canvasMapper, versionMapper);

        CanvasUpdateReq req = new CanvasUpdateReq();
        req.setName("new name");
        req.setDescription("new description");
        req.setGraphJson("{\"nodes\":[{\"id\":\"start\"}]}");
        req.setUpdatedBy("operator");

        service.updateDraft(10L, req);

        ArgumentCaptor<CanvasDO> canvasCaptor = ArgumentCaptor.forClass(CanvasDO.class);
        verify(canvasMapper).updateById(canvasCaptor.capture());
        CanvasDO updated = canvasCaptor.getValue();
        assertThat(updated.getName()).isEqualTo("new name");
        assertThat(updated.getDescription()).isEqualTo("new description");
        assertThat(updated.getTriggerType()).isEqualTo("SCHEDULED");
        assertThat(updated.getCronExpression()).isEqualTo("0 0 10 * * ?");
        assertThat(updated.getValidStart()).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));
        assertThat(updated.getValidEnd()).isEqualTo(LocalDateTime.of(2026, 12, 31, 23, 59));
        assertThat(updated.getMaxTotalExecutions()).isEqualTo(1000);
        assertThat(updated.getPerUserDailyLimit()).isEqualTo(3);
        assertThat(updated.getPerUserTotalLimit()).isEqualTo(10);
        assertThat(updated.getCooldownSeconds()).isEqualTo(60);

        ArgumentCaptor<CanvasVersionDO> versionCaptor = ArgumentCaptor.forClass(CanvasVersionDO.class);
        verify(versionMapper).insert(versionCaptor.capture());
        verify(versionMapper, never()).updateById(any(CanvasVersionDO.class));
        CanvasVersionDO inserted = versionCaptor.getValue();
        assertThat(inserted.getCanvasId()).isEqualTo(10L);
        assertThat(inserted.getVersion()).isEqualTo(3);
        assertThat(inserted.getStatus()).isEqualTo(VersionStatus.DRAFT.getCode());
        assertThat(inserted.getGraphJson()).isEqualTo("{\"nodes\":[{\"id\":\"start\"}]}");
        assertThat(inserted.getCreatedBy()).isEqualTo("operator");
    }

    @Test
    void updateDraftRejectsPublishedRuntimePolicyChange() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        CanvasDO canvas = publishedCanvas();
        when(canvasMapper.selectById(10L)).thenReturn(canvas);
        CanvasService service = service(canvasMapper, versionMapper);

        CanvasUpdateReq req = new CanvasUpdateReq();
        req.setName("new name");
        req.setMaxTotalExecutions(2000);

        assertThatThrownBy(() -> service.updateDraft(10L, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("published runtime policy");

        verify(canvasMapper, never()).updateById(any(CanvasDO.class));
        verify(versionMapper, never()).updateById(any(CanvasVersionDO.class));
    }

    @Test
    void updateDraftPersistsControlGroupAndAttributionSettingsForDraftCanvas() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        CanvasDO canvas = new CanvasDO();
        canvas.setId(13L);
        canvas.setStatus(CanvasStatusEnum.DRAFT.getCode());
        when(canvasMapper.selectById(13L)).thenReturn(canvas);
        CanvasService service = service(canvasMapper, versionMapper);

        CanvasUpdateReq req = new CanvasUpdateReq();
        req.setName("draft");
        req.setControlGroupPercent(10);
        req.setControlGroupSalt("salt-a");
        req.setConversionEventCode("ORDER_PAID");
        req.setAttributionWindowDays(14);

        service.updateDraft(13L, req);

        ArgumentCaptor<CanvasDO> canvasCaptor = ArgumentCaptor.forClass(CanvasDO.class);
        verify(canvasMapper).updateById(canvasCaptor.capture());
        assertThat(canvasCaptor.getValue().getControlGroupPercent()).isEqualTo(10);
        assertThat(canvasCaptor.getValue().getControlGroupSalt()).isEqualTo("salt-a");
        assertThat(canvasCaptor.getValue().getConversionEventCode()).isEqualTo("ORDER_PAID");
        assertThat(canvasCaptor.getValue().getAttributionWindowDays()).isEqualTo(14);
    }

    @Test
    void updateDraftRejectsKilledCanvas() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        CanvasDO canvas = new CanvasDO();
        canvas.setId(11L);
        canvas.setStatus(CanvasStatusEnum.KILLED.getCode());
        when(canvasMapper.selectById(11L)).thenReturn(canvas);
        CanvasService service = service(canvasMapper, versionMapper);

        CanvasUpdateReq req = new CanvasUpdateReq();
        req.setName("new name");

        assertThatThrownBy(() -> service.updateDraft(11L, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("KILLED");

        verify(canvasMapper, never()).updateById(any(CanvasDO.class));
        verify(versionMapper, never()).updateById(any(CanvasVersionDO.class));
    }

    @Test
    void revertToVersionRejectsKilledCanvas() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        CanvasDO canvas = new CanvasDO();
        canvas.setId(12L);
        canvas.setStatus(CanvasStatusEnum.KILLED.getCode());
        when(canvasMapper.selectById(12L)).thenReturn(canvas);
        CanvasVersionDO target = draftVersion();
        target.setCanvasId(12L);
        when(versionMapper.selectById(102L)).thenReturn(target);
        when(versionMapper.selectOne(any())).thenReturn(draftVersion());
        CanvasService service = service(canvasMapper, versionMapper);

        assertThatThrownBy(() -> service.revertToVersion(12L, 102L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("KILLED");

        verify(versionMapper, never()).updateById(any(CanvasVersionDO.class));
    }

    private static CanvasService service(CanvasMapper canvasMapper, CanvasVersionMapper versionMapper) {
        return new CanvasService(
                canvasMapper,
                versionMapper,
                mock(DagParser.class),
                mock(TriggerRouteService.class),
                mock(CanvasSchedulerService.class),
                mock(CanvasConfigCache.class),
                mock(CanvasExecutionService.class),
                mock(TriggerPreCheckService.class),
                mock(GroovyHandler.class),
                mock(org.chovy.canvas.engine.handlers.MqTriggerHandler.class),
                mock(CanvasRuleGraphValidator.class),
                mock(org.springframework.data.redis.core.StringRedisTemplate.class),
                mock(CanvasTransactionService.class),
                new CanvasExamplesProperties());
    }

    private static CanvasDO publishedCanvas() {
        CanvasDO canvas = new CanvasDO();
        canvas.setId(10L);
        canvas.setStatus(CanvasStatusEnum.PUBLISHED.getCode());
        canvas.setName("old name");
        canvas.setDescription("old description");
        canvas.setTriggerType("SCHEDULED");
        canvas.setCronExpression("0 0 10 * * ?");
        canvas.setValidStart(LocalDateTime.of(2026, 1, 1, 0, 0));
        canvas.setValidEnd(LocalDateTime.of(2026, 12, 31, 23, 59));
        canvas.setMaxTotalExecutions(1000);
        canvas.setPerUserDailyLimit(3);
        canvas.setPerUserTotalLimit(10);
        canvas.setCooldownSeconds(60);
        return canvas;
    }

    private static CanvasVersionDO draftVersion() {
        CanvasVersionDO draft = new CanvasVersionDO();
        draft.setId(101L);
        draft.setCanvasId(10L);
        draft.setVersion(2);
        draft.setStatus(VersionStatus.DRAFT.getCode());
        draft.setGraphJson("{\"nodes\":[]}");
        return draft;
    }
}
