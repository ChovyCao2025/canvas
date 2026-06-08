package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.chovy.canvas.dal.dataobject.BiQueryGovernancePolicyDO;

/**
 * BiQueryGovernancePolicyMapper 定义 dal.mapper 场景中的扩展契约。
 */
@Mapper
public interface BiQueryGovernancePolicyMapper extends BaseMapper<BiQueryGovernancePolicyDO> {

    /**
     * 执行数据写入或状态变更。
     *
     * @param entity entity 参数，用于 insert 流程中的校验、计算或对象转换。
     * @return 返回 insert 计算得到的数量、金额或指标值。
     */
    @Override
    int insert(BiQueryGovernancePolicyDO entity);

    /**
     * 执行数据写入或状态变更。
     *
     * @param entity entity 参数，用于 updateById 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    @Override
    int updateById(BiQueryGovernancePolicyDO entity);
}
