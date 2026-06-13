package org.chovy.canvas.canvas.domain;

import java.util.Optional;

public interface CanvasRepository {

    Canvas save(Canvas canvas);

    Optional<Canvas> findById(Long canvasId);
}
