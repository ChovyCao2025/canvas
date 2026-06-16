package org.chovy.canvas.canvas.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * 封装ExecutionPublicationPortContractTest相关的业务逻辑。
 */
class ExecutionPublicationPortContractTest {

    /**
     * 处理exposesPublishAndUnpublishWithoutRuntimeImplementationDetails。
     */
    @Test
    void exposesPublishAndUnpublishWithoutRuntimeImplementationDetails() throws Exception {
        Method publish = ExecutionPublicationPort.class.getMethod("publish", PublishedCanvasDefinition.class);
        Method unpublish = ExecutionPublicationPort.class.getMethod("unpublish", Long.class, Long.class);

        assertThat(publish.getReturnType()).isEqualTo(Void.TYPE);
        assertThat(unpublish.getReturnType()).isEqualTo(Void.TYPE);
        assertThat(Arrays.stream(ExecutionPublicationPort.class.getMethods())
                .map(Method::getName)
                .filter(name -> name.contains("Redis") || name.contains("Scheduler") || name.contains("Cache")))
                .isEmpty();
    }

    /**
     * 处理canBeImplementedByExecutionAdapterWithoutCanvasPersistenceTypes。
     */
    @Test
    void canBeImplementedByExecutionAdapterWithoutCanvasPersistenceTypes() {
        CapturingPublicationPort port = new CapturingPublicationPort();
        PublishedCanvasDefinition definition = new PublishedCanvasDefinition(
                1L,
                2L,
                3L,
                1,
                "{\"nodes\":[]}",
                Instant.parse("2026-06-10T01:00:00Z"),
                Map.of("lane", "standard"),
                List.of(),
                List.of());

        port.publish(definition);
        port.unpublish(1L, 2L);

        assertThat(port.published).isSameAs(definition);
        assertThat(port.unpublishedTenantId).isEqualTo(1L);
        assertThat(port.unpublishedCanvasId).isEqualTo(2L);
    }

    /**
     * 封装CapturingPublicationPort相关的业务逻辑。
     */
    private static final class CapturingPublicationPort implements ExecutionPublicationPort {

        /**
         * 保存published。
         */
        private PublishedCanvasDefinition published;

        /**
         * 保存unpublished tenant标识。
         */
        private Long unpublishedTenantId;

        /**
         * 保存unpublished canvas标识。
         */
        private Long unpublishedCanvasId;

        /**
         * 处理publish。
         */
        @Override
        public void publish(PublishedCanvasDefinition definition) {
            this.published = definition;
        }

        /**
         * 处理unpublish。
         */
        @Override
        public void unpublish(Long tenantId, Long canvasId) {
            this.unpublishedTenantId = tenantId;
            this.unpublishedCanvasId = canvasId;
        }
    }
}
