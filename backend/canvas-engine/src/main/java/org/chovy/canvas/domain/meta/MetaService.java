package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import org.chovy.canvas.dal.dataobject.ContextFieldDO;
import org.chovy.canvas.dal.mapper.ContextFieldMapper;
import org.chovy.canvas.dal.dataobject.NodeTypeRegistryDO;
import org.chovy.canvas.dal.mapper.NodeTypeRegistryMapper;

/**
 * 元数据读取服务。
 *
 * 职责边界：
 * 1) 提供节点类型、上下文字段等配置元数据查询；
 * 2) 提供当前阶段 stub 选项数据；
 * 3) 不负责执行引擎运行时决策。
 */
@Service
@RequiredArgsConstructor
public class MetaService {

    /** 节点类型注册表 Mapper。 */
    private final NodeTypeRegistryMapper nodeTypeRegistryMapper;

    /** 执行上下文字段 Mapper。 */
    private final ContextFieldMapper contextFieldMapper;

    /** 查询全部启用节点类型（按分类排序）。 */
    public List<NodeTypeRegistryDO> getAllNodeTypes() {
        return nodeTypeRegistryMapper.selectList(
                new LambdaQueryWrapper<NodeTypeRegistryDO>()
                        .eq(NodeTypeRegistryDO::getEnabled, 1)
                        .orderByAsc(NodeTypeRegistryDO::getCategory)
        );
    }

    /** 查询指定节点类型的 schema 配置。 */
    public NodeTypeRegistryDO getNodeTypeSchema(String typeKey) {
        return nodeTypeRegistryMapper.selectById(typeKey);
    }

    /** 查询全部上下文字段定义。 */
    public List<ContextFieldDO> getAllContextFields() {
        return contextFieldMapper.selectList(null);
    }
}
