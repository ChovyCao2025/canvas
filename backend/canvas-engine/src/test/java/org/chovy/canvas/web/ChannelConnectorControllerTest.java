package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChannelConnectorControllerTest {

    @Test
    void listsConnectorsWithModeAndHealth() {
        ChannelConnectorController.Service service = mock(ChannelConnectorController.Service.class);
        when(service.list(0L)).thenReturn(List.of(new ChannelConnectorController.ConnectorRow(
                1L, "sms-aliyun", "SMS", "ALIYUN", "REAL", "UP", "ok")));

        ChannelConnectorController controller = new ChannelConnectorController(service);

        R<List<ChannelConnectorController.ConnectorRow>> response = controller.list().block();

        assertThat(response.getData().get(0).mode()).isEqualTo("REAL");
        assertThat(response.getData().get(0).healthStatus()).isEqualTo("UP");
    }

    @Test
    void validatesFallbackPolicyBeforeSave() {
        ChannelConnectorController.Service service = mock(ChannelConnectorController.Service.class);
        ChannelConnectorController.FallbackPolicyReq req =
                new ChannelConnectorController.FallbackPolicyReq("PUSH", "JPUSH", "SMS", "ALIYUN");
        when(service.validateFallback(0L, req)).thenReturn(
                new ChannelConnectorController.ValidationResult(false, "cycle: PUSH:JPUSH -> SMS:ALIYUN -> PUSH:JPUSH"));

        ChannelConnectorController.ValidationResult result = new ChannelConnectorController(service)
                .validateFallback(req)
                .block()
                .getData();

        assertThat(result.valid()).isFalse();
        assertThat(result.message()).contains("cycle");
    }
}
