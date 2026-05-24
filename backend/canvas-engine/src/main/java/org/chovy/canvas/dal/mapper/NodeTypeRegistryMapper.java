package org.chovy.canvas.dal.mapper;

import org.chovy.canvas.dal.dataobject.NodeTypeRegistryDO;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 节点类型注册表 Mapper（表：node_type_registry）。
 *
 * <p>用于前端节点面板渲染和后端节点类型元数据查询。
 */
@Mapper
public interface NodeTypeRegistryMapper extends BaseMapper<NodeTypeRegistryDO> {
    // 节点运行逻辑由 NodeHandlerRegistry + 各 Handler 实现，不在表层处理。
    // 前端节点面板渲染优先依赖该表的类型与分类信息。
    // type_key 变更会影响前后端协议，需谨慎迁移。
}
