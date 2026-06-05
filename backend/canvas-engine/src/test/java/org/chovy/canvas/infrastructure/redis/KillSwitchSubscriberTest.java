package org.chovy.canvas.infrastructure.redis;

import org.chovy.canvas.engine.trigger.InFlightExecutionRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.Disposable;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KillSwitchSubscriberTest {

    @Test
    void shutdownDisposesSubscriptionAndListenerContainer() throws Exception {
        KillSwitchSubscriber subscriber = new KillSwitchSubscriber(
                mock(ReactiveRedisConnectionFactory.class),
                mock(InFlightExecutionRegistry.class));
        Disposable subscription = mock(Disposable.class);
        ReactiveRedisMessageListenerContainer listenerContainer =
                mock(ReactiveRedisMessageListenerContainer.class);

        when(subscription.isDisposed()).thenReturn(false);
        ReflectionTestUtils.setField(subscriber, "subscription", subscription);
        ReflectionTestUtils.setField(subscriber, "listenerContainer", listenerContainer);

        subscriber.shutdown();

        verify(subscription).dispose();
        verify(listenerContainer).destroy();
    }
}
