package org.chovy.canvas.domain.canvas;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CanvasExamplesPropertiesTest {

    @Test
    void examplesAreEnabledByDefault() {
        CanvasExamplesProperties properties = new CanvasExamplesProperties();

        assertThat(properties.isEnabled()).isTrue();
    }
}
