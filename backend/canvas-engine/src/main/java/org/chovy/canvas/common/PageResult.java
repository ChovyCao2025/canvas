package org.chovy.canvas.common;

import lombok.Data;
import java.util.List;

/**
 * 分页响应对象。
 *
 * <p>统一用于列表接口返回，避免每个接口重复定义分页结构。
 * 与前端约定字段固定为 `total + list`，便于通用表格组件复用。
 */
@Data
public class PageResult<T> {

    /** 总记录数（所有页合计）。 */
    private long total;

    /** 当前页数据列表（长度通常 <= pageSize）。 */
    private List<T> list;

// 结构保持扁平，便于前端列表组件直接消费。

    /**
     * 工厂方法。
     *
     * @param total 总记录数（不受分页 size 限制）
     * @param list  当前页数据
     * @return 标准分页返回体
     */
    public static <T> PageResult<T> of(long total, List<T> list) {
        PageResult<T> result = new PageResult<>();
        result.total = total;
        result.list = list;
        return result;
    }
}
