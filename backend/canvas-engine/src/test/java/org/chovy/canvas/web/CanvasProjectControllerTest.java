package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.canvas.CanvasService;
import org.chovy.canvas.domain.project.CanvasProjectPermissionService;
import org.chovy.canvas.domain.project.CanvasProjectService;
import org.chovy.canvas.dto.project.ProjectCreateReq;
import org.chovy.canvas.dto.project.ProjectDetailResp;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasProjectControllerTest {

    @Test
    void createDelegatesToServiceWithCurrentTenant() {
        TenantContextResolver tenantResolver = mock(TenantContextResolver.class);
        CanvasProjectService projectService = mock(CanvasProjectService.class);
        CanvasProjectController controller = new CanvasProjectController(
                tenantResolver,
                projectService,
                mock(CanvasProjectPermissionService.class),
                mock(CanvasService.class));
        when(tenantResolver.current()).thenReturn(Mono.just(new TenantContext(
                9L, RoleNames.TENANT_ADMIN, "alice")));
        ProjectDetailResp resp = new ProjectDetailResp(
                11L, 9L, "growth", "Growth", null, "ACTIVE", null, 0, null);
        when(projectService.create(org.mockito.ArgumentMatchers.eq(9L),
                org.mockito.ArgumentMatchers.any(ProjectCreateReq.class))).thenReturn(resp);

        StepVerifier.create(controller.create(new ProjectCreateReq(
                        " growth ", "Growth", null, null, 0, null, "ignored")))
                .assertNext(response -> assertThat(response.getData()).isSameAs(resp))
                .verifyComplete();

        ArgumentCaptor<ProjectCreateReq> captor = ArgumentCaptor.forClass(ProjectCreateReq.class);
        verify(projectService).create(org.mockito.ArgumentMatchers.eq(9L), captor.capture());
        assertThat(captor.getValue().operator()).isEqualTo("alice");
    }
}
