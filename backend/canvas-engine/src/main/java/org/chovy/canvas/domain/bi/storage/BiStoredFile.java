package org.chovy.canvas.domain.bi.storage;

/**
 * BiStoredFile 承载 domain.bi.storage 场景中的不可变数据快照。
 * @param provider provider 字段。
 * @param key key 字段。
 * @param path path 字段。
 * @param sizeBytes sizeBytes 字段。
 */
public record BiStoredFile(
        String provider,
        String key,
        String path,
        Long sizeBytes) {
}
