package org.chovy.canvas.conversation.adapter.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 联系人画像表的 MyBatis Plus 基础访问器。
 */
@Mapper
public interface ConversationContactProfileMapper extends BaseMapper<ConversationContactProfileDO> {
}
