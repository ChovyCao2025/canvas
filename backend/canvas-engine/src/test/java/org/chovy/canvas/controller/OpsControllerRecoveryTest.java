package org.chovy.canvas.web;

import org.chovy.canvas.dal.mapper.CanvasManualApprovalMapper;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasTemplateMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.infrastructure.cache.CanvasConfigCache;
import org.chovy.canvas.infrastructure.redis.TriggerRouteRecoveryService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpsControllerRecoveryTest {

    @Test
    void rebuildRuntimeStateDelegatesToRecoveryService() {
        TriggerRouteRecoveryService recoveryService = mock(TriggerRouteRecoveryService.class);
        TriggerRouteRecoveryService.RecoveryReport report =
                new TriggerRouteRecoveryService.RecoveryReport(2, 3, 4, 5, 6);
        when(recoveryService.rebuildRuntimeState()).thenReturn(report);
        OpsController controller = new OpsController(
                mock(CanvasTemplateMapper.class),
                mock(CanvasMapper.class),
                mock(CanvasVersionMapper.class),
                mock(CanvasManualApprovalMapper.class),
                mock(CanvasConfigCache.class),
                recoveryService);

        var response = controller.rebuildRuntimeState().block();

        verify(recoveryService).rebuildRuntimeState();
        assertThat(response.getCode()).isZero();
        assertThat(response.getData()).isEqualTo(report);
    }
}
