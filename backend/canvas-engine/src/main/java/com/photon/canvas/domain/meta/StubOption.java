package com.photon.canvas.domain.meta;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 给各 stub 元数据接口使用的通用选项 VO */
@Data
@AllArgsConstructor
public class StubOption {
    private String key;
    private String label;
}
