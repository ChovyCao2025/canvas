package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.chovy.canvas.dal.dataobject.BiQueryGovernancePolicyDO;

@Mapper
public interface BiQueryGovernancePolicyMapper extends BaseMapper<BiQueryGovernancePolicyDO> {

    @Override
    int insert(BiQueryGovernancePolicyDO entity);

    @Override
    int updateById(BiQueryGovernancePolicyDO entity);
}
