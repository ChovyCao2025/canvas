package org.chovy.canvas.domain.canvas;

import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.common.enums.VersionStatus;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.dto.CanvasUpdateReq;
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
    void updateDraftPreservesPublishedRuntimePolicyWhenEditingMetadataAndDraftGraph() {
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

        verify(versionMapper).updateById(draft);
        assertThat(draft.getGraphJson()).isEqualTo("{\"nodes\":[{\"id\":\"start\"}]}");
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

    private static CanvasService service(CanvasMapper canvasMapper, CanvasVersionMapper versionMapper) {
        return new CanvasService(
                canvasMapper,
                versionMapper,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new CanvasStateTransitionPolicy(),
                new CanvasExamplesProperties(),
                null
        );
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
