package org.chovy.canvas.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebConfigTest {

    @Test
    void productionCorsRejectsWildcardWhenCredentialsAreAllowed() {
        WebConfig config = new WebConfig(List.of("*"), true);

        assertThatThrownBy(config::corsConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("canvas.cors.allowed-origins");
    }

    @Test
    void localCorsMayUseWildcardForDeveloperConvenience() {
        WebConfig config = new WebConfig(List.of("*"), false);

        assertThatCode(config::corsConfiguration).doesNotThrowAnyException();
    }
}
