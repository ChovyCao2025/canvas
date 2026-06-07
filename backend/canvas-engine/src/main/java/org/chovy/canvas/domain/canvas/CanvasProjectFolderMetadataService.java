package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.dal.dataobject.CanvasProjectDO;
import org.chovy.canvas.dal.dataobject.CanvasProjectFolderDO;
import org.chovy.canvas.dal.mapper.CanvasProjectFolderMapper;
import org.chovy.canvas.dal.mapper.CanvasProjectMapper;
import org.chovy.canvas.dto.canvas.ProjectFolderMetadataReq;
import org.chovy.canvas.dto.canvas.ProjectFolderMetadataResp;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CanvasProjectFolderMetadataService {

    private final CanvasProjectFolderMapper mapper;
    private final CanvasProjectMapper projectMapper;

    public ProjectFolderMetadataResp getMetadata(Long canvasId) {
        return getMetadata(null, canvasId);
    }

    public ProjectFolderMetadataResp getMetadata(Long tenantId, Long canvasId) {
        CanvasProjectFolderDO row = selectByCanvasId(tenantId, canvasId);
        if (row == null) {
            return new ProjectFolderMetadataResp(canvasId, null, null, null, null, null);
        }
        return response(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public ProjectFolderMetadataResp saveMetadata(Long canvasId, ProjectFolderMetadataReq req) {
        return saveMetadata(null, canvasId, req);
    }

    @Transactional(rollbackFor = Exception.class)
    public ProjectFolderMetadataResp saveMetadata(Long tenantId, Long canvasId, ProjectFolderMetadataReq req) {
        CanvasProjectFolderDO row = selectByCanvasId(tenantId, canvasId);
        if (row == null) {
            row = new CanvasProjectFolderDO();
            row.setCanvasId(canvasId);
            apply(row, tenantId, req);
            mapper.insert(row);
        } else {
            apply(row, tenantId, req);
            mapper.updateById(row);
        }
        return response(row);
    }

    private CanvasProjectFolderDO selectByCanvasId(Long canvasId) {
        return selectByCanvasId(null, canvasId);
    }

    private CanvasProjectFolderDO selectByCanvasId(Long tenantId, Long canvasId) {
        return mapper.selectOne(new LambdaQueryWrapper<CanvasProjectFolderDO>()
                .eq(tenantId != null, CanvasProjectFolderDO::getTenantId, tenantId)
                .eq(CanvasProjectFolderDO::getCanvasId, canvasId)
                .last("LIMIT 1"));
    }

    private void apply(CanvasProjectFolderDO row, Long tenantId, ProjectFolderMetadataReq req) {
        row.setTenantId(tenantId);
        CanvasProjectDO project = selectProject(tenantId, req.projectId());
        row.setProjectId(project == null ? req.projectId() : project.getId());
        row.setProjectKey(project == null ? normalize(req.projectKey()) : project.getProjectKey());
        row.setProjectName(project == null ? normalize(req.projectName()) : project.getProjectName());
        row.setFolderKey(normalize(req.folderKey()));
        row.setFolderName(normalize(req.folderName()));
        row.setUpdatedBy(normalize(req.operator()));
    }

    private ProjectFolderMetadataResp response(CanvasProjectFolderDO row) {
        return new ProjectFolderMetadataResp(
                row.getCanvasId(),
                row.getProjectId(),
                row.getProjectKey(),
                row.getProjectName(),
                row.getFolderKey(),
                row.getFolderName());
    }

    private CanvasProjectDO selectProject(Long tenantId, Long projectId) {
        if (tenantId == null || projectId == null) {
            return null;
        }
        CanvasProjectDO project = projectMapper.selectOne(new LambdaQueryWrapper<CanvasProjectDO>()
                .eq(CanvasProjectDO::getTenantId, tenantId)
                .eq(CanvasProjectDO::getId, projectId)
                .last("LIMIT 1"));
        return project;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
