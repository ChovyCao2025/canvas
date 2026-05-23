package org.chovy.canvas.domain.meta;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 通用枚举选项 VO（非数据库实体）。
 *
 * <p>用于各元数据接口（事件定义、API 定义、标签定义等）的下拉选项响应，
 * 前端配置面板 {@code type=select} 字段通过 dataSource 接口获取此格式数据。
 */
@Data
@AllArgsConstructor
public class StubOption {

    /** 选项值，存储到节点 bizConfig 的字段值。 */
    private String key;

    /** 选项显示文本，展示在下拉菜单中。 */
    private String label;

    // 该结构刻意保持最小字段，便于被不同前端表单组件复用。
}
