package org.chovy.canvas.infra.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.common.message.MessageExt;
import org.chovy.cache.CacheInvalidationEvent;
import org.chovy.cache.TieredCacheManager;
import org.chovy.canvas.infrastructure.cache.RocketMqCacheInvalidationConsumer;
import org.chovy.canvas.infrastructure.cache.RocketMqCacheInvalidationPublisher;
import org.chovy.canvas.infrastructure.mq.CanvasMessageBus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RocketMqCacheInvalidationTest {

    @Mock CanvasMessageBus messageBus;
    @Mock TieredCacheManager cacheManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void publisherSendsInvalidationEventToBroadcastTopicWithCacheNameTag() {
        RocketMqCacheInvalidationPublisher publisher = new RocketMqCacheInvalidationPublisher(messageBus);
        ReflectionTestUtils.setField(publisher, "topic", "CACHE_INVALIDATE");
        CacheInvalidationEvent event = new CacheInvalidationEvent("sample", "42", 3L);

        publisher.publish(event);

        verify(messageBus).publishCacheInvalidation("CACHE_INVALIDATE", event);
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
