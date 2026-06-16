package org.chovy.canvas.conversation.adapter.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 路由坐席表的 MyBatis Plus 基础访问器。
 */
@Mapper
public interface ConversationRoutingAgentMapper extends BaseMapper<ConversationRoutingAgentDO> {
}
