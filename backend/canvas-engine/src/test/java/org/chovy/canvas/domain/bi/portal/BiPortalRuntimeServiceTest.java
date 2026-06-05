package org.chovy.canvas.domain.bi.portal;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.domain.bi.permission.BiPermissionService;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BiPortalRuntimeServiceTest {

    @Test
    void listPublishedFiltersDraftsAndInvisibleMenus() {
        BiPortalResourceService resourceService = mock(BiPortalResourceService.class);
        when(resourceService.list(7L)).thenReturn(List.of(portal("DRAFT"), portal("PUBLISHED")));
        BiPortalRuntimeService service = new BiPortalRuntimeService(resourceService, permissionService());

        List<BiPortalResource> portals = service.listPublished(
                7L,
                new BiQueryContext(7L, "alice", "OPERATOR"));

        assertThat(portals).singleElement()
                .satisfies(portal -> {
                    assertThat(portal.status()).isEqualTo("PUBLISHED");
                    assertThat(portal.menus()).extracting(BiPortalMenuResource::menuKey)
                            .containsExactly("overview", "open-link");
                });
    }

    @Test
    void getPublishedRejectsDraftPortal() {
        BiPortalResourceService resourceService = mock(BiPortalResourceService.class);
        when(resourceService.get(7L, "canvas-ops-portal")).thenReturn(portal("DRAFT"));
        BiPortalRuntimeService service = new BiPortalRuntimeService(resourceService, permissionService());

        assertThatThrownBy(() -> service.getPublished(
                7L,
                "canvas-ops-portal",
                new BiQueryContext(7L, "alice", "OPERATOR")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not published");
    }

    @Test
    void getPublishedReturnsOnlyVisibleMenus() {
        BiPortalResourceService resourceService = mock(BiPortalResourceService.class);
        when(resourceService.get(7L, "canvas-ops-portal")).thenReturn(portal("PUBLISHED"));
        BiPortalRuntimeService service = new BiPortalRuntimeService(resourceService, permissionService());

        BiPortalResource portal = service.getPublished(
                7L,
                "canvas-ops-portal",
                new BiQueryContext(7L, "bob", "TENANT_ADMIN"));

        assertThat(portal.menus()).extracting(BiPortalMenuResource::menuKey)
                .containsExactly("tenant-admin", "open-link");
    }

    private BiPortalResource portal(String status) {
        return new BiPortalResource(
                "canvas-ops-portal",
                "Canvas Operations Portal",
                Map.of("theme", "light"),
                List.of(
                        new BiPortalMenuResource(
                                "overview",
                                null,
                                "经营总览",
                                "DASHBOARD",
                                "canvas-effect",
                                21L,
                                null,
                                Map.of("roles", List.of("OPERATOR")),
                                10),
                        new BiPortalMenuResource(
                                "tenant-admin",
                                null,
                                "管理员分析",
                                "DASHBOARD",
                                "admin-effect",
                                22L,
                                null,
                                Map.of("roles", List.of("TENANT_ADMIN")),
                                20),
                        new BiPortalMenuResource(
                                "blocked-user",
                                null,
                                "禁用菜单",
                                "DASHBOARD",
                                "blocked-effect",
                                23L,
                                null,
                                Map.of("denyUsers", List.of("alice", "bob")),
                                30),
                        new BiPortalMenuResource(
                                "open-link",
                                null,
                                "画布列表",
                                "EXTERNAL_LINK",
                                null,
                                null,
                                "/canvas",
                                Map.of(),
                                40)),
                status,
                "PERSISTED");
    }

    private BiPermissionService permissionService() {
        return new BiPermissionService(null, null, null, null, null, new ObjectMapper());
    }
}
