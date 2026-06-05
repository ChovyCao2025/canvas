package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.dal.dataobject.CanvasProjectFolderDO;
import org.chovy.canvas.dal.mapper.CanvasProjectFolderMapper;
import org.chovy.canvas.dto.canvas.ProjectFolderMetadataReq;
import org.chovy.canvas.dto.canvas.ProjectFolderMetadataResp;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CanvasProjectFolderMetadataService {

    private final CanvasProjectFolderMapper mapper;

    public ProjectFolderMetadataResp getMetadata(Long canvasId) {
        CanvasProjectFolderDO row = selectByCanvasId(canvasId);
        if (row == null) {
            return new ProjectFolderMetadataResp(canvasId, null, null, null, null);
        }
        return response(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public ProjectFolderMetadataResp saveMetadata(Long canvasId, ProjectFolderMetadataReq req) {
        CanvasProjectFolderDO row = selectByCanvasId(canvasId);
        if (row == null) {
            row = new CanvasProjectFolderDO();
            row.setCanvasId(canvasId);
            apply(row, req);
            mapper.insert(row);
        } else {
            apply(row, req);
            mapper.updateById(row);
        }
        return response(row);
    }

    private CanvasProjectFolderDO selectByCanvasId(Long canvasId) {
        return mapper.selectOne(new LambdaQueryWrapper<CanvasProjectFolderDO>()
                .eq(CanvasProjectFolderDO::getCanvasId, canvasId)
                .last("LIMIT 1"));
    }

    private void apply(CanvasProjectFolderDO row, ProjectFolderMetadataReq req) {
        row.setProjectKey(normalize(req.projectKey()));
        row.setProjectName(normalize(req.projectName()));
        row.setFolderKey(normalize(req.folderKey()));
        row.setFolderName(normalize(req.folderName()));
        row.setUpdatedBy(normalize(req.operator()));
    }

    private ProjectFolderMetadataResp response(CanvasProjectFolderDO row) {
        return new ProjectFolderMetadataResp(
                row.getCanvasId(),
                row.getProjectKey(),
                row.getProjectName(),
                row.getFolderKey(),
                row.getFolderName());
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
