package org.chovy.canvas.domain.canvas;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.common.enums.VersionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.dataobject.CanvasTemplateDO;
import org.chovy.canvas.dal.mapper.CanvasTemplateMapper;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;

/**
 * Canvas Example Seeder 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
@ExtendWith(MockitoExtension.class)
class CanvasExampleSeederTest {

    @Mock CanvasTemplateMapper templateMapper;
    @Mock CanvasMapper canvasMapper;
    @Mock CanvasVersionMapper canvasVersionMapper;

    @Test
    void disabledToggleSkipsImport() throws Exception {
        CanvasExamplesProperties properties = new CanvasExamplesProperties();
        properties.setEnabled(false);
        CanvasExampleSeeder seeder = new CanvasExampleSeeder(
                templateMapper, canvasMapper, canvasVersionMapper, new ObjectMapper(), properties);

        seeder.run(null);

        verifyNoInteractions(templateMapper, canvasMapper, canvasVersionMapper);
    }

    @Test
    void importsMissingOfficialTemplateAsDraftCanvas() throws Exception {
        CanvasExamplesProperties properties = new CanvasExamplesProperties();
        CanvasTemplateDO template = template("component_event_if_coupon", "{\"nodes\":[]}");
        when(templateMapper.selectList(any())).thenReturn(List.of(template));
        when(canvasMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            CanvasDO canvas = invocation.getArgument(0);
            canvas.setId(99L);
            return 1;
        }).when(canvasMapper).insert(any(CanvasDO.class));

        CanvasExampleSeeder seeder = new CanvasExampleSeeder(
                templateMapper, canvasMapper, canvasVersionMapper, new ObjectMapper(), properties);

        seeder.run(null);

        ArgumentCaptor<CanvasDO> canvasCaptor = ArgumentCaptor.forClass(CanvasDO.class);
        verify(canvasMapper).insert(canvasCaptor.capture());
        CanvasDO inserted = canvasCaptor.getValue();
        assertThat(inserted.getName()).isEqualTo("示例：事件触发新客领券");
        assertThat(inserted.getStatus()).isEqualTo(CanvasStatusEnum.DRAFT.getCode());
        assertThat(inserted.getCreatedBy()).isEqualTo("example-seed");
        assertThat(inserted.getIsExample()).isEqualTo(1);
        assertThat(inserted.getSourceTemplateKey()).isEqualTo("component_event_if_coupon");

        ArgumentCaptor<CanvasVersionDO> versionCaptor = ArgumentCaptor.forClass(CanvasVersionDO.class);
        verify(canvasVersionMapper).insert(versionCaptor.capture());
        CanvasVersionDO version = versionCaptor.getValue();
        assertThat(version.getCanvasId()).isEqualTo(99L);
        assertThat(version.getVersion()).isEqualTo(1);
        assertThat(version.getStatus()).isEqualTo(VersionStatus.DRAFT.getCode());
        assertThat(version.getGraphJson()).isEqualTo("{\"nodes\":[]}");
        assertThat(version.getCreatedBy()).isEqualTo("example-seed");
    }

    @Test
    void existingImportedCanvasIsNotDuplicated() throws Exception {
        CanvasExamplesProperties properties = new CanvasExamplesProperties();
        when(templateMapper.selectList(any())).thenReturn(List.of(template("component_event_if_coupon", "{\"nodes\":[]}")));
        when(canvasMapper.selectOne(any())).thenReturn(new CanvasDO());
        CanvasExampleSeeder seeder = new CanvasExampleSeeder(
                templateMapper, canvasMapper, canvasVersionMapper, new ObjectMapper(), properties);

        seeder.run(null);

        verify(canvasMapper, never()).insert(any(CanvasDO.class));
        verify(canvasVersionMapper, never()).insert(any(CanvasVersionDO.class));
    }

    @Test
    void invalidTemplateGraphIsSkipped() throws Exception {
        CanvasExamplesProperties properties = new CanvasExamplesProperties();
        when(templateMapper.selectList(any())).thenReturn(List.of(template("bad_graph", "{")));
        CanvasExampleSeeder seeder = new CanvasExampleSeeder(
                templateMapper, canvasMapper, canvasVersionMapper, new ObjectMapper(), properties);

        seeder.run(null);

        verify(canvasMapper, never()).insert(any(CanvasDO.class));
        verify(canvasVersionMapper, never()).insert(any(CanvasVersionDO.class));
    }

    private static CanvasTemplateDO template(String key, String graphJson) {
        CanvasTemplateDO template = new CanvasTemplateDO();
        template.setTemplateKey(key);
        template.setName("示例：事件触发新客领券");
        template.setDescription("事件触发后判断新客并发券");
        template.setGraphJson(graphJson);
        template.setIsOfficial(1);
        template.setEnabled(1);
        return template;
    }
}
