package org.chovy.canvas.domain.canvas;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.common.enums.VersionStatus;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasProjectFolderDO;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasProjectFolderMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.dto.canvas.CanvasImportReq;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasImportExportServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void exportSanitizesRuntimeSnapshotSecretsAndKeepsFolderMetadata() throws Exception {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        CanvasProjectFolderMapper folderMapper = mock(CanvasProjectFolderMapper.class);
        CanvasDO canvas = new CanvasDO();
        canvas.setId(62L);
        canvas.setName("Journey");
        canvas.setDescription("desc");
        when(canvasMapper.selectById(62L)).thenReturn(canvas);

        CanvasVersionDO version = new CanvasVersionDO();
        version.setId(91L);
        version.setCanvasId(62L);
        version.setVersion(3);
        version.setGraphJson("""
                {"nodes":[{"id":"tag","type":"TAGGER","config":{
                  "audienceSnapshotId":501,
                  "idempotencyKey":"idem-1",
                  "apiKey":"live-key",
                  "password":"secret",
                  "body":"call 13812345678"
                }}]}
                """);
        when(versionMapper.selectById(91L)).thenReturn(version);

        CanvasProjectFolderDO metadata = new CanvasProjectFolderDO();
        metadata.setCanvasId(62L);
        metadata.setProjectKey("growth");
        metadata.setProjectName("Growth");
        metadata.setFolderKey("new-user");
        metadata.setFolderName("New User");
        when(folderMapper.selectOne(any())).thenReturn(metadata);
        CanvasImportExportService service = new CanvasImportExportService(
                canvasMapper, versionMapper, folderMapper, objectMapper);

        var exported = service.exportCanvas(62L, 91L);

        String graph = objectMapper.writeValueAsString(exported.graph());
        assertThat(graph)
                .doesNotContain("audienceSnapshotId", "idempotencyKey", "apiKey", "password")
                .doesNotContain("live-key", "secret")
                .contains("138****5678");
        assertThat(exported.packageVersion()).isEqualTo(1);
        assertThat(exported.canvas()).containsEntry("projectKey", "growth");
        assertThat(exported.canvas()).containsEntry("folderKey", "new-user");
    }

    @Test
    void exportKeepsFlatCanvasProjectAndFolderMetadataWhenSideTableIsAbsent() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        CanvasProjectFolderMapper folderMapper = mock(CanvasProjectFolderMapper.class);
        CanvasDO canvas = new CanvasDO();
        canvas.setId(63L);
        canvas.setName("Journey");
        canvas.setProjectKey("growth");
        canvas.setProjectName("Growth");
        canvas.setFolderKey("new-user");
        canvas.setFolderName("New User");
        when(canvasMapper.selectById(63L)).thenReturn(canvas);

        CanvasVersionDO version = new CanvasVersionDO();
        version.setId(92L);
        version.setCanvasId(63L);
        version.setVersion(1);
        version.setGraphJson("{\"nodes\":[]}");
        when(versionMapper.selectById(92L)).thenReturn(version);
        CanvasImportExportService service = new CanvasImportExportService(
                canvasMapper, versionMapper, folderMapper, objectMapper);

        var exported = service.exportCanvas(63L, 92L);

        assertThat(exported.canvas()).containsEntry("projectKey", "growth");
        assertThat(exported.canvas()).containsEntry("projectName", "Growth");
        assertThat(exported.canvas()).containsEntry("folderKey", "new-user");
        assertThat(exported.canvas()).containsEntry("folderName", "New User");
    }

    @Test
    void importCreatesDraftCanvasVersionAndFolderMetadata() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        CanvasProjectFolderMapper folderMapper = mock(CanvasProjectFolderMapper.class);
        doAnswer(invocation -> {
            CanvasDO canvas = invocation.getArgument(0);
            canvas.setId(700L);
            return 1;
        }).when(canvasMapper).insert(any(CanvasDO.class));
        doAnswer(invocation -> {
            CanvasVersionDO version = invocation.getArgument(0);
            version.setId(701L);
            return 1;
        }).when(versionMapper).insert(any(CanvasVersionDO.class));
        CanvasImportExportService service = new CanvasImportExportService(
                canvasMapper, versionMapper, folderMapper, objectMapper);
        String packageJson = """
                {"packageVersion":1,
                 "canvas":{"name":"Imported Journey","description":"from package",
                   "projectKey":"growth","projectName":"Growth","folderKey":"new-user","folderName":"New User"},
                 "graph":{"nodes":[{"id":"start","type":"START","config":{}}]}}
                """;

        var resp = service.importCanvas(new CanvasImportReq(packageJson, "alice"), 9L);

        ArgumentCaptor<CanvasDO> canvasCaptor = ArgumentCaptor.forClass(CanvasDO.class);
        ArgumentCaptor<CanvasVersionDO> versionCaptor = ArgumentCaptor.forClass(CanvasVersionDO.class);
        ArgumentCaptor<CanvasProjectFolderDO> folderCaptor = ArgumentCaptor.forClass(CanvasProjectFolderDO.class);
        verify(canvasMapper).insert(canvasCaptor.capture());
        verify(versionMapper).insert(versionCaptor.capture());
        verify(folderMapper).insert(folderCaptor.capture());
        assertThat(canvasCaptor.getValue().getTenantId()).isEqualTo(9L);
        assertThat(canvasCaptor.getValue().getStatus()).isEqualTo(CanvasStatusEnum.DRAFT.getCode());
        assertThat(canvasCaptor.getValue().getName()).isEqualTo("Imported Journey");
        assertThat(canvasCaptor.getValue().getProjectKey()).isEqualTo("growth");
        assertThat(canvasCaptor.getValue().getProjectName()).isEqualTo("Growth");
        assertThat(canvasCaptor.getValue().getFolderKey()).isEqualTo("new-user");
        assertThat(canvasCaptor.getValue().getFolderName()).isEqualTo("New User");
        assertThat(versionCaptor.getValue().getTenantId()).isEqualTo(9L);
        assertThat(versionCaptor.getValue().getCanvasId()).isEqualTo(700L);
        assertThat(versionCaptor.getValue().getVersion()).isEqualTo(1);
        assertThat(versionCaptor.getValue().getStatus()).isEqualTo(VersionStatus.DRAFT.getCode());
        assertThat(folderCaptor.getValue().getCanvasId()).isEqualTo(700L);
        assertThat(folderCaptor.getValue().getProjectKey()).isEqualTo("growth");
        assertThat(folderCaptor.getValue().getFolderKey()).isEqualTo("new-user");
        assertThat(resp.canvas()).isSameAs(canvasCaptor.getValue());
        assertThat(resp.draftVersionId()).isEqualTo(701L);
    }

    @Test
    void importRejectsUnsupportedPackageVersion() {
        CanvasImportExportService service = new CanvasImportExportService(
                mock(CanvasMapper.class),
                mock(CanvasVersionMapper.class),
                mock(CanvasProjectFolderMapper.class),
                objectMapper);

        assertThatThrownBy(() -> service.importCanvas(new CanvasImportReq(
                "{\"packageVersion\":2,\"canvas\":{},\"graph\":{\"nodes\":[]}}", "alice")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported canvas package version");
    }
}
