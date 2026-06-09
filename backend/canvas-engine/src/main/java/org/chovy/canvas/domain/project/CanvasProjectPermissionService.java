package org.chovy.canvas.domain.project;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasProjectDO;
import org.chovy.canvas.dal.dataobject.CanvasProjectFolderDO;
import org.chovy.canvas.dal.dataobject.CanvasProjectMemberDO;
import org.chovy.canvas.dal.mapper.CanvasProjectFolderMapper;
import org.chovy.canvas.dal.mapper.CanvasProjectMapper;
import org.chovy.canvas.dal.mapper.CanvasProjectMemberMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Set;

/**
 * CanvasProjectPermissionService 编排 domain.project 场景的领域业务规则。
 */
@Service
@RequiredArgsConstructor
public class CanvasProjectPermissionService {

    private static final String DISABLED = "DISABLED";
    private static final Set<CanvasProjectAction> DISABLED_PROJECT_ALLOWED_ACTIONS =
            EnumSet.of(CanvasProjectAction.READ, CanvasProjectAction.EXECUTE);

    private final CanvasProjectFolderMapper folderMapper;
    private final CanvasProjectMapper projectMapper;
    private final CanvasProjectMemberMapper memberMapper;

    /**
     * 校验当前用户是否可对 Canvas 执行指定项目动作。
     * 会先校验租户边界，再根据画布项目归属、项目状态、管理员身份和项目成员角色做权限判断。
     */
    public void requireCanvasAction(CanvasDO canvas, TenantContext context, CanvasProjectAction action) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (canvas == null) {
            throw new AccessDeniedException("Canvas is required");
        }
        if (context == null || context.username() == null || context.username().isBlank()) {
            throw new AccessDeniedException("Authenticated user is required");
        }
        if (!context.isSuperAdmin() && !sameTenant(canvas.getTenantId(), context.tenantId())) {
            throw new AccessDeniedException("Canvas tenant access denied");
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        CanvasProjectFolderDO assignment = selectAssignment(canvas);
        if (assignment == null || assignment.getProjectId() == null) {
            return;
        }

        CanvasProjectDO project = requireProject(canvas.getTenantId(), assignment.getProjectId());
        if (DISABLED.equalsIgnoreCase(project.getStatus()) && !DISABLED_PROJECT_ALLOWED_ACTIONS.contains(action)) {
            throw new AccessDeniedException("Project is disabled for action: " + action);
        }
        if (context.isSuperAdmin() || context.isTenantAdmin()) {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }

        CanvasProjectMemberDO member = selectMember(canvas.getTenantId(), assignment.getProjectId(), context.username());
        if (member == null) {
            throw new AccessDeniedException("User is not a project member");
        }
        CanvasProjectRole role = CanvasProjectRole.parse(member.getRole());
        if (!allowed(role, action)) {
            throw new AccessDeniedException("Project role " + role + " cannot perform action " + action);
        }
    }

    /**
     * 校验当前用户是否可直接操作指定项目。
     * 超级管理员和租户管理员放行，普通用户必须是项目成员且角色允许该动作。
     */
    public void requireProjectAction(Long tenantId, Long projectId, TenantContext context, CanvasProjectAction action) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (context == null || context.username() == null || context.username().isBlank()) {
            throw new AccessDeniedException("Authenticated user is required");
        }
        if (!context.isSuperAdmin() && !sameTenant(tenantId, context.tenantId())) {
            throw new AccessDeniedException("Project tenant access denied");
        }
        CanvasProjectDO project = requireProject(tenantId, projectId);
        if (DISABLED.equalsIgnoreCase(project.getStatus()) && !DISABLED_PROJECT_ALLOWED_ACTIONS.contains(action)) {
            throw new AccessDeniedException("Project is disabled for action: " + action);
        }
        if (context.isSuperAdmin() || context.isTenantAdmin()) {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }

        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        CanvasProjectMemberDO member = selectMember(tenantId, projectId, context.username());
        if (member == null) {
            throw new AccessDeniedException("User is not a project member");
        }
        CanvasProjectRole role = CanvasProjectRole.parse(member.getRole());
        if (!allowed(role, action)) {
            throw new AccessDeniedException("Project role " + role + " cannot perform action " + action);
        }
    }

    /**
     * 执行 allowed 流程，围绕 allowed 完成校验、计算或结果组装。
     *
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param action action 参数，用于 allowed 流程中的校验、计算或对象转换。
     * @return 返回 allowed 的布尔判断结果。
     */
    private boolean allowed(CanvasProjectRole role, CanvasProjectAction action) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return switch (role) {
            case PROJECT_ADMIN -> true;
            case EDITOR -> EnumSet.of(
                    CanvasProjectAction.READ,
                    CanvasProjectAction.EDIT,
                    // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                    CanvasProjectAction.PUBLISH,
                    CanvasProjectAction.EXECUTE).contains(action);
            case EXECUTOR -> EnumSet.of(CanvasProjectAction.READ, CanvasProjectAction.EXECUTE).contains(action);
            case VIEWER -> CanvasProjectAction.READ == action;
        };
    }

    /**
     * 处理安全、签名或敏感信息逻辑。
     *
     * @param canvas canvas 参数，用于 selectAssignment 流程中的校验、计算或对象转换。
     * @return 返回符合条件的数据列表或视图。
     */
    private CanvasProjectFolderDO selectAssignment(CanvasDO canvas) {
        return folderMapper.selectOne(new LambdaQueryWrapper<CanvasProjectFolderDO>()
                .eq(CanvasProjectFolderDO::getTenantId, canvas.getTenantId())
                .eq(CanvasProjectFolderDO::getCanvasId, canvas.getId())
                .last("LIMIT 1"));
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param projectId 业务对象 ID，用于定位具体记录。
     * @return 返回 requireProject 流程生成的业务结果。
     */
    private CanvasProjectDO requireProject(Long tenantId, Long projectId) {
        CanvasProjectDO project = projectMapper.selectOne(new LambdaQueryWrapper<CanvasProjectDO>()
                .eq(CanvasProjectDO::getTenantId, tenantId)
                .eq(CanvasProjectDO::getId, projectId)
                .last("LIMIT 1"));
        if (project == null) {
            throw new AccessDeniedException("Project not found: " + projectId);
        }
        return project;
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param projectId 业务对象 ID，用于定位具体记录。
     * @param username 操作人标识，用于审计和权限判断。
     * @return 返回符合条件的数据列表或视图。
     */
    private CanvasProjectMemberDO selectMember(Long tenantId, Long projectId, String username) {
        return memberMapper.selectOne(new LambdaQueryWrapper<CanvasProjectMemberDO>()
                .eq(CanvasProjectMemberDO::getTenantId, tenantId)
                .eq(CanvasProjectMemberDO::getProjectId, projectId)
                .eq(CanvasProjectMemberDO::getUsername, username.trim())
                .last("LIMIT 1"));
    }

    /**
     * 执行 sameTenant 流程，围绕 same tenant 完成校验、计算或结果组装。
     *
     * @param expected 待处理业务值，用于规则计算、转换或外部调用。
     * @param actual actual 参数，用于 sameTenant 流程中的校验、计算或对象转换。
     * @return 返回 same tenant 的布尔判断结果。
     */
    private boolean sameTenant(Long expected, Long actual) {
        return expected != null && expected.equals(actual);
    }
}
