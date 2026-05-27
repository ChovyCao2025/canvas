package org.chovy.canvas.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboundUrlValidatorTest {

    @Test
    void rejectsLoopbackAndPrivateAddresses() {
        assertThatThrownBy(() -> OutboundUrlValidator.validateHttpUrl("http://127.0.0.1:8080/internal"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不允许访问内网或本机地址");
        assertThatThrownBy(() -> OutboundUrlValidator.validateHttpUrl("http://10.0.0.5/admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不允许访问内网或本机地址");
        assertThatThrownBy(() -> OutboundUrlValidator.validateHttpUrl("http://[::1]/admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不允许访问内网或本机地址");
    }

    @Test
    void allowsPublicHttpAddress() {
        assertThatCode(() -> OutboundUrlValidator.validateHttpUrl("http://93.184.216.34/api"))
                .doesNotThrowAnyException();
    }
}
