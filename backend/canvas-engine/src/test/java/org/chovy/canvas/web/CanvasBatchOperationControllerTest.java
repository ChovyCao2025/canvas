package org.chovy.canvas.web;

import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.domain.canvas.CanvasOpsService;
import org.chovy.canvas.domain.canvas.CanvasService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasBatchOperationControllerTest {

    private final CanvasService canvasService = mock(CanvasService.class);
    private final CanvasOpsService opsService = mock(CanvasOpsService.class);
    private final CanvasMapper canvasMapper = mock(CanvasMapper.class);
    private final TenantContextResolver tenantResolver = mock(TenantContextResolver.class);
    private final CanvasBatchOperationController controller =
            new CanvasBatchOperationController(canvasService, opsService, canvasMapper, tenantResolver);

    @Test
    void pauseReturnsPerItemSuccessSkippedAndFailed() {
        tenant(RoleNames.TENANT_ADMIN);
        when(canvasService.requireTenantAccess(eq(1L), eq(7L), eq(false))).thenReturn(canvas(1L, CanvasStatusEnum.PUBLISHED));
        when(canvasService.requireTenantAccess(eq(2L), eq(7L), eq(false))).thenReturn(canvas(2L, CanvasStatusEnum.DRAFT));
        when(canvasService.requireTenantAccess(eq(3L), eq(7L), eq(false))).thenThrow(new IllegalArgumentException("missing"));

        StepVerifier.create(controller.run("pause", request(List.of(1L, 2L, 3L), null, null)).map(r -> r.getData()))
                .assertNext(result -> {
                    assertThat(result.operation()).isEqualTo("PAUSE");
                    assertThat(result.successCount()).isEqualTo(1);
                    assertThat(result.skippedCount()).isEqualTo(1);
                    assertThat(result.failedCount()).isEqualTo(1);
                    assertThat(result.items()).extracting(CanvasBatchOperationController.BatchOperationItem::status)
                            .containsExactly("SUCCESS", "SKIPPED", "FAILED");
                })
                .verifyComplete();

        verify(canvasService).offline(1L, "alice");
        verify(canvasService, never()).offline(2L, "alice");
    }

    @Test
    void resumeSkipsAlreadyPublishedAndArchivedButPublishesOfflineCanvas() {
        tenant(RoleNames.TENANT_ADMIN);
        when(canvasService.requireTenantAccess(eq(1L), eq(7L), eq(false))).thenReturn(canvas(1L, CanvasStatusEnum.PUBLISHED));
        when(canvasService.requireTenantAccess(eq(2L), eq(7L), eq(false))).thenReturn(canvas(2L, CanvasStatusEnum.OFFLINE));
        when(canvasService.requireTenantAccess(eq(3L), eq(7L), eq(false))).thenReturn(canvas(3L, CanvasStatusEnum.ARCHIVED));

        StepVerifier.create(controller.run("resume", request(List.of(1L, 2L, 3L), null, null)).map(r -> r.getData()))
                .assertNext(result -> {
                    assertThat(result.successCount()).isEqualTo(1);
                    assertThat(result.skippedCount()).isEqualTo(2);
                    assertThat(result.failedCount()).isZero();
                })
                .verifyComplete();

        verify(canvasService).publish(2L, "alice");
        verify(canvasService, never()).publish(1L, "alice");
        verify(canvasService, never()).publish(3L, "alice");
    }

    @Test
    void archiveSkipsAlreadyArchivedCanvas() {
        tenant(RoleNames.TENANT_ADMIN);
        when(canvasService.requireTenantAccess(eq(1L), eq(7L), eq(false))).thenReturn(canvas(1L, CanvasStatusEnum.OFFLINE));
        when(canvasService.requireTenantAccess(eq(2L), eq(7L), eq(false))).thenReturn(canvas(2L, CanvasStatusEnum.ARCHIVED));

        StepVerifier.create(controller.run("archive", request(List.of(1L, 2L), null, null)).map(r -> r.getData()))
                .assertNext(result -> {
                    assertThat(result.successCount()).isEqualTo(1);
                    assertThat(result.skippedCount()).isEqualTo(1);
                })
                .verifyComplete();

        verify(canvasService).archive(1L, "alice");
        verify(canvasService, never()).archive(2L, "alice");
    }

    @Test
    void cloneAppliesReplacementParametersToClonedCanvas() {
        tenant(RoleNames.TENANT_ADMIN);
        when(canvasService.requireTenantAccess(eq(1L), eq(7L), eq(false))).thenReturn(canvas(1L, CanvasStatusEnum.DRAFT));
        CanvasDO cloned = canvas(99L, CanvasStatusEnum.DRAFT);
        cloned.setName("${market} Journey");
        cloned.setDescription("for ${market}");
        when(opsService.clone(1L, "alice")).thenReturn(cloned);

        StepVerifier.create(controller.run("clone", request(List.of(1L), null,
                        Map.of("market", "EU"))).map(r -> r.getData()))
                .assertNext(result -> {
                    assertThat(result.successCount()).isEqualTo(1);
                    assertThat(result.items().get(0).targetCanvasId()).isEqualTo(99L);
                })
                .verifyComplete();

        ArgumentCaptor<CanvasDO> captor = ArgumentCaptor.forClass(CanvasDO.class);
        verify(canvasMapper).updateById(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("EU Journey");
        assertThat(captor.getValue().getDescription()).isEqualTo("for EU");
    }

    @Test
    void groupedFiltersCanResolveBatchInput() {
        tenant(RoleNames.TENANT_ADMIN);
        when(canvasMapper.selectList(any())).thenReturn(List.of(
                canvas(10L, CanvasStatusEnum.PUBLISHED),
                canvas(11L, CanvasStatusEnum.PUBLISHED)));
        when(canvasService.requireTenantAccess(eq(10L), eq(7L), eq(false))).thenReturn(canvas(10L, CanvasStatusEnum.PUBLISHED));
        when(canvasService.requireTenantAccess(eq(11L), eq(7L), eq(false))).thenReturn(canvas(11L, CanvasStatusEnum.PUBLISHED));

        StepVerifier.create(controller.run("pause", new CanvasBatchOperationController.BatchOperationRequest(
                        null,
                        new CanvasBatchOperationController.BatchCanvasFilters(
                                CanvasStatusEnum.PUBLISHED.getCode(), "growth", "REALTIME", 20),
                        null,
                        "batch pause")).map(r -> r.getData()))
                .assertNext(result -> {
                    assertThat(result.totalCount()).isEqualTo(2);
                    assertThat(result.successCount()).isEqualTo(2);
                })
                .verifyComplete();

        verify(canvasMapper).selectList(any());
        verify(canvasService).offline(10L, "alice");
        verify(canvasService).offline(11L, "alice");
    }

    @Test
    void rejectsNonAdminRole() {
        tenant(RoleNames.OPERATOR);

        StepVerifier.create(controller.run("archive", request(List.of(1L), null, null)))
                .expectErrorSatisfies(error -> assertThat(error).hasMessageContaining("admin"))
                .verify();
    }

    private CanvasBatchOperationController.BatchOperationRequest request(List<Long> ids,
                                                                        CanvasBatchOperationController.BatchCanvasFilters filters,
                                                                        Map<String, String> replacements) {
        return new CanvasBatchOperationController.BatchOperationRequest(ids, filters, replacements, "operator action");
    }

    private void tenant(String role) {
        when(tenantResolver.current()).thenReturn(Mono.just(new TenantContext(7L, role, "alice")));
    }

    private CanvasDO canvas(Long id, CanvasStatusEnum status) {
        CanvasDO canvas = new CanvasDO();
        canvas.setId(id);
        canvas.setTenantId(7L);
        canvas.setName("Canvas " + id);
        canvas.setStatus(status.getCode());
        return canvas;
    }
}
