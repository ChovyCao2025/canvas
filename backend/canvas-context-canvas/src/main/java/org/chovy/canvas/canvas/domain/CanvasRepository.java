package org.chovy.canvas.canvas.domain;

import java.util.Optional;

/**
 * 定义CanvasRepository对外提供的能力契约。
 */
public interface CanvasRepository {

    /**
     * 保存。
     */
    Canvas save(Canvas canvas);

    /**
     * 查询by标识。
     */
    Optional<Canvas> findById(Long canvasId);

    /**
     * 分页查询画布列表。
     */
    default CanvasPage<CanvasListItem> list(CanvasListQuery query) {
        return CanvasPage.of(0, java.util.List.of());
    }
}
