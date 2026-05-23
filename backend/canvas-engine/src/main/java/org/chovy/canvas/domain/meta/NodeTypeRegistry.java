package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 节点类型注册表（node_type_registry）。
 *
 * <p>系统支持的全部画布节点类型定义，由 Flyway 迁移脚本初始化和维护。
 * 前端节点面板从此表加载可拖拽节点，配置面板根据 configSchema 动态渲染表单。
 */
@Data
@TableName("node_type_registry")
public class NodeTypeRegistry {

    /** 节点类型唯一键，对应 DAG node.type 字段，见 {@link org.chovy.canvas.domain.constant.NodeType} */
    @TableId
    private String typeKey;

    /** 节点显示名称，如"事件触发"、"IF判断" */
    private String typeName;

    /** 节点所属分类，用于前端节点面板分组展示，如"行为策略"、"逻辑分支" */
    private String category;

    /** 处理器实现类全限定名（当前仅用于文档记录，运行时通过 @NodeHandlerType 注册） */
    private String handlerClass;

    /**
     * 前端配置面板 Schema，JSON 数组字符串。
     * 每个元素描述一个配置字段：key、label、type、required、dataSource 等。
     * 为空数组 {@code []} 时表示该节点无需用户配置。
     */
    private String configSchema;

    /**
     * 节点执行后写入上下文的输出字段定义，JSON 数组字符串。
     * 供后续节点通过 ${key} 引用时的提示信息（目前为预留字段）。
     */
    private String outputSchema;

    /** 节点出口定义，驱动前端 handle 和发布校验 */
    private String outletSchema;

    /** 画布卡片摘要模板 */
    private String summaryTemplate;

    /** 节点通用运行策略配置 schema */
    private String runtimePolicySchema;

    /** LOW/MEDIUM/HIGH */
    private String riskLevel;

    /**
     * 是否为触发器节点，1=是。
     * 触发器节点在 DAG 中无入边，只能作为流程入口。
     * 前端据此隐藏 target handle，禁止向该节点连线。
     */
    private Integer isTrigger;

    /**
     * 是否为终止节点，1=是。
     * 终止节点在 DAG 中无出边，只能作为流程末尾。
     * 前端据此隐藏 source handle，禁止从该节点连线。
     */
    private Integer isTerminal;

    /** 节点功能描述，展示在前端节点面板的 tooltip 中 */
    private String description;

    /** 是否在前端节点面板中展示，1=展示，0=隐藏（已废弃或内部节点） */
    private Integer enabled;
}
