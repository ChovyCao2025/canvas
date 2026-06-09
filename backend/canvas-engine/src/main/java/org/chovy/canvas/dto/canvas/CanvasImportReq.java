package org.chovy.canvas.dto.canvas;

/**
 * CanvasImportReq 承载 dto.canvas 场景中的不可变数据快照。
 * @param packageJson packageJson 字段。
 * @param operator operator 字段。
 */
public record CanvasImportReq(String packageJson, String operator) {
}
