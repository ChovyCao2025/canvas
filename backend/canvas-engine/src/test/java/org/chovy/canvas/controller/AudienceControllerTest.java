package org.chovy.canvas.web;

import org.chovy.canvas.dal.mapper.AudienceDefinitionMapper;
import org.chovy.canvas.dal.mapper.AudienceStatMapper;
import org.chovy.canvas.domain.notification.NotificationService;
import org.chovy.canvas.domain.task.AsyncTaskService;
import org.chovy.canvas.dto.audience.AudiencePreviewReq;
import org.chovy.canvas.dto.audience.AudienceSourceFieldDTO;
import org.chovy.canvas.engine.audience.AudienceBatchComputeService;
import org.chovy.canvas.engine.audience.AudienceComputeTaskRunner;
import org.chovy.canvas.engine.audience.AudienceSchedulerService;
import org.chovy.canvas.engine.audience.CdpAudienceSourceService;
import org.chovy.canvas.engine.concurrent.BackgroundTaskExecutor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Audience 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class AudienceControllerTest {

    @Test
    void computePassesPerfRunContextToComputeService() {
        AudienceBatchComputeService computeService = mock(AudienceBatchComputeService.class);
        BackgroundTaskExecutor backgroundTaskExecutor = new BackgroundTaskExecutor();
        try {
            AudienceController controller = new AudienceController(
                    mock(AudienceDefinitionMapper.class),
                    mock(AudienceStatMapper.class),
                    computeService,
                    mock(AudienceSchedulerService.class),
                    mock(AsyncTaskService.class),
                    mock(AudienceComputeTaskRunner.class),
                    mock(NotificationService.class),
                    mock(CdpAudienceSourceService.class),
                    backgroundTaskExecutor
            );
            AudienceController.ComputeReq req = new AudienceController.ComputeReq();
            req.setPerfRunId("perf_20260523_001");
            req.setPerfInputId("perf_20260523_001:audience:1");

            controller.compute(1L, req).block();

            verify(computeService, timeout(1_000))
                    .compute(1L, "perf_20260523_001", "perf_20260523_001:audience:1");
        } finally {
            backgroundTaskExecutor.shutdown();
        }
    }

    @Test
    void sourceFieldsReturnsCdpFields() {
        CdpAudienceSourceService cdpAudienceSourceService = mock(CdpAudienceSourceService.class);
        when(cdpAudienceSourceService.listSourceFields("CDP_TAG")).thenReturn(List.of(
                new AudienceSourceFieldDTO("high_value", "高价值用户", "STRING")
        ));
        AudienceController controller = controller(cdpAudienceSourceService);

        var response = controller.sourceFields("CDP_TAG").block();

        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).name()).isEqualTo("high_value");
    }

    @Test
    void previewReturnsEstimatedSizeAndLimitedSamples() {
        CdpAudienceSourceService cdpAudienceSourceService = mock(CdpAudienceSourceService.class);
        when(cdpAudienceSourceService.supports("CDP_TAG")).thenReturn(true);
        when(cdpAudienceSourceService.resolveUserIds("CDP_TAG", "{\"logic\":\"AND\"}"))
                .thenReturn(List.of("u1", "u2", "u3"));
        AudienceController controller = controller(cdpAudienceSourceService);

        var response = controller.preview(new AudiencePreviewReq("CDP_TAG", "{\"logic\":\"AND\"}", 2)).block();

        assertThat(response.getData().estimatedSize()).isEqualTo(3);
        assertThat(response.getData().sampleUserIds()).containsExactly("u1", "u2");
    }

    private AudienceController controller(CdpAudienceSourceService cdpAudienceSourceService) {
        return new AudienceController(
                mock(AudienceDefinitionMapper.class),
                mock(AudienceStatMapper.class),
                mock(AudienceBatchComputeService.class),
                mock(AudienceSchedulerService.class),
                mock(AsyncTaskService.class),
                mock(AudienceComputeTaskRunner.class),
                mock(NotificationService.class),
                cdpAudienceSourceService,
                mock(BackgroundTaskExecutor.class)
        );
    }
}
