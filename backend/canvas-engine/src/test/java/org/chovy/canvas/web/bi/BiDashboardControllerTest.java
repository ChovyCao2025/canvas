package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardCloneCommand;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardExportPackage;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardImportCommand;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardResource;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardResourceService;
import org.chovy.canvas.domain.bi.dashboard.MarketingBiDashboardPresetRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiDashboardControllerTest {

    @Test
    void saveDraftUsesCurrentTenantUserAndLockToken() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiDashboardResourceService service = mock(BiDashboardResourceService.class);
        var preset = MarketingBiDashboardPresetRegistry.preset("canvas-effect");
        var resource = new BiDashboardResource(preset, "DRAFT", 1, "PERSISTED");
        when(service.saveDraft(7L, "alice", "TENANT_ADMIN", preset, "lock-token-1")).thenReturn(resource);
        BiDashboardController controller = new BiDashboardController(resolver, service);

        StepVerifier.create(controller.saveDraft("canvas-effect", "lock-token-1", preset))
                .assertNext(response -> {
                    assertThat(response.getData().status()).isEqualTo("DRAFT");
                    assertThat(response.getData().source()).isEqualTo("PERSISTED");
                })
                .verifyComplete();

        verify(service).saveDraft(7L, "alice", "TENANT_ADMIN", preset, "lock-token-1");
    }

    @Test
    void publishUsesCurrentTenant() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiDashboardResourceService service = mock(BiDashboardResourceService.class);
        var preset = MarketingBiDashboardPresetRegistry.preset("canvas-effect");
        var resource = new BiDashboardResource(preset, "PUBLISHED", 2, "PERSISTED");
        when(service.publish(7L, "alice", "TENANT_ADMIN", "canvas-effect")).thenReturn(resource);
        BiDashboardController controller = new BiDashboardController(resolver, service);

        StepVerifier.create(controller.publish("canvas-effect"))
                .assertNext(response -> {
                    assertThat(response.getData().status()).isEqualTo("PUBLISHED");
                    assertThat(response.getData().version()).isEqualTo(2);
                })
                .verifyComplete();

        verify(service).publish(7L, "alice", "TENANT_ADMIN", "canvas-effect");
    }

    @Test
    void cloneUsesCurrentTenantAndUser() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiDashboardResourceService service = mock(BiDashboardResourceService.class);
        var preset = MarketingBiDashboardPresetRegistry.preset("canvas-effect");
        var command = new BiDashboardCloneCommand("canvas-effect-copy", "画布效果分析 副本", "copy");
        var resource = new BiDashboardResource(preset, "DRAFT", 1, "PERSISTED");
        when(service.cloneResource(7L, "alice", "canvas-effect", command)).thenReturn(resource);
        BiDashboardController controller = new BiDashboardController(resolver, service);

        StepVerifier.create(controller.clone("canvas-effect", command))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("DRAFT"))
                .verifyComplete();

        verify(service).cloneResource(7L, "alice", "canvas-effect", command);
    }

    @Test
    void archiveUsesCurrentTenant() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiDashboardResourceService service = mock(BiDashboardResourceService.class);
        var preset = MarketingBiDashboardPresetRegistry.preset("canvas-effect");
        var resource = new BiDashboardResource(preset, "ARCHIVED", 2, "PERSISTED");
        when(service.archive(7L, "canvas-effect")).thenReturn(resource);
        BiDashboardController controller = new BiDashboardController(resolver, service);

        StepVerifier.create(controller.archive("canvas-effect"))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("ARCHIVED"))
                .verifyComplete();

        verify(service).archive(7L, "canvas-effect");
    }

    @Test
    void exportPackageUsesCurrentTenantAndUser() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiDashboardResourceService service = mock(BiDashboardResourceService.class);
        var preset = MarketingBiDashboardPresetRegistry.preset("canvas-effect");
        var exported = new BiDashboardExportPackage(
                "DASHBOARD",
                1,
                "canvas-effect",
                2,
                preset,
                "alice",
                java.time.LocalDateTime.parse("2026-06-05T09:30:00"));
        when(service.exportResource(7L, "alice", "canvas-effect")).thenReturn(exported);
        BiDashboardController controller = new BiDashboardController(resolver, service);

        StepVerifier.create(controller.exportPackage("canvas-effect"))
                .assertNext(response -> {
                    assertThat(response.getData().resourceType()).isEqualTo("DASHBOARD");
                    assertThat(response.getData().sourceVersion()).isEqualTo(2);
                })
                .verifyComplete();

        verify(service).exportResource(7L, "alice", "canvas-effect");
    }

    @Test
    void exportPackageFileDownloadsJsonWithFilename() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiDashboardResourceService service = mock(BiDashboardResourceService.class);
        byte[] content = "{\"resourceType\":\"DASHBOARD\"}".getBytes(StandardCharsets.UTF_8);
        var file = new BiDashboardResourceService.DashboardPackageFile(
                "canvas-effect-v2.bi-dashboard.json",
                "application/json",
                content);
        when(service.exportResourceFile(7L, "alice", "canvas-effect")).thenReturn(file);
        BiDashboardController controller = new BiDashboardController(resolver, service);

        StepVerifier.create(controller.exportPackageFile("canvas-effect"))
                .assertNext(response -> {
                    assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                            .contains("canvas-effect-v2.bi-dashboard.json");
                    assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
                    assertThat(response.getBody()).isEqualTo(content);
                })
                .verifyComplete();

        verify(service).exportResourceFile(7L, "alice", "canvas-effect");
    }

    @Test
    void importPackageUsesCurrentTenantAndUser() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiDashboardResourceService service = mock(BiDashboardResourceService.class);
        var preset = MarketingBiDashboardPresetRegistry.preset("canvas-effect");
        var exported = new BiDashboardExportPackage(
                "DASHBOARD",
                1,
                "canvas-effect",
                2,
                preset,
                "alice",
                java.time.LocalDateTime.parse("2026-06-05T09:30:00"));
        var command = new BiDashboardImportCommand(exported, "imported-canvas-effect", "导入画布效果", false);
        var resource = new BiDashboardResource(preset, "DRAFT", 1, "PERSISTED");
        when(service.importResource(7L, "alice", command)).thenReturn(resource);
        BiDashboardController controller = new BiDashboardController(resolver, service);

        StepVerifier.create(controller.importPackage(command))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("DRAFT"))
                .verifyComplete();

        verify(service).importResource(7L, "alice", command);
    }

    @Test
    void importPackageFileUsesCurrentTenantAndUser() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiDashboardResourceService service = mock(BiDashboardResourceService.class);
        byte[] content = "{\"resourceType\":\"DASHBOARD\"}".getBytes(StandardCharsets.UTF_8);
        FilePart filePart = mock(FilePart.class);
        when(filePart.content()).thenReturn(Flux.just(new DefaultDataBufferFactory().wrap(content)));
        var preset = MarketingBiDashboardPresetRegistry.preset("canvas-effect");
        var resource = new BiDashboardResource(preset, "DRAFT", 1, "PERSISTED");
        when(service.importResourceFile(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(),
                any(byte[].class),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(resource);
        BiDashboardController controller = new BiDashboardController(resolver, service);

        StepVerifier.create(controller.importPackageFile(filePart, "uploaded-canvas-effect", "上传画布效果", false))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("DRAFT"))
                .verifyComplete();

        ArgumentCaptor<byte[]> contentCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(service).importResourceFile(
                org.mockito.Mockito.eq(7L),
                org.mockito.Mockito.eq("alice"),
                contentCaptor.capture(),
                org.mockito.Mockito.eq("uploaded-canvas-effect"),
                org.mockito.Mockito.eq("上传画布效果"),
                org.mockito.Mockito.eq(false));
        assertThat(contentCaptor.getValue()).isEqualTo(content);
    }

    @Test
    void listReturnsDashboardResources() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiDashboardResourceService service = mock(BiDashboardResourceService.class);
        var resource = new BiDashboardResource(
                MarketingBiDashboardPresetRegistry.preset("canvas-effect"),
                "PUBLISHED",
                2,
                "PERSISTED");
        when(service.list(7L)).thenReturn(List.of(resource));
        BiDashboardController controller = new BiDashboardController(resolver, service);

        StepVerifier.create(controller.list())
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(item -> assertThat(item.status()).isEqualTo("PUBLISHED")))
                .verifyComplete();
    }

    @Test
    void listVersionsUsesCurrentTenant() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiDashboardResourceService service = mock(BiDashboardResourceService.class);
        var preset = MarketingBiDashboardPresetRegistry.preset("canvas-effect");
        var version = new org.chovy.canvas.domain.bi.dashboard.BiDashboardVersionView(
                88L,
                "canvas-effect",
                2,
                "PUBLISHED",
                preset,
                "alice",
                java.time.LocalDateTime.parse("2026-06-05T08:30:00"));
        when(service.listVersions(7L, "canvas-effect", 5)).thenReturn(List.of(version));
        BiDashboardController controller = new BiDashboardController(resolver, service);

        StepVerifier.create(controller.listVersions("canvas-effect", 5))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(item -> {
                            assertThat(item.version()).isEqualTo(2);
                            assertThat(item.publishedBy()).isEqualTo("alice");
                        }))
                .verifyComplete();

        verify(service).listVersions(7L, "canvas-effect", 5);
    }

    @Test
    void restoreVersionUsesCurrentTenantUserAndLockToken() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiDashboardResourceService service = mock(BiDashboardResourceService.class);
        var preset = MarketingBiDashboardPresetRegistry.preset("canvas-effect");
        var resource = new BiDashboardResource(preset, "DRAFT", 3, "PERSISTED");
        when(service.restoreVersion(7L, "alice", "TENANT_ADMIN", "canvas-effect", 2, "lock-token-1"))
                .thenReturn(resource);
        BiDashboardController controller = new BiDashboardController(resolver, service);

        StepVerifier.create(controller.restoreVersion("canvas-effect", "lock-token-1", 2))
                .assertNext(response -> {
                    assertThat(response.getData().status()).isEqualTo("DRAFT");
                    assertThat(response.getData().version()).isEqualTo(3);
                })
                .verifyComplete();

        verify(service).restoreVersion(7L, "alice", "TENANT_ADMIN", "canvas-effect", 2, "lock-token-1");
    }
}
