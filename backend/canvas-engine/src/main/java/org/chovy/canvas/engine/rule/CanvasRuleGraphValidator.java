package org.chovy.canvas.engine.rule;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    public CanvasRuleGraphValidator(RuleParser ruleParser, RuleValidator ruleValidator) {
        this.ruleParser = ruleParser;
        this.ruleValidator = ruleValidator;
    }

    public void validateOrThrow(DagGraph graph) {
        List<String> errors = new ArrayList<>();
        for (Map.Entry<String, DagParser.CanvasNode> entry : graph.getNodeMap().entrySet()) {
            String nodeId = entry.getKey();
            DagParser.CanvasNode node = entry.getValue();
            Map<String, Object> config = mergedConfig(node);
            validateNodeRules(nodeId, node, config, errors);
            validateTopology(nodeId, node, graph, errors);
            validateScheduledTriggerTopology(nodeId, node, graph, errors);
        }
        if (!errors.isEmpty()) {
            throw new RuleValidationException("发布校验失败：\n" + String.join("\n", errors));
        }
    }

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
        } catch (RuleValidationException e) {
            errors.add(nodeId + ": " + e.getMessage());
        }
    }

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
        errors.add(nodeId + ": 多分支汇入副作用/普通节点必须先经过汇聚节点");
    }

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
            boolean audienceTagger = downstreamNode != null
                    && NodeType.TAGGER.equals(downstreamNode.getType())
                    && "audience".equals(String.valueOf(downstreamConfig.getOrDefault("mode", "")));
            if (!audienceTagger) {
                errors.add(nodeId + ": 定时触发节点只负责定时，下游必须先连接人群筛选 TAGGER 节点");
            }
        }
    }

    private RuleValidationOptions options(String nodeId, String field, boolean allowEmpty) {
        RuleValidationOptions options = RuleValidationOptions.strict(nodeId + "." + field);
        return allowEmpty ? options.withAllowEmpty() : options;
    }

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
}
