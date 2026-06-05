package org.chovy.canvas.dto.canvas;

public record UserInputSubmitResp(
        Long responseId,
        String status,
        boolean duplicate
) {
}
