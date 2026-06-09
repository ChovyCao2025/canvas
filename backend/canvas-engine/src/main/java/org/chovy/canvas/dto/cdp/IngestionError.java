package org.chovy.canvas.dto.cdp;

/**
 * IngestionError 承载 dto.cdp 场景中的不可变数据快照。
 * @param messageId messageId 字段。
 * @param code code 字段。
 * @param message message 字段。
 */
public record IngestionError(String messageId, String code, String message) {
}
