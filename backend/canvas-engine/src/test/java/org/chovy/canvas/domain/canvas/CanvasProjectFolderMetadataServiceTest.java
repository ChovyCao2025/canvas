package org.chovy.canvas.domain.canvas;

import org.chovy.canvas.dal.dataobject.CanvasProjectFolderDO;
import org.chovy.canvas.dal.mapper.CanvasProjectFolderMapper;
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
        when(mapper.selectOne(any())).thenReturn(null);
        CanvasProjectFolderMetadataService service = new CanvasProjectFolderMetadataService(mapper);

        var resp = service.saveMetadata(62L, new ProjectFolderMetadataReq(
                " growth ", " Growth ", " new-user ", " New User ", " alice "));

        ArgumentCaptor<CanvasProjectFolderDO> captor = ArgumentCaptor.forClass(CanvasProjectFolderDO.class);
        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getCanvasId()).isEqualTo(62L);
        assertThat(captor.getValue().getProjectKey()).isEqualTo("growth");
        assertThat(captor.getValue().getFolderKey()).isEqualTo("new-user");
        assertThat(captor.getValue().getUpdatedBy()).isEqualTo("alice");
        assertThat(resp.projectName()).isEqualTo("Growth");
        assertThat(resp.folderName()).isEqualTo("New User");
    }

    @Test
    void saveMetadataUpdatesExistingRow() {
        CanvasProjectFolderMapper mapper = mock(CanvasProjectFolderMapper.class);
        CanvasProjectFolderDO existing = new CanvasProjectFolderDO();
        existing.setId(5L);
        existing.setCanvasId(62L);
        when(mapper.selectOne(any())).thenReturn(existing);
        CanvasProjectFolderMetadataService service = new CanvasProjectFolderMetadataService(mapper);

        service.saveMetadata(62L, new ProjectFolderMetadataReq("growth", null, "new-user", null, "alice"));

        ArgumentCaptor<CanvasProjectFolderDO> captor = ArgumentCaptor.forClass(CanvasProjectFolderDO.class);
        verify(mapper).updateById(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(5L);
        assertThat(captor.getValue().getProjectKey()).isEqualTo("growth");
        assertThat(captor.getValue().getProjectName()).isNull();
        assertThat(captor.getValue().getFolderKey()).isEqualTo("new-user");
    }

    @Test
    void getMetadataReturnsEmptyShapeWhenMissing() {
        CanvasProjectFolderMapper mapper = mock(CanvasProjectFolderMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);
        CanvasProjectFolderMetadataService service = new CanvasProjectFolderMetadataService(mapper);

        var resp = service.getMetadata(62L);

        assertThat(resp.canvasId()).isEqualTo(62L);
        assertThat(resp.projectKey()).isNull();
        assertThat(resp.folderKey()).isNull();
    }
}
