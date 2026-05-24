package org.chovy.canvas.dal.mapper;

import org.chovy.canvas.dal.dataobject.CustomerProfileDO;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CustomerProfileMapper extends BaseMapper<CustomerProfileDO> {
}
