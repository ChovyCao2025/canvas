package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 画布模板 Mapper（表：canvas_template）。
 *
 * <p>用于模板中心的模板读取、创建与更新。
 */
@Mapper
public interface CanvasTemplateMapper extends BaseMapper<CanvasTemplate> {
    // 模板复制创建画布等组合操作在 CanvasService 层实现。
    // 模板的版本化策略若需要扩展，也应在 Service 层处理。
    // 模板启用/禁用等业务状态也建议在 Service 统一控制。
}
