package org.chovy.canvas.engine.channel;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChannelConnectorRegistryTest {

    @Test
    void missingConnectorResolvesDisabledConnector() {
        ChannelConnectorRegistry.Repository repository = mock(ChannelConnectorRegistry.Repository.class);
        when(repository.find(0L, "SMS", "ALIYUN")).thenReturn(null);

        ChannelConnector connector = new ChannelConnectorRegistry(repository, Map.of()).resolve(0L, "SMS", "ALIYUN");

        assertThat(connector.mode()).isEqualTo(ChannelConnector.ConnectorMode.DISABLED);
        assertThat(connector.health().message()).contains("not configured");
    }

    @Test
    void sandboxConnectorReturnsDeterministicFakeMessageId() {
        ChannelConnectorRegistry.Repository repository = mock(ChannelConnectorRegistry.Repository.class);
        when(repository.find(0L, "SMS", "SANDBOX")).thenReturn(new ChannelConnectorRegistry.ConnectorConfig(
                "sms-sandbox", "SMS", "SANDBOX", ChannelConnector.ConnectorMode.SANDBOX, null));

        ChannelConnector connector = new ChannelConnectorRegistry(repository, Map.of()).resolve(0L, "SMS", "SANDBOX");

        ChannelConnector.ConnectorSendResult result = connector.send(new ChannelConnector.ConnectorSendRequest(
                0L, "SMS", "SANDBOX", "u1", Map.of("templateId", "tpl-1")));

        assertThat(result.accepted()).isTrue();
        assertThat(result.externalMessageId()).isEqualTo("sandbox-SMS-u1");
    }

    @Test
    void realConnectorWithoutRegisteredImplementationFailsClosed() {
        ChannelConnectorRegistry.Repository repository = mock(ChannelConnectorRegistry.Repository.class);
        when(repository.find(0L, "SMS", "ALIYUN")).thenReturn(new ChannelConnectorRegistry.ConnectorConfig(
                "sms-aliyun", "SMS", "ALIYUN", ChannelConnector.ConnectorMode.REAL, null));

        ChannelConnector connector = new ChannelConnectorRegistry(repository, Map.of()).resolve(0L, "SMS", "ALIYUN");

        assertThat(connector.mode()).isEqualTo(ChannelConnector.ConnectorMode.DISABLED);
        assertThat(connector.health().message()).contains("real connector not registered");
    }
}
