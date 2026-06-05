package org.chovy.canvas.dto.canvas;

import org.chovy.canvas.dal.dataobject.CanvasDO;

public record CanvasImportResp(CanvasDO canvas, Long draftVersionId) {
}
