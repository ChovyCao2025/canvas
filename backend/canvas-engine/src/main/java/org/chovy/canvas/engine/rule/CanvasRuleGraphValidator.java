package org.chovy.canvas.engine.rule;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CanvasRuleGraphValidator 参与 engine.rule 场景的画布执行引擎处理。
 */
@Component
public class CanvasRuleGraphValidator {

    private static final Set<String> SAFE_MULTI_UPSTREAM_TYPES = Set.of(
            NodeType.HUB,
            NodeType.AGGREGATE,
            NodeType.THRESHOLD,
            NodeType.END
    );

    private final RuleParser ruleParser;
    private final RuleValidator ruleValidator;

    @Value("${canvas.validation.max-node-count:1000}")
    private int maxNodeCount = 1000;

    @Value("${canvas.validation.max-goto-jumps:100}")
    private int maxGotoJumps = 100;

    /** 单个 LOOP 节点允许的最大迭代次数。 */
    @Value("${canvas.validation.max-loop-iterations:1000}")
    private int maxLoopIterations = 1000;

    /**
     * 创建画布规则图校验器。
     *
     * @param ruleParser 规则解析器
     * @param ruleValidator 规则校验器
     */
    public CanvasRuleGraphValidator(RuleParser ruleParser, RuleValidator ruleValidator) {
        this.ruleParser = ruleParser;
        this.ruleValidator = ruleValidator;
    }

    /**
     * validateOrThrow 校验或转换 engine.rule 场景的数据。
     * @param graph graph 参数，用于 validateOrThrow 流程中的校验、计算或对象转换。
     */
    public void validateOrThrow(DagGraph graph) {
        List<String> errors = new ArrayList<>();
        validateGraphLimits(graph, errors);
        for (Map.Entry<String, DagParser.CanvasNode> entry : graph.getNodeMap().entrySet()) {
            String nodeId = entry.getKey();
            DagParser.CanvasNode node = entry.getValue();
            // 节点配置可能保存在旧版 config 或 bizConfig 中，校验必须读取合并后的运行时视图。
            Map<String, Object> config = mergedConfig(node);
            validateNodeRules(nodeId, node, config, errors);
            validateRuntimeGuards(nodeId, node, config, errors);
            validateTopology(nodeId, node, graph, errors);
            validateScheduledTriggerTopology(nodeId, node, graph, errors);
        }
        if (!errors.isEmpty()) {
            throw new RuleValidationException("发布校验失败：\n" + String.join("\n", errors));
        }
    }

    /**
     * 校验画布图级别的规模限制。
     *
     * @param graph DAG 图
     * @param errors 错误收集列表
     */
    private void validateGraphLimits(DagGraph graph, List<String> errors) {
        if (graph.getNodeMap().size() > maxNodeCount) {
            errors.add("节点数量 " + graph.getNodeMap().size() + " 超过最大限制 " + maxNodeCount);
        }
    }

    /**
     * 校验节点配置中的规则表达式。
     *
     * @param nodeId 节点 ID
     * @param node 节点定义
     * @param config 合并后的节点配置
     * @param errors 错误收集列表
     */
    @SuppressWarnings("unchecked")
    private void validateNodeRules(String nodeId,
                                   DagParser.CanvasNode node,
                                   Map<String, Object> config,
                                   List<String> errors) {
        String type = node.getType();
        try {
            if (NodeType.IF_CONDITION.equals(type)) {
                RuleGroup rule = ruleParser.parseCanvasRules((List<Map<String, Object>>) config.get("rules"));
                RuleValidationOptions options = options(nodeId, "rules", Boolean.TRUE.equals(config.get("matchAll")));
                errors.addAll(ruleValidator.validate(rule, options));
            }
            if (NodeType.MQ_TRIGGER.equals(type)
                    || NodeType.API_CALL.equals(type)
                    || NodeType.GROOVY.equals(type)) {
                if (Boolean.TRUE.equals(config.get(MapFieldKeys.VALIDATE_RESULT))) {
                    RuleGroup rule = ruleParser.parseCanvasRules((List<Map<String, Object>>) config.get(MapFieldKeys.VALIDATE_RULES));
                    errors.addAll(ruleValidator.validate(rule, options(nodeId, MapFieldKeys.VALIDATE_RULES, false)));
                }
            }
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuleValidationException e) {
            errors.add(nodeId + ": " + e.getMessage());
        }
    }

    /**
     * 校验多上游汇入拓扑是否经过安全汇聚节点。
     *
     * @param nodeId 节点 ID
     * @param node 节点定义
     * @param graph DAG 图
     * @param errors 错误收集列表
     */
    private void validateTopology(String nodeId,
                                  DagParser.CanvasNode node,
                                  DagGraph graph,
                                  List<String> errors) {
        if (graph.upstream(nodeId).size() <= 1) {
            return;
        }
        if (SAFE_MULTI_UPSTREAM_TYPES.contains(node.getType())) {
            return;
        }
        // 多个上游分支若不经过明确的安全汇聚节点，可能导致副作用重复执行。
        errors.add(nodeId + ": 多分支汇入副作用/普通节点必须先经过汇聚节点");
    }

    /**
     * 校验 GOTO 和 LOOP 的运行时保护上限。
     *
     * @param nodeId 节点 ID
     * @param node 节点定义
     * @param config 合并后的节点配置
     * @param errors 错误收集列表
     */
    private void validateRuntimeGuards(String nodeId,
                                       DagParser.CanvasNode node,
                                       Map<String, Object> config,
                                       List<String> errors) {
        String type = node.getType();
        if ("GOTO".equalsIgnoreCase(type)) {
            Integer maxJumps = integerValue(config.get("maxJumps"));
            if (maxJumps == null || maxJumps <= 0) {
                errors.add(nodeId + ": goto maxJumps 必须配置为正整数");
            // 根据前序判断结果进入后续条件分支。
            } else if (maxJumps > maxGotoJumps) {
                errors.add(nodeId + ": goto maxJumps " + maxJumps + " 超过最大限制 " + maxGotoJumps);
            }
        }
        if ("LOOP".equalsIgnoreCase(type)) {
            Integer maxIterations = integerValue(config.get("maxIterations"));
            if (maxIterations == null || maxIterations <= 0) {
                errors.add(nodeId + ": loop maxIterations 必须配置为正整数");
            // 根据前序判断结果进入后续条件分支。
            } else if (maxIterations > maxLoopIterations) {
                errors.add(nodeId + ": loop maxIterations " + maxIterations
                        + " 超过最大限制 " + maxLoopIterations);
            }
        }
    }

    /**
     * 校验定时触发节点下游必须先连接人群筛选节点。
     *
     * @param nodeId 节点 ID
     * @param node 节点定义
     * @param graph DAG 图
     * @param errors 错误收集列表
     */
    private void validateScheduledTriggerTopology(String nodeId,
                                                  DagParser.CanvasNode node,
                                                  DagGraph graph,
                                                  List<String> errors) {
        if (!NodeType.SCHEDULED_TRIGGER.equals(node.getType())) {
            return;
        }
        List<String> downstream = graph.downstream(nodeId);
        if (downstream.isEmpty()) {
            errors.add(nodeId + ": 定时触发节点只负责定时，必须连接一个人群筛选 TAGGER 节点");
            return;
        }
        for (String downstreamId : downstream) {
            DagParser.CanvasNode downstreamNode = graph.getNode(downstreamId);
            Map<String, Object> downstreamConfig = downstreamNode == null ? Map.of() : mergedConfig(downstreamNode);
            // 定时触发节点只定义时间，首个业务节点必须先物化目标人群。
            boolean audienceTagger = downstreamNode != null
                    && NodeType.TAGGER.equals(downstreamNode.getType())
                    && "audience".equals(String.valueOf(downstreamConfig.getOrDefault("mode", "")));
            if (!audienceTagger) {
                errors.add(nodeId + ": 定时触发节点只负责定时，下游必须先连接人群筛选 TAGGER 节点");
            }
        }
    }

    /**
     * 构造规则校验选项。
     *
     * @param nodeId 节点 ID
     * @param field 配置字段名
     * @param allowEmpty 是否允许空规则
     * @return 规则校验选项
     */
    private RuleValidationOptions options(String nodeId, String field, boolean allowEmpty) {
        RuleValidationOptions options = RuleValidationOptions.strict(nodeId + "." + field);
        return allowEmpty ? options.withAllowEmpty() : options;
    }

    /**
     * 合并节点 bizConfig 和 legacy config。
     *
     * @param node 节点定义
     * @return 合并后的配置，legacy config 覆盖 bizConfig 同名字段
     */
    private Map<String, Object> mergedConfig(DagParser.CanvasNode node) {
        Map<String, Object> config = new HashMap<>();
        if (node.getBizConfig() != null) {
            config.putAll(node.getBizConfig());
        }
        if (node.getConfig() != null) {
            config.putAll(node.getConfig());
        }
        return config;
    }

    /**
     * 将配置值解析为整数。
     *
     * @param value 原始配置值
     * @return 整数值，无法解析时返回 null
     */
    private Integer integerValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.parseInt(text.trim());
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    /**
     * setMaxNodeCountForTests 处理 engine.rule 场景的业务逻辑。
     * @param maxNodeCount max node count 参数，用于 setMaxNodeCountForTests 流程中的校验、计算或对象转换。
     */
    public void setMaxNodeCountForTests(int maxNodeCount) {
        this.maxNodeCount = maxNodeCount;
    }

    /**
     * setMaxGotoJumpsForTests 处理 engine.rule 场景的业务逻辑。
     * @param maxGotoJumps max goto jumps 参数，用于 setMaxGotoJumpsForTests 流程中的校验、计算或对象转换。
     */
    public void setMaxGotoJumpsForTests(int maxGotoJumps) {
        this.maxGotoJumps = maxGotoJumps;
    }

    /**
     * setMaxLoopIterationsForTests 处理 engine.rule 场景的业务逻辑。
     * @param maxLoopIterations max loop iterations 参数，用于 setMaxLoopIterationsForTests 流程中的校验、计算或对象转换。
     */
    public void setMaxLoopIterationsForTests(int maxLoopIterations) {
        this.maxLoopIterations = maxLoopIterations;
    }
}
