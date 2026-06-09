package org.chovy.canvas.dto.canvas;

/**
 * UserInputSubmitResp 承载 dto.canvas 场景中的不可变数据快照。
 * @param responseId responseId 字段。
 * @param status status 字段。
 * @param duplicate duplicate 字段。
 */
public record UserInputSubmitResp(
        Long responseId,
        String status,
        boolean duplicate
) {
}
