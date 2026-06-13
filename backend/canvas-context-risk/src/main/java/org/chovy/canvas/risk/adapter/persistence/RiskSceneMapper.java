package org.chovy.canvas.risk.adapter.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.chovy.canvas.risk.adapter.persistence.RiskSceneDO;

/**
 * 风控场景 MyBatis Mapper。
 */
@Mapper
public interface RiskSceneMapper extends BaseMapper<RiskSceneDO> {
}
