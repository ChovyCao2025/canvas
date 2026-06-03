package org.chovy.canvas.infra.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.chovy.cache.CacheInvalidationEvent;
import org.chovy.cache.TieredCacheManager;
import org.chovy.canvas.engine.concurrent.BackgroundTaskExecutor;
import org.chovy.canvas.infrastructure.cache.RocketMqCacheInvalidationConsumer;
import org.chovy.canvas.infrastructure.cache.RocketMqCacheInvalidationPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RocketMqCacheInvalidationTest {

    @Mock RocketMQTemplate rocketMQTemplate;
    @Mock TieredCacheManager cacheManager;
    @Mock BackgroundTaskExecutor backgroundTaskExecutor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void publisherSchedulesInvalidationEventToBroadcastTopicWithCacheNameTag() {
        RocketMqCacheInvalidationPublisher publisher = new RocketMqCacheInvalidationPublisher(
                rocketMQTemplate, backgroundTaskExecutor);
        ReflectionTestUtils.setField(publisher, "topic", "CACHE_INVALIDATE");
        CacheInvalidationEvent event = new CacheInvalidationEvent("sample", "42", 3L);
        when(backgroundTaskExecutor.submitBestEffort(eq("cache-invalidation:sample"), any(Runnable.class)))
                .thenAnswer(invocation -> {
                    Runnable task = invocation.getArgument(1);
                    task.run();
                    return true;
                });

        publisher.publish(event);

        verify(backgroundTaskExecutor).submitBestEffort(eq("cache-invalidation:sample"), any(Runnable.class));
        verify(rocketMQTemplate).syncSend("CACHE_INVALIDATE:sample", event);
    }

    @Test
    void consumerDispatchesInvalidationEventToCacheManager() throws Exception {
        RocketMqCacheInvalidationConsumer consumer =
                new RocketMqCacheInvalidationConsumer(objectMapper, cacheManager);
        CacheInvalidationEvent event = new CacheInvalidationEvent("sample", "42", 3L);
        MessageExt message = new MessageExt();
        message.setBody(objectMapper.writeValueAsString(event).getBytes(StandardCharsets.UTF_8));

        consumer.onMessage(message);

        verify(cacheManager).receiveInvalidation(event);
    }
}
