package org.chovy.canvas.engine.trigger;

import org.chovy.canvas.domain.cdp.CdpUserService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CanvasExecutionServiceCdpTest {

    @Test
    void serviceDeclaresCdpUserServiceDependency() {
        boolean hasDependency = java.util.Arrays.stream(CanvasExecutionService.class.getDeclaredFields())
                .anyMatch(field -> field.getType().equals(CdpUserService.class));

        assertThat(hasDependency).isTrue();
    }
}
