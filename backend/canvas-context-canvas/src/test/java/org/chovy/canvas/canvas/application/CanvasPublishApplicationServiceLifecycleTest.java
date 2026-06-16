package org.chovy.canvas.canvas.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.chovy.canvas.canvas.api.ExecutionPublicationPort;
import org.chovy.canvas.canvas.api.PublishedCanvasDefinition;
import org.chovy.canvas.canvas.domain.Canvas;
import org.chovy.canvas.canvas.domain.CanvasRepository;
import org.chovy.canvas.canvas.domain.CanvasRuntimeOptions;
import org.chovy.canvas.canvas.domain.CanvasStatus;
import org.chovy.canvas.canvas.domain.CanvasVersion;
import org.chovy.canvas.canvas.domain.CanvasVersionRepository;
import org.chovy.canvas.canvas.domain.VersionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 封装CanvasPublishApplicationServiceLifecycleTest相关的业务逻辑。
 */
class CanvasPublishApplicationServiceLifecycleTest {

    /**
     * 处理publishCreatesImmutableVersionUpdatesCanvasAndCallsExecutionPort。
     */
    @Test
    void publishCreatesImmutableVersionUpdatesCanvasAndCallsExecutionPort() {
        CanvasDraftApplicationServiceTest.InMemoryCanvasRepository canvases =
                new CanvasDraftApplicationServiceTest.InMemoryCanvasRepository();
        CanvasDraftApplicationServiceTest.InMemoryCanvasVersionRepository versions =
                new CanvasDraftApplicationServiceTest.InMemoryCanvasVersionRepository();
        CapturingPublicationPort port = new CapturingPublicationPort();
        Canvas canvas = canvases.save(Canvas.createDraft(10L, 9L, "Welcome", "desc", "creator"));
        versions.save(CanvasVersion.draft(20L, canvas.id(), 9L, 1,
                "{\"nodes\":[{\"id\":\"start\",\"type\":\"webhook\"}]}", "creator"));
        CanvasPublishApplicationService service = new CanvasPublishApplicationService(canvases, versions, port);

        CanvasVersion published = service.publish(canvas.id(), "publisher");

        assertThat(published.status()).isEqualTo(VersionStatus.PUBLISHED);
        assertThat(published.version()).isEqualTo(2);
        assertThat(canvases.findById(canvas.id())).get()
                .extracting(Canvas::status, Canvas::publishedVersionId)
                .containsExactly(CanvasStatus.PUBLISHED, published.id());
        assertThat(port.published).isNotNull();
        assertThat(port.published.tenantId()).isEqualTo(9L);
        assertThat(port.published.canvasId()).isEqualTo(canvas.id());
        assertThat(port.published.versionId()).isEqualTo(published.id());
        assertThat(port.published.graphJson()).contains("start");
    }

    /**
     * 处理publishIncludesRuntimeOptionsAndParsedGraphDefinitionForExecution。
     */
    @Test
    void publishIncludesRuntimeOptionsAndParsedGraphDefinitionForExecution() {
        CanvasDraftApplicationServiceTest.InMemoryCanvasRepository canvases =
                new CanvasDraftApplicationServiceTest.InMemoryCanvasRepository();
        CanvasDraftApplicationServiceTest.InMemoryCanvasVersionRepository versions =
                new CanvasDraftApplicationServiceTest.InMemoryCanvasVersionRepository();
        CapturingPublicationPort port = new CapturingPublicationPort();
        Canvas canvas = canvases.save(Canvas.createDraft(10L, 9L, "Welcome", "desc", "creator")
                .withRuntimeOptions(new CanvasRuntimeOptions(
                        "SCHEDULED",
                        "0 0 10 * * ?",
                        "2026-01-01T00:00",
                        "2026-12-31T23:59",
                        1000,
                        3,
                        10,
                        60,
                        15,
                        "salt-a",
                        "ORDER_PAID",
                        14,
                        "LAST_TOUCH")));
        versions.save(CanvasVersion.draft(20L, canvas.id(), 9L, 1, """
                {"nodes":[{"id":"start","type":"webhook","name":"Start","config":{"event":"signup"},"position":{"x":10,"y":20},"metadata":{"lane":"trigger"}}],
                 "edges":[{"id":"e1","from":"start","to":"end","condition":{"expr":"ok"},"metadata":{"label":"yes"}}]}
                """, "creator"));
        CanvasPublishApplicationService service = new CanvasPublishApplicationService(canvases, versions, port);

        service.publish(canvas.id(), "publisher");

        assertThat(port.published.executionOptions())
                .containsEntry("triggerType", "SCHEDULED")
                .containsEntry("cronExpression", "0 0 10 * * ?")
                .containsEntry("validStart", "2026-01-01T00:00")
                .containsEntry("validEnd", "2026-12-31T23:59")
                .containsEntry("maxTotalExecutions", 1000)
                .containsEntry("perUserDailyLimit", 3)
                .containsEntry("perUserTotalLimit", 10)
                .containsEntry("cooldownSeconds", 60)
                .containsEntry("controlGroupPercent", 15)
                .containsEntry("controlGroupSalt", "salt-a")
                .containsEntry("conversionEventCode", "ORDER_PAID")
                .containsEntry("attributionWindowDays", 14)
                .containsEntry("attributionModel", "LAST_TOUCH");
        assertThat(port.published.nodes()).hasSize(1);
        assertThat(port.published.nodes().get(0).nodeId()).isEqualTo("start");
        assertThat(port.published.nodes().get(0).nodeType()).isEqualTo("webhook");
        assertThat(port.published.nodes().get(0).displayName()).isEqualTo("Start");
        assertThat(port.published.nodes().get(0).configJson()).contains("\"event\":\"signup\"");
        assertThat(port.published.nodes().get(0).position()).containsEntry("x", 10);
        assertThat(port.published.nodes().get(0).metadata()).containsEntry("lane", "trigger");
        assertThat(port.published.edges()).hasSize(1);
        assertThat(port.published.edges().get(0).edgeId()).isEqualTo("e1");
        assertThat(port.published.edges().get(0).sourceNodeId()).isEqualTo("start");
        assertThat(port.published.edges().get(0).targetNodeId()).isEqualTo("end");
        assertThat(port.published.edges().get(0).conditionJson()).contains("\"expr\":\"ok\"");
        assertThat(port.published.edges().get(0).metadata()).containsEntry("label", "yes");
    }

    /**
     * 处理publishDefersExecutionPublicationUntilAfterCommitWhenTransactionSynchronizationIsActive。
     */
    @Test
    void publishDefersExecutionPublicationUntilAfterCommitWhenTransactionSynchronizationIsActive() {
        CanvasDraftApplicationServiceTest.InMemoryCanvasRepository canvases =
                new CanvasDraftApplicationServiceTest.InMemoryCanvasRepository();
        CanvasDraftApplicationServiceTest.InMemoryCanvasVersionRepository versions =
                new CanvasDraftApplicationServiceTest.InMemoryCanvasVersionRepository();
        CapturingPublicationPort port = new CapturingPublicationPort();
        Canvas canvas = canvases.save(Canvas.createDraft(10L, 9L, "Welcome", "desc", "creator"));
        versions.save(CanvasVersion.draft(20L, canvas.id(), 9L, 1, "{\"nodes\":[]}", "creator"));
        CanvasPublishApplicationService service = new CanvasPublishApplicationService(canvases, versions, port);

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.publish(canvas.id(), "publisher");

            assertThat(port.published).isNull();
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(synchronization -> synchronization.afterCommit());
            assertThat(port.published).isNotNull();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    /**
     * 处理unpublishClearsPublishedPointerAndCallsExecutionPort。
     */
    @Test
    void unpublishClearsPublishedPointerAndCallsExecutionPort() {
        InMemoryCanvasRepository canvases = new InMemoryCanvasRepository();
        InMemoryCanvasVersionRepository versions = new InMemoryCanvasVersionRepository();
        CapturingPublicationPort port = new CapturingPublicationPort();
        Canvas canvas = canvases.save(Canvas.createDraft(10L, 9L, "Welcome", "desc", "creator").publish(20L));
        CanvasPublishApplicationService service = new CanvasPublishApplicationService(canvases, versions, port);

        service.unpublish(canvas.id());

        assertThat(canvases.findById(canvas.id())).get()
                .extracting(Canvas::status, Canvas::publishedVersionId)
                .containsExactly(CanvasStatus.OFFLINE, null);
        assertThat(port.unpublishedTenantId).isEqualTo(9L);
        assertThat(port.unpublishedCanvasId).isEqualTo(canvas.id());
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

    /**
     * 封装InMemoryCanvasRepository相关的业务逻辑。
     */
    private static final class InMemoryCanvasRepository implements CanvasRepository {

        /**
         * 保存测试或内存实现使用的rows列表。
         */
        private final List<Canvas> rows = new ArrayList<>();

        /**
         * 保存。
         */
        @Override
        public Canvas save(Canvas canvas) {
            rows.removeIf(row -> row.id().equals(canvas.id()));
            rows.add(canvas);
            return canvas;
        }

        /**
         * 查询by标识。
         */
        @Override
        public Optional<Canvas> findById(Long canvasId) {
            return rows.stream().filter(row -> row.id().equals(canvasId)).findFirst();
        }
    }

    /**
     * 封装InMemoryCanvasVersionRepository相关的业务逻辑。
     */
    private static final class InMemoryCanvasVersionRepository implements CanvasVersionRepository {

        /**
         * 保存。
         */
        @Override
        public CanvasVersion save(CanvasVersion version) {
            return version;
        }

        /**
         * 处理latestDraft。
         */
        @Override
        public Optional<CanvasVersion> latestDraft(Long canvasId) {
            return Optional.empty();
        }

        /**
         * 查询by标识。
         */
        @Override
        public Optional<CanvasVersion> findById(Long versionId) {
            return Optional.empty();
        }

        /**
         * 查询by canvas标识。
         */
        @Override
        public List<CanvasVersion> findByCanvasId(Long canvasId) {
            return List.of();
        }
    }
}
