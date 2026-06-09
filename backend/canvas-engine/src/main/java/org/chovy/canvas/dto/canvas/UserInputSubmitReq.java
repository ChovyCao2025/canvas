package org.chovy.canvas.dto.canvas;

import java.util.Map;

/**
 * UserInputSubmitReq 承载 dto.canvas 场景中的不可变数据快照。
 * @param response response 字段。
 * @param operator operator 字段。
 */
public record UserInputSubmitReq(
        Map<String, Object> response,
        String operator
) {
}
