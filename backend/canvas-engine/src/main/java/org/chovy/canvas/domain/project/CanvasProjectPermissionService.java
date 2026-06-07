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

@Service
@RequiredArgsConstructor
public class CanvasProjectPermissionService {

    private static final String DISABLED = "DISABLED";
    private static final Set<CanvasProjectAction> DISABLED_PROJECT_ALLOWED_ACTIONS =
            EnumSet.of(CanvasProjectAction.READ, CanvasProjectAction.EXECUTE);

    private final CanvasProjectFolderMapper folderMapper;
    private final CanvasProjectMapper projectMapper;
    private final CanvasProjectMemberMapper memberMapper;

    public void requireCanvasAction(CanvasDO canvas, TenantContext context, CanvasProjectAction action) {
        if (canvas == null) {
            throw new AccessDeniedException("Canvas is required");
        }
        if (context == null || context.username() == null || context.username().isBlank()) {
            throw new AccessDeniedException("Authenticated user is required");
        }
        if (!context.isSuperAdmin() && !sameTenant(canvas.getTenantId(), context.tenantId())) {
            throw new AccessDeniedException("Canvas tenant access denied");
        }
        CanvasProjectFolderDO assignment = selectAssignment(canvas);
        if (assignment == null || assignment.getProjectId() == null) {
            return;
        }

        CanvasProjectDO project = requireProject(canvas.getTenantId(), assignment.getProjectId());
        if (DISABLED.equalsIgnoreCase(project.getStatus()) && !DISABLED_PROJECT_ALLOWED_ACTIONS.contains(action)) {
            throw new AccessDeniedException("Project is disabled for action: " + action);
        }
        if (context.isSuperAdmin() || context.isTenantAdmin()) {
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

    public void requireProjectAction(Long tenantId, Long projectId, TenantContext context, CanvasProjectAction action) {
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
            return;
        }

        CanvasProjectMemberDO member = selectMember(tenantId, projectId, context.username());
        if (member == null) {
            throw new AccessDeniedException("User is not a project member");
        }
        CanvasProjectRole role = CanvasProjectRole.parse(member.getRole());
        if (!allowed(role, action)) {
            throw new AccessDeniedException("Project role " + role + " cannot perform action " + action);
        }
    }

    private boolean allowed(CanvasProjectRole role, CanvasProjectAction action) {
        return switch (role) {
            case PROJECT_ADMIN -> true;
            case EDITOR -> EnumSet.of(
                    CanvasProjectAction.READ,
                    CanvasProjectAction.EDIT,
                    CanvasProjectAction.PUBLISH,
                    CanvasProjectAction.EXECUTE).contains(action);
            case EXECUTOR -> EnumSet.of(CanvasProjectAction.READ, CanvasProjectAction.EXECUTE).contains(action);
            case VIEWER -> CanvasProjectAction.READ == action;
        };
    }

    private CanvasProjectFolderDO selectAssignment(CanvasDO canvas) {
        return folderMapper.selectOne(new LambdaQueryWrapper<CanvasProjectFolderDO>()
                .eq(CanvasProjectFolderDO::getTenantId, canvas.getTenantId())
                .eq(CanvasProjectFolderDO::getCanvasId, canvas.getId())
                .last("LIMIT 1"));
    }

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

    private CanvasProjectMemberDO selectMember(Long tenantId, Long projectId, String username) {
        return memberMapper.selectOne(new LambdaQueryWrapper<CanvasProjectMemberDO>()
                .eq(CanvasProjectMemberDO::getTenantId, tenantId)
                .eq(CanvasProjectMemberDO::getProjectId, projectId)
                .eq(CanvasProjectMemberDO::getUsername, username.trim())
                .last("LIMIT 1"));
    }

    private boolean sameTenant(Long expected, Long actual) {
        return expected != null && expected.equals(actual);
    }
}
