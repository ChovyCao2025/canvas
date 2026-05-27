package org.chovy.canvas.web;

import org.chovy.canvas.dal.mapper.AudienceDefinitionMapper;
import org.chovy.canvas.dal.mapper.AudienceStatMapper;
import org.chovy.canvas.domain.notification.NotificationService;
import org.chovy.canvas.domain.task.AsyncTaskService;
import org.chovy.canvas.engine.audience.AudienceBatchComputeService;
import org.chovy.canvas.engine.audience.AudienceComputeTaskRunner;
import org.chovy.canvas.engine.audience.AudienceSchedulerService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

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
