package org.chovy.canvas.dal.mapper;

import org.chovy.canvas.dal.dataobject.AbExperimentDO;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * A/B 实验定义 Mapper（表：ab_experiment）。
 *
 * <p>用于 AB 节点引用实验配置时读取分组规则。
 */
@Mapper
public interface AbExperimentMapper extends BaseMapper<AbExperimentDO> {
    // 分流算法执行在 AbSplitHandler，这里只保存实验配置元数据。
    // 实验比例调整后，画布节点读取到的是最新启用配置。
    // 灰度发布/版本化等更复杂策略建议由实验平台侧托管。
}
