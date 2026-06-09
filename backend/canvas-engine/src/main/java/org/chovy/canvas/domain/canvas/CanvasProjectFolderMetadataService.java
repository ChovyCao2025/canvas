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

/**
 * CanvasProjectFolderMetadataService 编排 domain.canvas 场景的领域业务规则。
 */
@Service
@RequiredArgsConstructor
public class CanvasProjectFolderMetadataService {

    private final CanvasProjectFolderMapper mapper;
    private final CanvasProjectMapper projectMapper;

    /**
     * 查询 Canvas 的项目/文件夹元数据。
     * 该兼容入口不附加租户过滤，主要用于旧调用方；未找到记录时返回空元数据响应。
     */
    public ProjectFolderMetadataResp getMetadata(Long canvasId) {
        return getMetadata(null, canvasId);
    }

    /**
     * 在租户范围内查询 Canvas 项目/文件夹元数据。
     * 返回值包含项目 ID、项目 key/name 和文件夹 key/name，未绑定时返回字段为空的响应。
     */
    public ProjectFolderMetadataResp getMetadata(Long tenantId, Long canvasId) {
        CanvasProjectFolderDO row = selectByCanvasId(tenantId, canvasId);
        if (row == null) {
            return new ProjectFolderMetadataResp(canvasId, null, null, null, null, null);
        }
        return response(row);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 保存 Canvas 项目/文件夹元数据，兼容无租户过滤的旧入口。
     */
    public ProjectFolderMetadataResp saveMetadata(Long canvasId, ProjectFolderMetadataReq req) {
        return saveMetadata(null, canvasId, req);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 在租户范围内新增或更新 Canvas 项目/文件夹元数据。
     * 如果传入 projectId 能解析到租户内项目，会使用项目表中的 key/name 覆盖请求值；操作人写入 updatedBy。
     */
    public ProjectFolderMetadataResp saveMetadata(Long tenantId, Long canvasId, ProjectFolderMetadataReq req) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        CanvasProjectFolderDO row = selectByCanvasId(tenantId, canvasId);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (row == null) {
            row = new CanvasProjectFolderDO();
            row.setCanvasId(canvasId);
            apply(row, tenantId, req);
            mapper.insert(row);
        } else {
            apply(row, tenantId, req);
            mapper.updateById(row);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return response(row);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @return 返回符合条件的数据列表或视图。
     */
    private CanvasProjectFolderDO selectByCanvasId(Long canvasId) {
        return selectByCanvasId(null, canvasId);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @return 返回符合条件的数据列表或视图。
     */
    private CanvasProjectFolderDO selectByCanvasId(Long tenantId, Long canvasId) {
        return mapper.selectOne(new LambdaQueryWrapper<CanvasProjectFolderDO>()
                .eq(tenantId != null, CanvasProjectFolderDO::getTenantId, tenantId)
                .eq(CanvasProjectFolderDO::getCanvasId, canvasId)
                .last("LIMIT 1"));
    }

    /**
     * 应用请求中的业务字段或租户约束。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param req 请求对象，承载本次操作的输入参数。
     */
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

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 response 流程生成的业务结果。
     */
    private ProjectFolderMetadataResp response(CanvasProjectFolderDO row) {
        return new ProjectFolderMetadataResp(
                row.getCanvasId(),
                row.getProjectId(),
                row.getProjectKey(),
                row.getProjectName(),
                row.getFolderKey(),
                row.getFolderName());
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param projectId 业务对象 ID，用于定位具体记录。
     * @return 返回符合条件的数据列表或视图。
     */
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

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
