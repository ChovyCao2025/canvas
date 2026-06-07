package org.chovy.canvas.platform;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.web.PlatformWorkstreamController;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlatformWorkstreamContractTest {

    @Test
    void migrationCreatesWorkstreamTableWithChildSpecGate() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V264__platform_product_evolution_workstreams.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS platform_workstream")
                .contains("workstream_key VARCHAR(128) NOT NULL")
                .contains("child_spec_path VARCHAR(255) NULL")
                .contains("requires_child_spec TINYINT NOT NULL DEFAULT 1")
                .contains("UNIQUE KEY uk_platform_workstream_key");
    }

    @Test
    void listMarksWorkstreamsWithoutChildSpecsAsBlocked() {
        PlatformWorkstreamService.WorkstreamRepository repository =
                mock(PlatformWorkstreamService.WorkstreamRepository.class);
        PlatformWorkstreamService service = new PlatformWorkstreamService(repository);
        when(repository.list()).thenReturn(List.of(
                new PlatformWorkstreamService.Workstream(
                        "platformization", "Platformization", "P2", true, null, "Extension points"),
                new PlatformWorkstreamService.Workstream(
                        "data-assets", "Data Assets", "P2", true,
                        "docs/product-evolution/specs/p2-016-analytics-event-trace-schema-and-sink.md",
                        "Event pipeline")));

        List<PlatformWorkstreamService.WorkstreamStatus> result = service.statuses();

        assertThat(result)
                .extracting(PlatformWorkstreamService.WorkstreamStatus::status)
                .containsExactly("BLOCKED_CHILD_SPEC_REQUIRED", "READY_FOR_CHILD_EXECUTION");
    }

    @Test
    void requireExecutableChildSpecRejectsBroadWorkstreamWithoutSpecPath() {
        PlatformWorkstreamService.WorkstreamRepository repository =
                mock(PlatformWorkstreamService.WorkstreamRepository.class);
        PlatformWorkstreamService service = new PlatformWorkstreamService(repository);
        when(repository.get("channels")).thenReturn(new PlatformWorkstreamService.Workstream(
                "channels", "Channels", "P2", true, null, "WeCom and adapters"));

        assertThatThrownBy(() -> service.requireExecutableChildSpec("channels"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("channels requires a child spec before implementation");
    }

    @Test
    void controllerReturnsWorkstreamStatusesForAuthenticatedTenant() {
        PlatformWorkstreamService service = mock(PlatformWorkstreamService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.just(new TenantContext(8L, RoleNames.OPERATOR, "operator-1")));
        when(service.statuses()).thenReturn(List.of(new PlatformWorkstreamService.WorkstreamStatus(
                "data-assets",
                "Data Assets",
                "P2",
                "READY_FOR_CHILD_EXECUTION",
                "docs/product-evolution/specs/p2-016-analytics-event-trace-schema-and-sink.md",
                "Event pipeline")));
        PlatformWorkstreamController controller = new PlatformWorkstreamController(service, resolver);

        StepVerifier.create(controller.list())
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo(0);
                    assertThat(response.getData()).hasSize(1);
                    assertThat(response.getData().get(0).status()).isEqualTo("READY_FOR_CHILD_EXECUTION");
                })
                .verifyComplete();
    }

    @Test
    void controllerRejectsMissingTenantContext() {
        PlatformWorkstreamService service = mock(PlatformWorkstreamService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.error(new SecurityException("AUTH_003: missing tenant context")));
        PlatformWorkstreamController controller = new PlatformWorkstreamController(service, resolver);

        StepVerifier.create(controller.list())
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(SecurityException.class)
                        .hasMessageContaining("AUTH_003"))
                .verify();
    }
}
