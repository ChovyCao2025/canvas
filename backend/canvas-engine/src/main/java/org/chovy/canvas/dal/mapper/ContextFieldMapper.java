package org.chovy.canvas.dal.mapper;

import org.chovy.canvas.dal.dataobject.ContextFieldDO;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 上下文字段定义 Mapper（表：context_field）。
 *
 * <p>维护可在节点配置中引用的上下文字段元数据。
 */
@Mapper
public interface ContextFieldMapper extends BaseMapper<ContextFieldDO> {
    // 字段校验与展示逻辑由前后端配置面板共同完成。
    // 该表主要用于元数据驱动 UI，不直接参与执行链路运算。
    // 字段类型变更需评估历史节点配置兼容性。
}
