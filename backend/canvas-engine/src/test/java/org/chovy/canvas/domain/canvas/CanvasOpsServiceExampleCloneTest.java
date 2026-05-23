package org.chovy.canvas.domain.canvas;

import org.chovy.canvas.domain.constant.CanvasStatusEnum;
import org.chovy.canvas.domain.constant.VersionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CanvasOpsServiceExampleCloneTest {

    @Mock CanvasMapper canvasMapper;
    @Mock CanvasVersionMapper canvasVersionMapper;
    @Mock org.chovy.canvas.infra.redis.TriggerRouteService triggerRouteService;
    @Mock org.springframework.data.redis.core.StringRedisTemplate redis;

    @Test
    void cloneDoesNotInheritExampleMarkers() {
        Canvas src = new Canvas();
        src.setId(7L);
        src.setName("示例：新客首单券发放");
        src.setDescription("官方示例");
        src.setStatus(CanvasStatusEnum.DRAFT.getCode());
        src.setIsExample(1);
        src.setSourceTemplateKey("ecommerce_new_user_coupon");
        when(canvasMapper.selectById(7L)).thenReturn(src);
        when(canvasVersionMapper.selectOne(any())).thenReturn(draft(7L, "{\"nodes\":[]}"));
        doAnswer(invocation -> {
            Canvas canvas = invocation.getArgument(0);
            canvas.setId(99L);
            return 1;
        }).when(canvasMapper).insert(any(Canvas.class));

        CanvasOpsService service = new CanvasOpsService(canvasMapper, canvasVersionMapper, triggerRouteService, redis);

        service.clone(7L, "alice");

        ArgumentCaptor<Canvas> canvasCaptor = ArgumentCaptor.forClass(Canvas.class);
        verify(canvasMapper).insert(canvasCaptor.capture());
        assertThat(canvasCaptor.getValue().getIsExample()).isEqualTo(0);
        assertThat(canvasCaptor.getValue().getSourceTemplateKey()).isNull();

        ArgumentCaptor<CanvasVersion> versionCaptor = ArgumentCaptor.forClass(CanvasVersion.class);
        verify(canvasVersionMapper).insert(versionCaptor.capture());
        assertThat(versionCaptor.getValue().getCanvasId()).isEqualTo(99L);
        assertThat(versionCaptor.getValue().getGraphJson()).isEqualTo("{\"nodes\":[]}");
    }

    private static CanvasVersion draft(Long canvasId, String graphJson) {
        CanvasVersion version = new CanvasVersion();
        version.setCanvasId(canvasId);
        version.setVersion(1);
        version.setGraphJson(graphJson);
        version.setStatus(VersionStatus.DRAFT.getCode());
        return version;
    }
}
