package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.domain.canvas.CanvasImportExportService;
import org.chovy.canvas.domain.canvas.CanvasMessagePreviewService;
import org.chovy.canvas.domain.canvas.CanvasOpsService;
import org.chovy.canvas.domain.canvas.CanvasPrePublishCheckService;
import org.chovy.canvas.domain.canvas.CanvasProjectFolderMetadataService;
import org.chovy.canvas.domain.canvas.CanvasService;
import org.chovy.canvas.domain.compliance.AuditEventService;
import org.chovy.canvas.domain.notification.NotificationEventService;
import org.chovy.canvas.dto.canvas.CanvasImportReq;
import org.chovy.canvas.dto.canvas.CanvasImportResp;
import org.chovy.canvas.dto.canvas.MessagePreviewReq;
import org.chovy.canvas.dto.canvas.MessagePreviewResp;
import org.chovy.canvas.dto.canvas.ProjectFolderMetadataReq;
import org.chovy.canvas.dto.canvas.ProjectFolderMetadataResp;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasControllerOperatorLoopTest {

    @Test
    void publishRecordsMaskedAuditEvent() {
        CanvasService canvasService = mock(CanvasService.class);
        AuditEventService auditEventService = mock(AuditEventService.class);
        CanvasController controller = controller(canvasService, mock(CanvasMessagePreviewService.class),
                mock(CanvasImportExportService.class), mock(CanvasProjectFolderMetadataService.class));
        controller.setAuditEventService(auditEventService);
        CanvasVersionDO version = new CanvasVersionDO();
        version.setId(99L);
        when(canvasService.publish(62L, "alice")).thenReturn(version);

        StepVerifier.create(controller.publish(62L, "alice"))
                .assertNext(response -> assertThat(response.getData()).isSameAs(version))
                .verifyComplete();

        ArgumentCaptor<AuditEventService.AuditEventCommand> captor =
                ArgumentCaptor.forClass(AuditEventService.AuditEventCommand.class);
        verify(auditEventService).record(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(10L);
        assertThat(captor.getValue().getActor()).isEqualTo("alice");
        assertThat(captor.getValue().getActorRole()).isEqualTo(RoleNames.TENANT_ADMIN);
        assertThat(captor.getValue().getOperation()).isEqualTo("canvas publish");
        assertThat(captor.getValue().getTargetType()).isEqualTo("canvas");
        assertThat(captor.getValue().getTargetId()).isEqualTo("62");
        assertThat(captor.getValue().getMetadata()).containsEntry("toVersion", 99L);
    }

    @Test
    void previewMessageNormalizesCanvasIdAndChecksTenantAccess() {
        CanvasService canvasService = mock(CanvasService.class);
        CanvasMessagePreviewService previewService = mock(CanvasMessagePreviewService.class);
        CanvasController controller = controller(canvasService, previewService,
                mock(CanvasImportExportService.class), mock(CanvasProjectFolderMetadataService.class));
        MessagePreviewResp preview = new MessagePreviewResp(
                "SMS", "tpl-1", Map.of("body", "Hi"), Map.of(), List.of("PREVIEW_ONLY_NO_SEND"));
        when(previewService.preview(any(MessagePreviewReq.class))).thenReturn(preview);

        StepVerifier.create(controller.previewMessage(62L, new MessagePreviewReq(
                        999L, "send", "u1", "{\"nodes\":[]}", Map.of())))
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo(0);
                    assertThat(response.getData()).isSameAs(preview);
                })
                .verifyComplete();

        verify(canvasService).requireTenantAccess(62L, 10L, false);
        ArgumentCaptor<MessagePreviewReq> captor = ArgumentCaptor.forClass(MessagePreviewReq.class);
        verify(previewService).preview(captor.capture());
        assertThat(captor.getValue().canvasId()).isEqualTo(62L);
    }

    @Test
    void importCanvasDelegatesWithCurrentTenantAndDefaultOperator() {
        CanvasService canvasService = mock(CanvasService.class);
        CanvasImportExportService importExportService = mock(CanvasImportExportService.class);
        CanvasController controller = controller(canvasService, mock(CanvasMessagePreviewService.class),
                importExportService, mock(CanvasProjectFolderMetadataService.class));
        CanvasDO canvas = new CanvasDO();
        canvas.setId(700L);
        CanvasImportResp resp = new CanvasImportResp(canvas, 701L);
        when(importExportService.importCanvas(any(CanvasImportReq.class), eq(10L))).thenReturn(resp);

        StepVerifier.create(controller.importCanvas(new CanvasImportReq("{\"packageVersion\":1}", null)))
                .assertNext(response -> assertThat(response.getData()).isSameAs(resp))
                .verifyComplete();

        ArgumentCaptor<CanvasImportReq> captor = ArgumentCaptor.forClass(CanvasImportReq.class);
        verify(importExportService).importCanvas(captor.capture(), eq(10L));
        assertThat(captor.getValue().operator()).isEqualTo("system");
    }

    @Test
    void saveProjectFolderMetadataChecksTenantAccessAndDefaultsOperator() {
        CanvasService canvasService = mock(CanvasService.class);
        CanvasProjectFolderMetadataService metadataService = mock(CanvasProjectFolderMetadataService.class);
        CanvasController controller = controller(canvasService, mock(CanvasMessagePreviewService.class),
                mock(CanvasImportExportService.class), metadataService);
        ProjectFolderMetadataResp resp = new ProjectFolderMetadataResp(
                62L, "growth", "Growth", "new-user", "New User");
        when(metadataService.saveMetadata(eq(62L), any(ProjectFolderMetadataReq.class))).thenReturn(resp);

        StepVerifier.create(controller.saveProjectFolderMetadata(62L, new ProjectFolderMetadataReq(
                        "growth", "Growth", "new-user", "New User", " ")))
                .assertNext(response -> assertThat(response.getData()).isSameAs(resp))
                .verifyComplete();

        verify(canvasService).requireTenantAccess(62L, 10L, false);
        ArgumentCaptor<ProjectFolderMetadataReq> captor = ArgumentCaptor.forClass(ProjectFolderMetadataReq.class);
        verify(metadataService).saveMetadata(eq(62L), captor.capture());
        assertThat(captor.getValue().operator()).isEqualTo("system");
    }

    private CanvasController controller(
            CanvasService canvasService,
            CanvasMessagePreviewService previewService,
            CanvasImportExportService importExportService,
            CanvasProjectFolderMetadataService metadataService) {
        TenantContextResolver tenantResolver = mock(TenantContextResolver.class);
        when(tenantResolver.current()).thenReturn(Mono.just(new TenantContext(
                10L, RoleNames.TENANT_ADMIN, "alice")));
        return new CanvasController(
                canvasService,
                mock(CanvasOpsService.class),
                mock(NotificationEventService.class),
                tenantResolver,
                previewService,
                importExportService,
                metadataService,
                mock(CanvasPrePublishCheckService.class));
    }
}
