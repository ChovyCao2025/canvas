package org.chovy.canvas.dto;

import org.chovy.canvas.domain.canvas.Canvas;
import lombok.Data;

@Data
public class CanvasDetailDTO {
    private Canvas canvas;
    /** 当前草稿的 graph JSON */
    private String graphJson;
    private Long draftVersionId;
}
