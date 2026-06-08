package org.chovy.canvas.domain.bi.query;

/**
 * BiSort 承载 domain.bi.query 场景中的不可变数据快照。
 * @param field field 字段。
 * @param direction direction 字段。
 */
public record BiSort(
        String field,
        Direction direction
) {
    /**
     * Direction 枚举类型。
     */
    public enum Direction {
        ASC,
        DESC
    }
}
