package org.chovy.canvas.engine.reactive;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessSubscriptionGovernanceTest {

    @Test
    void executionRequestExecutorDoesNotOwnRawReactorSubscriptions() throws Exception {
        assertManagedSubscriptionsOnly(
                "src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestExecutor.java");
    }

    @Test
    void notificationRealtimeServiceDoesNotOwnRawReactorSubscriptions() throws Exception {
        assertManagedSubscriptionsOnly(
                "src/main/java/org/chovy/canvas/domain/notification/NotificationRealtimeService.java");
    }

    @Test
    void killSwitchSubscriberDoesNotOwnRawReactorSubscriptions() throws Exception {
        assertManagedSubscriptionsOnly(
                "src/main/java/org/chovy/canvas/infrastructure/redis/KillSwitchSubscriber.java");
    }

    private static void assertManagedSubscriptionsOnly(String sourcePath) throws Exception {
        String source = Files.readString(Path.of(sourcePath));

        assertThat(source)
                .satisfiesAnyOf(
                        s -> assertThat(s).contains("BackgroundSubscriptionRegistry"),
                        s -> assertThat(s).contains("TrackedReactiveTaskRegistry"));
        assertThat(source).doesNotContain(".subscribe(");
    }
}
