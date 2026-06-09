package org.chovy.canvas.engine.rule;

/**
 * RuleNode 定义 engine.rule 场景中的扩展契约。
 */
public sealed interface RuleNode permits RuleGroup, RuleCondition {
}
