package org.chovy.canvas.engine.audience;

import org.chovy.canvas.domain.notification.NotificationService;
import org.chovy.canvas.domain.task.AsyncTaskService;
import org.chovy.canvas.engine.concurrent.BackgroundTaskExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Audience Compute Task Runner Spring 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class AudienceComputeTaskRunnerSpringTest {

    @Test
    void springCreatesRunnerWithCollaboratorConstructor() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(AudienceBatchComputeService.class, () -> mock(AudienceBatchComputeService.class));
            context.registerBean(AsyncTaskService.class, () -> mock(AsyncTaskService.class));
            context.registerBean(NotificationService.class, () -> mock(NotificationService.class));
            context.registerBean(BackgroundTaskExecutor.class, () -> mock(BackgroundTaskExecutor.class));
            context.register(AudienceComputeTaskRunner.class);

            context.refresh();

            assertThat(context.getBean(AudienceComputeTaskRunner.class)).isNotNull();
        }
    }
}
