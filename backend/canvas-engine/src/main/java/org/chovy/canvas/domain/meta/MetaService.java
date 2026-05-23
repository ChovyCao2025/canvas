package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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

    /** 上下文字段定义 Mapper。 */
    private final ContextFieldMapper contextFieldMapper;

    /** 查询全部启用节点类型（按分类排序）。 */
    public List<NodeTypeRegistry> getAllNodeTypes() {
        return nodeTypeRegistryMapper.selectList(
                new LambdaQueryWrapper<NodeTypeRegistry>()
                        .eq(NodeTypeRegistry::getEnabled, 1)
                        .orderByAsc(NodeTypeRegistry::getCategory)
        );
    }

    /** 查询指定节点类型的 schema 配置。 */
    public NodeTypeRegistry getNodeTypeSchema(String typeKey) {
        return nodeTypeRegistryMapper.selectById(typeKey);
    }

    /** 查询全部上下文字段定义。 */
    public List<ContextField> getAllContextFields() {
        return contextFieldMapper.selectList(null);
    }
}
