package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.chovy.canvas.dal.dataobject.ConversationMessageDO;

@Mapper
public interface ConversationMessageMapper extends BaseMapper<ConversationMessageDO> {
}
