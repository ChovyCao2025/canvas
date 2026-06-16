package org.chovy.canvas.canvas.domain;

import java.util.List;
import java.util.Optional;

/**
 * 定义CanvasVersionRepository对外提供的能力契约。
 */
public interface CanvasVersionRepository {

    /**
     * 保存。
     */
    CanvasVersion save(CanvasVersion version);

    /**
     * 处理latestDraft。
     */
    Optional<CanvasVersion> latestDraft(Long canvasId);

    /**
     * 查询by标识。
     */
    Optional<CanvasVersion> findById(Long versionId);

    /**
     * 查询by canvas标识。
     */
    List<CanvasVersion> findByCanvasId(Long canvasId);

    /**
     * 处理nextVersion。
     */
    default int nextVersion(Long canvasId) {
        return findByCanvasId(canvasId).stream()
                .map(CanvasVersion::version)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }
}
