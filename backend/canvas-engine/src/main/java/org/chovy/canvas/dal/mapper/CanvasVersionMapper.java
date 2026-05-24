package org.chovy.canvas.dal.mapper;

import org.chovy.canvas.dal.dataobject.CanvasVersionDO;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 画布版本快照 Mapper（表：canvas_version）。
 *
 * <p>保存草稿/发布版本的 graphJson 快照与版本状态。
 */
@Mapper
public interface CanvasVersionMapper extends BaseMapper<CanvasVersionDO> {
    // 版本发布、回滚和清理策略由上层服务与任务编排。
    // 本层仅负责单表持久化，不承担版本关系判断。
    // 查询“最新草稿/最新发布”这类语义由 Service 侧封装。
}
