package org.chovy.canvas.domain.canvas;

import org.chovy.canvas.dal.dataobject.CanvasProjectFolderDO;
import org.chovy.canvas.dal.dataobject.CanvasProjectDO;
import org.chovy.canvas.dal.mapper.CanvasProjectFolderMapper;
import org.chovy.canvas.dal.mapper.CanvasProjectMapper;
import org.chovy.canvas.dto.canvas.ProjectFolderMetadataReq;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasProjectFolderMetadataServiceTest {

    @Test
    void saveMetadataCreatesRowWhenMissing() {
        CanvasProjectFolderMapper mapper = mock(CanvasProjectFolderMapper.class);
        CanvasProjectMapper projectMapper = mock(CanvasProjectMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);
        CanvasProjectFolderMetadataService service = new CanvasProjectFolderMetadataService(mapper, projectMapper);

        var resp = service.saveMetadata(1L, 62L, new ProjectFolderMetadataReq(
                9L, " growth ", " Growth ", " new-user ", " New User ", " alice "));

        ArgumentCaptor<CanvasProjectFolderDO> captor = ArgumentCaptor.forClass(CanvasProjectFolderDO.class);
        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getCanvasId()).isEqualTo(62L);
        assertThat(captor.getValue().getProjectId()).isEqualTo(9L);
        assertThat(captor.getValue().getProjectKey()).isEqualTo("growth");
        assertThat(captor.getValue().getFolderKey()).isEqualTo("new-user");
        assertThat(captor.getValue().getUpdatedBy()).isEqualTo("alice");
        assertThat(resp.projectId()).isEqualTo(9L);
        assertThat(resp.projectName()).isEqualTo("Growth");
        assertThat(resp.folderName()).isEqualTo("New User");
    }

    @Test
    void saveMetadataUpdatesExistingRow() {
        CanvasProjectFolderMapper mapper = mock(CanvasProjectFolderMapper.class);
        CanvasProjectMapper projectMapper = mock(CanvasProjectMapper.class);
        CanvasProjectFolderDO existing = new CanvasProjectFolderDO();
        existing.setId(5L);
        existing.setCanvasId(62L);
        when(mapper.selectOne(any())).thenReturn(existing);
        CanvasProjectFolderMetadataService service = new CanvasProjectFolderMetadataService(mapper, projectMapper);

        service.saveMetadata(1L, 62L, new ProjectFolderMetadataReq(9L, "growth", null, "new-user", null, "alice"));

        ArgumentCaptor<CanvasProjectFolderDO> captor = ArgumentCaptor.forClass(CanvasProjectFolderDO.class);
        verify(mapper).updateById(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(5L);
        assertThat(captor.getValue().getProjectId()).isEqualTo(9L);
        assertThat(captor.getValue().getProjectKey()).isEqualTo("growth");
        assertThat(captor.getValue().getProjectName()).isNull();
        assertThat(captor.getValue().getFolderKey()).isEqualTo("new-user");
    }

    @Test
    void saveMetadataWithProjectIdCopiesProjectKeyAndName() {
        CanvasProjectFolderMapper mapper = mock(CanvasProjectFolderMapper.class);
        CanvasProjectMapper projectMapper = mock(CanvasProjectMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);
        CanvasProjectDO project = new CanvasProjectDO();
        project.setId(9L);
        project.setTenantId(1L);
        project.setProjectKey("growth");
        project.setProjectName("Growth");
        when(projectMapper.selectOne(any())).thenReturn(project);
        CanvasProjectFolderMetadataService service = new CanvasProjectFolderMetadataService(mapper, projectMapper);

        var resp = service.saveMetadata(1L, 62L, new ProjectFolderMetadataReq(
                9L, "ignored", "Ignored", "new-user", "New User", "alice"));

        ArgumentCaptor<CanvasProjectFolderDO> captor = ArgumentCaptor.forClass(CanvasProjectFolderDO.class);
        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(1L);
        assertThat(captor.getValue().getProjectId()).isEqualTo(9L);
        assertThat(captor.getValue().getProjectKey()).isEqualTo("growth");
        assertThat(captor.getValue().getProjectName()).isEqualTo("Growth");
        assertThat(resp.projectKey()).isEqualTo("growth");
        assertThat(resp.projectName()).isEqualTo("Growth");
    }

    @Test
    void getMetadataReturnsEmptyShapeWhenMissing() {
        CanvasProjectFolderMapper mapper = mock(CanvasProjectFolderMapper.class);
        CanvasProjectMapper projectMapper = mock(CanvasProjectMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);
        CanvasProjectFolderMetadataService service = new CanvasProjectFolderMetadataService(mapper, projectMapper);

        var resp = service.getMetadata(62L);

        assertThat(resp.canvasId()).isEqualTo(62L);
        assertThat(resp.projectId()).isNull();
        assertThat(resp.projectKey()).isNull();
        assertThat(resp.folderKey()).isNull();
    }
}
