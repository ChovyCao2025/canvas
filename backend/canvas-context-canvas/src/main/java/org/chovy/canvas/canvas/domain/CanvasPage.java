package org.chovy.canvas.canvas.domain;

import java.util.List;

/**
 * 标准分页结果。
 */
public record CanvasPage<T>(long total, List<T> list) {

    public static <T> CanvasPage<T> of(long total, List<T> list) {
        return new CanvasPage<>(total, list == null ? List.of() : list);
    }
}
