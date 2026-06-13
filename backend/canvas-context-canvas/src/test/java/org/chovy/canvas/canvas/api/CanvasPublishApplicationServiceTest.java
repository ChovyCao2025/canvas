package org.chovy.canvas.canvas.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class CanvasPublishApplicationServiceTest {

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

    private static final class ContractCanvasPublishApplicationService {
        private final ExecutionPublicationPort publicationPort;

        private ContractCanvasPublishApplicationService(ExecutionPublicationPort publicationPort) {
            this.publicationPort = publicationPort;
        }

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

    private static final class CapturingPublicationPort implements ExecutionPublicationPort {
        private PublishedCanvasDefinition published;

        @Override
        public void publish(PublishedCanvasDefinition definition) {
            this.published = definition;
        }

        @Override
        public void unpublish(Long tenantId, Long canvasId) {
        }
    }
}
