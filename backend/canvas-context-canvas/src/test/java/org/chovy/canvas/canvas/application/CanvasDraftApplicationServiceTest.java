package org.chovy.canvas.canvas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.chovy.canvas.canvas.domain.Canvas;
import org.chovy.canvas.canvas.domain.CanvasRepository;
import org.chovy.canvas.canvas.domain.CanvasStatus;
import org.chovy.canvas.canvas.domain.CanvasVersion;
import org.chovy.canvas.canvas.domain.CanvasVersionRepository;
import org.chovy.canvas.canvas.domain.VersionStatus;
import org.junit.jupiter.api.Test;

/**
 * 封装CanvasDraftApplicationServiceTest相关的业务逻辑。
 */
class CanvasDraftApplicationServiceTest {

    /**
     * 创建DraftStoresCanvasAndInitialDraftVersion。
     */
    @Test
    void createDraftStoresCanvasAndInitialDraftVersion() {
        InMemoryCanvasRepository canvases = new InMemoryCanvasRepository();
        InMemoryCanvasVersionRepository versions = new InMemoryCanvasVersionRepository();
        CanvasDraftApplicationService service = new CanvasDraftApplicationService(canvases, versions);

        Canvas canvas = service.createDraft(new CanvasDraftApplicationService.CreateDraftCommand(
                9L,
                "Welcome",
                "desc",
                "{\"nodes\":[]}",
                "operator"));

        assertThat(canvas.id()).isEqualTo(1L);
        assertThat(canvas.status()).isEqualTo(CanvasStatus.DRAFT);
        assertThat(canvas.tenantId()).isEqualTo(9L);
        assertThat(versions.latestDraft(canvas.id())).get()
                .extracting(CanvasVersion::version, CanvasVersion::status, CanvasVersion::graphJson)
                .containsExactly(1, VersionStatus.DRAFT, "{\"nodes\":[]}");
    }

    /**
     * 更新PublishedCanvasCreatesNewDraftVersionInsteadOfMutatingExistingDraft。
     */
    @Test
    void updatePublishedCanvasCreatesNewDraftVersionInsteadOfMutatingExistingDraft() {
        InMemoryCanvasRepository canvases = new InMemoryCanvasRepository();
        InMemoryCanvasVersionRepository versions = new InMemoryCanvasVersionRepository();
        CanvasDraftApplicationService service = new CanvasDraftApplicationService(canvases, versions);
        Canvas canvas = canvases.save(Canvas.createDraft(1L, 9L, "Published", "desc", "creator"));
        canvases.save(canvas.publish(101L));
        versions.save(CanvasVersion.draft(10L, canvas.id(), 9L, 2, "{\"nodes\":[]}", "creator"));

        service.updateDraft(canvas.id(), new CanvasDraftApplicationService.UpdateDraftCommand(
                "Published edited",
                "desc edited",
                "{\"nodes\":[{\"id\":\"start\"}]}",
                "editor"));

        assertThat(versions.all()).hasSize(2);
        assertThat(versions.latestDraft(canvas.id())).get()
                .extracting(CanvasVersion::version, CanvasVersion::graphJson, CanvasVersion::createdBy)
                .containsExactly(3, "{\"nodes\":[{\"id\":\"start\"}]}", "editor");
    }

    /**
     * 更新KilledCanvasIsRejected。
     */
    @Test
    void updateKilledCanvasIsRejected() {
        InMemoryCanvasRepository canvases = new InMemoryCanvasRepository();
        InMemoryCanvasVersionRepository versions = new InMemoryCanvasVersionRepository();
        CanvasDraftApplicationService service = new CanvasDraftApplicationService(canvases, versions);
        Canvas canvas = canvases.save(Canvas.createDraft(1L, 9L, "Killed", "desc", "creator").kill());

        assertThatThrownBy(() -> service.updateDraft(canvas.id(), new CanvasDraftApplicationService.UpdateDraftCommand(
                "x", "y", "{\"nodes\":[]}", "editor")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("KILLED");
    }

    /**
     * 封装InMemoryCanvasRepository相关的业务逻辑。
     */
    static final class InMemoryCanvasRepository implements CanvasRepository {

        /**
         * 保存测试或内存实现使用的rows列表。
         */
        private final List<Canvas> rows = new ArrayList<>();

        /**
         * 保存next标识。
         */
        private long nextId = 1L;

        /**
         * 保存。
         */
        @Override
        public Canvas save(Canvas canvas) {
            Canvas saved = canvas.id() == null ? canvas.withId(nextId++) : canvas;
            rows.removeIf(row -> row.id().equals(saved.id()));
            rows.add(saved);
            return saved;
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
    static final class InMemoryCanvasVersionRepository implements CanvasVersionRepository {

        /**
         * 保存测试或内存实现使用的rows列表。
         */
        private final List<CanvasVersion> rows = new ArrayList<>();

        /**
         * 保存next标识。
         */
        private long nextId = 1L;

        /**
         * 保存。
         */
        @Override
        public CanvasVersion save(CanvasVersion version) {
            CanvasVersion saved = version.id() == null ? version.withId(nextId++) : version;
            rows.removeIf(row -> row.id().equals(saved.id()));
            rows.add(saved);
            return saved;
        }

        /**
         * 处理latestDraft。
         */
        @Override
        public Optional<CanvasVersion> latestDraft(Long canvasId) {
            return rows.stream()
                    .filter(row -> row.canvasId().equals(canvasId))
                    .filter(row -> row.status() == VersionStatus.DRAFT)
                    .max(java.util.Comparator.comparing(CanvasVersion::version));
        }

        /**
         * 查询by标识。
         */
        @Override
        public Optional<CanvasVersion> findById(Long versionId) {
            return rows.stream().filter(row -> row.id().equals(versionId)).findFirst();
        }

        /**
         * 查询by canvas标识。
         */
        @Override
        public List<CanvasVersion> findByCanvasId(Long canvasId) {
            return rows.stream().filter(row -> row.canvasId().equals(canvasId)).toList();
        }

        /**
         * 处理all。
         */
        List<CanvasVersion> all() {
            return rows;
        }
    }
}
