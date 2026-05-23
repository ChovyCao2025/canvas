package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 事件定义 Mapper（表：event_definition）。
 *
 * <p>供事件触发节点与事件上报入口做事件编码校验。
 */
@Mapper
public interface EventDefinitionMapper extends BaseMapper<EventDefinition> {
    // 事件属性结构校验在事件上报入口完成，不在 Mapper 层处理。
    // 触发节点通常按 eventCode 关联本表定义。
    // 事件停用后，相关触发节点应同步下线或调整配置。
}
