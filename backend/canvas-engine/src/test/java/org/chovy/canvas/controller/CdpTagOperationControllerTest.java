package org.chovy.canvas.controller;

import org.chovy.canvas.domain.cdp.CdpTagOperation;
import org.chovy.canvas.domain.cdp.CdpTagOperationService;
import org.chovy.canvas.dto.cdp.CdpBatchTagReq;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class CdpTagOperationControllerTest {

    @Test
    void createReturnsCreatedOperation() {
        CdpTagOperationService service = Mockito.mock(CdpTagOperationService.class);
        CdpTagOperationController controller = new CdpTagOperationController(service);
        CdpTagOperation op = new CdpTagOperation();
        op.setId(7L);
        when(service.create(new CdpBatchTagReq("BATCH_SET", "vip", "true", List.of("u1"), "reason", "admin")))
                .thenReturn(op);

        CdpBatchTagReq req = new CdpBatchTagReq("BATCH_SET", "vip", "true", List.of("u1"), "reason", "admin");
        assertThat(controller.create(req).block().getData().getId()).isEqualTo(7L);
    }

    @Test
    void listReturnsRecentOperations() {
        CdpTagOperationService service = Mockito.mock(CdpTagOperationService.class);
        CdpTagOperationController controller = new CdpTagOperationController(service);
        when(service.listRecent(10)).thenReturn(List.of());

        assertThat(controller.list(10).block().getData()).isEmpty();
    }

    @Test
    void retryFailedReturnsNewOperation() {
        CdpTagOperationService service = Mockito.mock(CdpTagOperationService.class);
        CdpTagOperationController controller = new CdpTagOperationController(service);
        CdpTagOperation op = new CdpTagOperation();
        op.setId(9L);
        when(service.retryFailed(7L, null)).thenReturn(op);

        assertThat(controller.retryFailed(7L).block().getData().getId()).isEqualTo(9L);
    }
}
