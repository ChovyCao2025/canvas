package org.chovy.canvas.controller;

import org.chovy.canvas.domain.audience.AudienceDefinitionMapper;
import org.chovy.canvas.domain.audience.AudienceStatMapper;
import org.chovy.canvas.domain.notification.NotificationService;
import org.chovy.canvas.domain.task.AsyncTaskService;
import org.chovy.canvas.engine.audience.AudienceBatchComputeService;
import org.chovy.canvas.engine.audience.AudienceComputeTaskRunner;
import org.chovy.canvas.engine.audience.AudienceSchedulerService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

class AudienceControllerTest {

    @Test
    void computePassesPerfRunContextToComputeService() {
        AudienceBatchComputeService computeService = mock(AudienceBatchComputeService.class);
        AudienceController controller = new AudienceController(
                mock(AudienceDefinitionMapper.class),
                mock(AudienceStatMapper.class),
                computeService,
                mock(AudienceSchedulerService.class),
                mock(AsyncTaskService.class),
                mock(AudienceComputeTaskRunner.class),
                mock(NotificationService.class)
        );
        AudienceController.ComputeReq req = new AudienceController.ComputeReq();
        req.setPerfRunId("perf_20260523_001");
        req.setPerfInputId("perf_20260523_001:audience:1");

        controller.compute(1L, req).block();

        verify(computeService, timeout(1_000))
                .compute(1L, "perf_20260523_001", "perf_20260523_001:audience:1");
    }
}
