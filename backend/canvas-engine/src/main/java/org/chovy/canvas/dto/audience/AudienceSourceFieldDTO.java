package org.chovy.canvas.dto.audience;

/**
 * AudienceSourceFieldDTO 承载 dto.audience 场景中的不可变数据快照。
 * @param name name 字段。
 * @param label label 字段。
 * @param valueType valueType 字段。
 */
public record AudienceSourceFieldDTO(
        String name,
        String label,
        String valueType
) {}
