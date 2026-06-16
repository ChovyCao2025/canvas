package org.chovy.canvas.canvas.adapter.persistence;

import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.canvas.application.CanvasProjectFolderRepository;
import org.chovy.canvas.canvas.application.ProjectFolderMetadata;
import org.springframework.stereotype.Repository;

/**
 * 封装MybatisCanvasProjectFolderRepository相关的业务逻辑。
 */
@Repository
public class MybatisCanvasProjectFolderRepository implements CanvasProjectFolderRepository {

    /**
     * 保存映射器。
     */
    private final CanvasProjectFolderMapper mapper;

    /**
     * 创建当前对象实例。
     */
    public MybatisCanvasProjectFolderRepository(CanvasProjectFolderMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 查询by tenant id and canvas标识。
     */
    @Override
    public Optional<ProjectFolderMetadata> findByTenantIdAndCanvasId(Long tenantId, Long canvasId) {
        CanvasProjectFolderDO row = mapper.selectOne(new LambdaQueryWrapper<CanvasProjectFolderDO>()
                .eq(tenantId != null, CanvasProjectFolderDO::getTenantId, tenantId)
                .eq(CanvasProjectFolderDO::getCanvasId, canvasId)
                .last("LIMIT 1"));
        return Optional.ofNullable(toDomain(row));
    }

    /**
     * 保存。
     */
    @Override
    public ProjectFolderMetadata save(ProjectFolderMetadata metadata) {
        CanvasProjectFolderDO existing = mapper.selectOne(new LambdaQueryWrapper<CanvasProjectFolderDO>()
                .eq(metadata.tenantId() != null, CanvasProjectFolderDO::getTenantId, metadata.tenantId())
                .eq(CanvasProjectFolderDO::getCanvasId, metadata.canvasId())
                .last("LIMIT 1"));
        CanvasProjectFolderDO row = existing == null ? new CanvasProjectFolderDO() : existing;
        row.setTenantId(metadata.tenantId());
        row.setCanvasId(metadata.canvasId());
        row.setProjectId(metadata.projectId());
        row.setProjectKey(metadata.projectKey());
        row.setProjectName(metadata.projectName());
        row.setFolderKey(metadata.folderKey());
        row.setFolderName(metadata.folderName());
        if (row.getId() == null) {
            int inserted = mapper.insert(row);
            if (inserted <= 0) {
                throw new IllegalStateException("Canvas project folder insert affected 0 rows");
            }
        } else {
            int updated = mapper.updateById(row);
            if (updated <= 0) {
                throw new IllegalStateException("Canvas project folder update affected 0 rows: " + row.getId());
            }
        }
        return toDomain(row);
    }

    /**
     * 转换为Domain。
     */
    private static ProjectFolderMetadata toDomain(CanvasProjectFolderDO row) {
        if (row == null) {
            return null;
        }
        return new ProjectFolderMetadata(
                row.getCanvasId(),
                row.getTenantId(),
                row.getProjectId(),
                row.getProjectKey(),
                row.getProjectName(),
                row.getFolderKey(),
                row.getFolderName());
    }
}
