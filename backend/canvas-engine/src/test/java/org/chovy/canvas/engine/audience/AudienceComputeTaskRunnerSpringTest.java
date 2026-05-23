package org.chovy.canvas.engine.audience;

import org.chovy.canvas.domain.notification.NotificationService;
import org.chovy.canvas.domain.task.AsyncTaskService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AudienceComputeTaskRunnerSpringTest {

    @Test
    void springCreatesRunnerWithCollaboratorConstructor() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(AudienceBatchComputeService.class, () -> mock(AudienceBatchComputeService.class));
            context.registerBean(AsyncTaskService.class, () -> mock(AsyncTaskService.class));
            context.registerBean(NotificationService.class, () -> mock(NotificationService.class));
            context.register(AudienceComputeTaskRunner.class);

            context.refresh();

            assertThat(context.getBean(AudienceComputeTaskRunner.class)).isNotNull();
        }
    }
}
