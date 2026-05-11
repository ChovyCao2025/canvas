package com.photon.canvas.common;

import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private long total;
    private List<T> list;

    public static <T> PageResult<T> of(long total, List<T> list) {
        PageResult<T> result = new PageResult<>();
        result.total = total;
        result.list = list;
        return result;
    }
}
