package org.chovy.canvas.dal.mapper;

import org.chovy.canvas.dal.dataobject.TagDefinitionDO;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 标签定义 Mapper（表：tag_definition）。
 *
 * <p>供标签相关节点配置时查询 tagCode/tagName 元数据。
 */
@Mapper
public interface TagDefinitionMapper extends BaseMapper<TagDefinitionDO> {
    // 标签实时/离线查询在外部标签系统，当前表仅维护元信息映射。
    // 节点配置时通过 tagCode 引用该表定义，避免硬编码标签 ID。
    // 标签值计算逻辑不在本系统内实现。
}
