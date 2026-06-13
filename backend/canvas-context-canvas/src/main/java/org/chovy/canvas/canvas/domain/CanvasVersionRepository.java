package org.chovy.canvas.canvas.domain;

import java.util.List;
import java.util.Optional;

public interface CanvasVersionRepository {

    CanvasVersion save(CanvasVersion version);

    Optional<CanvasVersion> latestDraft(Long canvasId);

    Optional<CanvasVersion> findById(Long versionId);

    List<CanvasVersion> findByCanvasId(Long canvasId);

    default int nextVersion(Long canvasId) {
        return findByCanvasId(canvasId).stream()
                .map(CanvasVersion::version)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }
}
