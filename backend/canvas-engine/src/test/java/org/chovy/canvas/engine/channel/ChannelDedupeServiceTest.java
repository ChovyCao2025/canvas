package org.chovy.canvas.engine.channel;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChannelDedupeServiceTest {

    @Test
    void duplicateContentSuppressesProviderCallWithinWindow() {
        ChannelDedupeService.Repository repo = mock(ChannelDedupeService.Repository.class);
        when(repo.reserve(0L, "welcome", "hash-1", "SMS", "u1", Duration.ofHours(24)))
                .thenReturn(true, false);
        ChannelDedupeService service = new ChannelDedupeService(repo);

        assertThat(service.reserve(0L, "welcome", "hash-1", "SMS", "u1", Duration.ofHours(24)).status())
                .isEqualTo("RESERVED");
        assertThat(service.reserve(0L, "welcome", "hash-1", "SMS", "u1", Duration.ofHours(24)).status())
                .isEqualTo("DUPLICATE");
    }

    @Test
    void payloadHashIsStableForEquivalentMapOrdering() {
        ChannelDedupeService service = new ChannelDedupeService((tenantId, group, hash, channel, userId, ttl) -> true);

        String first = service.hashPayload("sms", "tpl-1", Map.of("b", 2, "a", 1));
        String second = service.hashPayload("SMS", "tpl-1", Map.of("a", 1, "b", 2));

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSize(64);
    }
}
