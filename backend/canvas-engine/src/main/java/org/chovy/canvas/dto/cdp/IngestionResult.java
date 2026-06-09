package org.chovy.canvas.dto.cdp;

import java.util.List;

/**
 * IngestionResult 承载 dto.cdp 场景中的不可变数据快照。
 * @param accepted accepted 字段。
 * @param rejected rejected 字段。
 * @param errors errors 字段。
 */
public record IngestionResult(int accepted, int rejected, List<IngestionError> errors) {
}
