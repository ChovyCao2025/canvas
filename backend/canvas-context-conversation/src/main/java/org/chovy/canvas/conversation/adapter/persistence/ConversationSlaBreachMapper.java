package org.chovy.canvas.conversation.adapter.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * SLA 违约表的 MyBatis Plus 基础访问器。
 */
@Mapper
public interface ConversationSlaBreachMapper extends BaseMapper<ConversationSlaBreachDO> {
}
