package org.chovy.canvas.conversation.adapter.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单审计表的 MyBatis Plus 基础访问器。
 */
@Mapper
public interface ConversationWorkItemAuditMapper extends BaseMapper<ConversationWorkItemAuditDO> {
}
