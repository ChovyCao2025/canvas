package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.chovy.canvas.dal.dataobject.ConversationSessionDO;

@Mapper
public interface ConversationSessionMapper extends BaseMapper<ConversationSessionDO> {
}
