package org.chovy.canvas.domain.canvas;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.domain.constant.CanvasStatusEnum;
import org.chovy.canvas.domain.constant.VersionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
        CanvasTemplate template = template("component_event_if_coupon", "{\"nodes\":[]}");
        when(templateMapper.selectList(any())).thenReturn(List.of(template));
        when(canvasMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            Canvas canvas = invocation.getArgument(0);
            canvas.setId(99L);
            return 1;
        }).when(canvasMapper).insert(any(Canvas.class));

        CanvasExampleSeeder seeder = new CanvasExampleSeeder(
                templateMapper, canvasMapper, canvasVersionMapper, new ObjectMapper(), properties);

        seeder.run(null);

        ArgumentCaptor<Canvas> canvasCaptor = ArgumentCaptor.forClass(Canvas.class);
        verify(canvasMapper).insert(canvasCaptor.capture());
        Canvas inserted = canvasCaptor.getValue();
        assertThat(inserted.getName()).isEqualTo("示例：事件触发新客领券");
        assertThat(inserted.getStatus()).isEqualTo(CanvasStatusEnum.DRAFT.getCode());
        assertThat(inserted.getCreatedBy()).isEqualTo("example-seed");
        assertThat(inserted.getIsExample()).isEqualTo(1);
        assertThat(inserted.getSourceTemplateKey()).isEqualTo("component_event_if_coupon");

        ArgumentCaptor<CanvasVersion> versionCaptor = ArgumentCaptor.forClass(CanvasVersion.class);
        verify(canvasVersionMapper).insert(versionCaptor.capture());
        CanvasVersion version = versionCaptor.getValue();
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
        when(canvasMapper.selectOne(any())).thenReturn(new Canvas());
        CanvasExampleSeeder seeder = new CanvasExampleSeeder(
                templateMapper, canvasMapper, canvasVersionMapper, new ObjectMapper(), properties);

        seeder.run(null);

        verify(canvasMapper, never()).insert(any(Canvas.class));
        verify(canvasVersionMapper, never()).insert(any(CanvasVersion.class));
    }

    @Test
    void invalidTemplateGraphIsSkipped() throws Exception {
        CanvasExamplesProperties properties = new CanvasExamplesProperties();
        when(templateMapper.selectList(any())).thenReturn(List.of(template("bad_graph", "{")));
        CanvasExampleSeeder seeder = new CanvasExampleSeeder(
                templateMapper, canvasMapper, canvasVersionMapper, new ObjectMapper(), properties);

        seeder.run(null);

        verify(canvasMapper, never()).insert(any(Canvas.class));
        verify(canvasVersionMapper, never()).insert(any(CanvasVersion.class));
    }

    private static CanvasTemplate template(String key, String graphJson) {
        CanvasTemplate template = new CanvasTemplate();
        template.setTemplateKey(key);
        template.setName("示例：事件触发新客领券");
        template.setDescription("事件触发后判断新客并发券");
        template.setGraphJson(graphJson);
        template.setIsOfficial(1);
        template.setEnabled(1);
        return template;
    }
}
