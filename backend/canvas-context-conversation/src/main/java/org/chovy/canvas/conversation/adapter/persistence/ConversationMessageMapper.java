package org.chovy.canvas.conversation.adapter.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会话消息表的 MyBatis Plus 基础访问器。
 */
@Mapper
public interface ConversationMessageMapper extends BaseMapper<ConversationMessageDO> {
}
