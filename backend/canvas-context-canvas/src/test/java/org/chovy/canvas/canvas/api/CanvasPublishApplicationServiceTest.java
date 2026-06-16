package org.chovy.canvas.canvas.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * 封装CanvasPublishApplicationServiceTest相关的业务逻辑。
 */
class CanvasPublishApplicationServiceTest {

    /**
     * 处理publishBuildsDefinitionAndCallsExecutionPublicationPort。
     */
    @Test
    void publishBuildsDefinitionAndCallsExecutionPublicationPort() {
        CapturingPublicationPort port = new CapturingPublicationPort();
        ContractCanvasPublishApplicationService service = new ContractCanvasPublishApplicationService(port);

        Long versionId = service.publish(9L, 100L, 200L, "{\"nodes\":[{\"id\":\"start\"}]}");

        assertThat(versionId).isEqualTo(200L);
        assertThat(port.published).isNotNull();
        assertThat(port.published.tenantId()).isEqualTo(9L);
        assertThat(port.published.canvasId()).isEqualTo(100L);
        assertThat(port.published.versionId()).isEqualTo(200L);
        assertThat(port.published.graphJson()).contains("start");
    }

    /**
     * 封装ContractCanvasPublishApplicationService相关的业务逻辑。
     */
    private static final class ContractCanvasPublishApplicationService {

        /**
         * 保存publicationPort。
         */
        private final ExecutionPublicationPort publicationPort;

        /**
         * 创建当前对象实例。
         */
        private ContractCanvasPublishApplicationService(ExecutionPublicationPort publicationPort) {
            this.publicationPort = publicationPort;
        }

        /**
         * 处理publish。
         */
        private Long publish(Long tenantId, Long canvasId, Long versionId, String graphJson) {
            PublishedCanvasDefinition definition = new PublishedCanvasDefinition(
                    tenantId,
                    canvasId,
                    versionId,
                    1,
                    graphJson,
                    Instant.parse("2026-06-10T01:00:00Z"),
                    Map.of("publishMode", "standard"),
                    List.of(),
                    List.of());
            publicationPort.publish(definition);
            return definition.versionId();
        }
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
        }
    }
}
