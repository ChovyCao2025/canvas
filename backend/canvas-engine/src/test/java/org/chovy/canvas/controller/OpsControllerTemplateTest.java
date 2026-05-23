package org.chovy.canvas.controller;

import org.chovy.canvas.domain.approval.CanvasManualApprovalMapper;
import org.chovy.canvas.domain.canvas.Canvas;
import org.chovy.canvas.domain.canvas.CanvasMapper;
import org.chovy.canvas.domain.canvas.CanvasTemplate;
import org.chovy.canvas.domain.canvas.CanvasTemplateMapper;
import org.chovy.canvas.domain.canvas.CanvasVersion;
import org.chovy.canvas.domain.canvas.CanvasVersionMapper;
import org.chovy.canvas.domain.constant.CanvasStatusEnum;
import org.chovy.canvas.domain.constant.VersionStatus;
import org.chovy.canvas.infra.cache.CanvasConfigCache;
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
class OpsControllerTemplateTest {

    @Mock CanvasTemplateMapper templateMapper;
    @Mock CanvasMapper canvasMapper;
    @Mock CanvasVersionMapper canvasVersionMapper;
    @Mock CanvasManualApprovalMapper approvalMapper;
    @Mock CanvasConfigCache configCache;

    @Test
    void createFromTemplateCreatesDraftVersionWithTemplateGraph() {
        CanvasTemplate template = new CanvasTemplate();
        template.setId(5L);
        template.setName("示例：事件触发新客领券");
        template.setDescription("事件触发后判断新客并发券");
        template.setGraphJson("{\"nodes\":[]}");
        template.setUseCount(3);
        when(templateMapper.selectById(5L)).thenReturn(template);
        doAnswer(invocation -> {
            Canvas canvas = invocation.getArgument(0);
            canvas.setId(88L);
            return 1;
        }).when(canvasMapper).insert(any(Canvas.class));

        OpsController controller = new OpsController(
                templateMapper, canvasMapper, canvasVersionMapper, approvalMapper, configCache);
        OpsController.FromTemplateReq req = new OpsController.FromTemplateReq();
        req.setName("我的新客发券流程");

        Canvas created = controller.createFromTemplate(5L, req).block().getData();

        assertThat(created.getId()).isEqualTo(88L);
        assertThat(created.getName()).isEqualTo("我的新客发券流程");
        assertThat(created.getStatus()).isEqualTo(CanvasStatusEnum.DRAFT.getCode());
        assertThat(created.getIsExample()).isEqualTo(0);
        assertThat(created.getSourceTemplateKey()).isNull();

        ArgumentCaptor<CanvasVersion> versionCaptor = ArgumentCaptor.forClass(CanvasVersion.class);
        verify(canvasVersionMapper).insert(versionCaptor.capture());
        CanvasVersion version = versionCaptor.getValue();
        assertThat(version.getCanvasId()).isEqualTo(88L);
        assertThat(version.getVersion()).isEqualTo(1);
        assertThat(version.getGraphJson()).isEqualTo("{\"nodes\":[]}");
        assertThat(version.getStatus()).isEqualTo(VersionStatus.DRAFT.getCode());

        assertThat(template.getUseCount()).isEqualTo(4);
        verify(templateMapper).updateById(template);
    }
}
