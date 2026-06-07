package org.chovy.canvas.domain.project;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasProjectDO;
import org.chovy.canvas.dal.dataobject.CanvasProjectFolderDO;
import org.chovy.canvas.dal.dataobject.CanvasProjectMemberDO;
import org.chovy.canvas.dal.mapper.CanvasProjectFolderMapper;
import org.chovy.canvas.dal.mapper.CanvasProjectMapper;
import org.chovy.canvas.dal.mapper.CanvasProjectMemberMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CanvasProjectPermissionServiceTest {

    @Test
    void editorCanEditButExecutorCannotEditProjectCanvas() {
        CanvasProjectPermissionService service = serviceWithMember("alice", "EDITOR", "ACTIVE");
        assertThatCode(() -> service.requireCanvasAction(canvas(9L), context("alice"), CanvasProjectAction.EDIT))
                .doesNotThrowAnyException();

        CanvasProjectPermissionService executorService = serviceWithMember("alice", "EXECUTOR", "ACTIVE");
        assertThatThrownBy(() -> executorService.requireCanvasAction(canvas(9L), context("alice"), CanvasProjectAction.EDIT))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void executorCanExecuteButViewerCannotExecuteProjectCanvas() {
        CanvasProjectPermissionService service = serviceWithMember("alice", "EXECUTOR", "ACTIVE");
        assertThatCode(() -> service.requireCanvasAction(canvas(9L), context("alice"), CanvasProjectAction.EXECUTE))
                .doesNotThrowAnyException();

        CanvasProjectPermissionService viewerService = serviceWithMember("alice", "VIEWER", "ACTIVE");
        assertThatThrownBy(() -> viewerService.requireCanvasAction(canvas(9L), context("alice"), CanvasProjectAction.EXECUTE))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void disabledProjectAllowsReadButDeniesEdit() {
        CanvasProjectPermissionService service = serviceWithMember("alice", "PROJECT_ADMIN", "DISABLED");

        assertThatCode(() -> service.requireCanvasAction(canvas(9L), context("alice"), CanvasProjectAction.READ))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> service.requireCanvasAction(canvas(9L), context("alice"), CanvasProjectAction.EDIT))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    void tenantAdminBypassesProjectMembershipForActiveProject() {
        CanvasProjectPermissionService service = serviceWithMember("alice", "VIEWER", "ACTIVE");

        assertThatCode(() -> service.requireCanvasAction(
                canvas(9L),
                new TenantContext(9L, RoleNames.TENANT_ADMIN, "tenant-admin"),
                CanvasProjectAction.MANAGE_MEMBERS))
                .doesNotThrowAnyException();
    }

    @Test
    void tenantAdminCannotManageDisabledProject() {
        CanvasProjectPermissionService service = serviceWithMember("alice", "VIEWER", "DISABLED");

        assertThatThrownBy(() -> service.requireCanvasAction(
                canvas(9L),
                new TenantContext(9L, RoleNames.TENANT_ADMIN, "tenant-admin"),
                CanvasProjectAction.MANAGE_MEMBERS))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    void unassignedCanvasAllowsSameTenantAuthenticatedUser() {
        CanvasProjectPermissionService service = serviceWithoutAssignment();

        assertThatCode(() -> service.requireCanvasAction(canvas(9L), context("alice"), CanvasProjectAction.EDIT))
                .doesNotThrowAnyException();
    }

    private CanvasProjectPermissionService serviceWithMember(String username, String role, String projectStatus) {
        CanvasProjectFolderMapper folderMapper = mock(CanvasProjectFolderMapper.class);
        CanvasProjectMapper projectMapper = mock(CanvasProjectMapper.class);
        CanvasProjectMemberMapper memberMapper = mock(CanvasProjectMemberMapper.class);

        CanvasProjectFolderDO folder = new CanvasProjectFolderDO();
        folder.setTenantId(9L);
        folder.setCanvasId(62L);
        folder.setProjectId(11L);
        when(folderMapper.selectOne(any())).thenReturn(folder);

        CanvasProjectDO project = new CanvasProjectDO();
        project.setId(11L);
        project.setTenantId(9L);
        project.setStatus(projectStatus);
        when(projectMapper.selectOne(any())).thenReturn(project);

        CanvasProjectMemberDO member = new CanvasProjectMemberDO();
        member.setTenantId(9L);
        member.setProjectId(11L);
        member.setUsername(username);
        member.setRole(role);
        when(memberMapper.selectOne(any())).thenReturn(member);

        return new CanvasProjectPermissionService(folderMapper, projectMapper, memberMapper);
    }

    private CanvasProjectPermissionService serviceWithoutAssignment() {
        CanvasProjectFolderMapper folderMapper = mock(CanvasProjectFolderMapper.class);
        when(folderMapper.selectOne(any())).thenReturn(null);
        return new CanvasProjectPermissionService(
                folderMapper,
                mock(CanvasProjectMapper.class),
                mock(CanvasProjectMemberMapper.class));
    }

    private CanvasDO canvas(Long tenantId) {
        CanvasDO canvas = new CanvasDO();
        canvas.setId(62L);
        canvas.setTenantId(tenantId);
        return canvas;
    }

    private TenantContext context(String username) {
        return new TenantContext(9L, RoleNames.OPERATOR, username);
    }
}
