package org.chovy.canvas.dto.canvas;

import java.util.Map;

public record UserInputSubmitReq(
        Map<String, Object> response,
        String operator
) {
}
