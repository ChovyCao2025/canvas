package org.chovy.canvas.risk.domain.runtime;

import org.chovy.canvas.risk.domain.dsl.RiskRuleConditionNode;
import org.chovy.canvas.risk.domain.dsl.RiskRuleGroupNode;
import org.chovy.canvas.risk.domain.dsl.RiskRuleLogic;
import org.chovy.canvas.risk.domain.dsl.RiskRuleOperand;
import org.chovy.canvas.risk.domain.dsl.RiskRuleOperator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 风控规则执行器，评估已解析的规则树并记录条件级证据。
 */
public class RiskRuleEvaluator {

    /**
     * 评估根规则组，并返回匹配结果、证据列表和缺失特征列表。
     */
    public RiskRuleEvaluationResult evaluate(RiskRuleGroupNode root, RiskFeatureResolver resolver) {
        EvaluationContext context = new EvaluationContext(resolver);
        boolean matched = evaluateGroup(root, "$", context);
        return new RiskRuleEvaluationResult(
                matched,
                List.copyOf(context.evidence),
                List.copyOf(context.missingFeatures));
    }

    /**
     * 按规则组逻辑分派到 AND 或 OR 评估流程。
     */
    private boolean evaluateGroup(RiskRuleGroupNode group, String path, EvaluationContext context) {
        if (group == null) {
            // 缺失分组按空 AND 组处理，避免半配置规则直接阻断决策。
            return true;
        }
        if (group.logic() == RiskRuleLogic.OR) {
            return evaluateOrGroup(group, path, context);
        }
        return evaluateAndGroup(group, path, context);
    }

    /**
     * 评估 AND 规则组，任一条件或子组不匹配即短路返回 false。
     */
    private boolean evaluateAndGroup(RiskRuleGroupNode group, String path, EvaluationContext context) {
        for (int i = 0; i < group.conditions().size(); i++) {
            // 条件失败时立即停止，保留已经评估过的证据。
            if (!evaluateCondition(group.conditions().get(i), path + ".conditions[" + i + "]", context)) {
                return false;
            }
        }
        for (int i = 0; i < group.groups().size(); i++) {
            // 子组失败同样短路，避免无意义地解析后续分支。
            if (!evaluateGroup(group.groups().get(i), path + ".groups[" + i + "]", context)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 评估 OR 规则组，任一条件或子组匹配即短路返回 true。
     */
    private boolean evaluateOrGroup(RiskRuleGroupNode group, String path, EvaluationContext context) {
        boolean hasChildren = false;
        for (int i = 0; i < group.conditions().size(); i++) {
            hasChildren = true;
            // OR 分支命中后立即返回，证据仅包含实际评估过的路径。
            if (evaluateCondition(group.conditions().get(i), path + ".conditions[" + i + "]", context)) {
                return true;
            }
        }
        for (int i = 0; i < group.groups().size(); i++) {
            hasChildren = true;
            if (evaluateGroup(group.groups().get(i), path + ".groups[" + i + "]", context)) {
                return true;
            }
        }
        // 空 OR 组按 true 处理，与解析器允许空分组的默认语义保持一致。
        return !hasChildren;
    }

    /**
     * 评估单条条件，并把左右值、操作符和命中结果写入证据。
     */
    private boolean evaluateCondition(RiskRuleConditionNode condition, String path, EvaluationContext context) {
        RiskResolvedValue left = context.resolve(condition.left());
        RiskResolvedValue right = rightValue(condition, context);
        boolean matched = left.present() && right.present() && matches(condition.op(), left.value(), right.value());
        // 即使条件未命中也记录证据，便于运营排查规则未触发原因。
        context.evidence.add(new RiskRuleEvidence(
                path,
                condition.op().wireValue(),
                left.value(),
                right.value(),
                matched));
        return matched;
    }

    /**
     * 解析条件右侧值；存在性/空值类操作符不需要真实右操作数。
     */
    private RiskResolvedValue rightValue(RiskRuleConditionNode condition, EvaluationContext context) {
        if (condition.op() == RiskRuleOperator.EXISTS
                || condition.op() == RiskRuleOperator.IS_EMPTY
                || condition.op() == RiskRuleOperator.IS_NULL) {
            return RiskResolvedValue.present(null);
        }
        return context.resolve(condition.right());
    }

    /**
     * 根据操作符执行具体比较逻辑。
     */
    private boolean matches(RiskRuleOperator operator, Object left, Object right) {
        return switch (operator) {
            case EQ -> Objects.equals(left, right);
            case NE -> !Objects.equals(left, right);
            case GT -> compareNumbers(left, right) > 0;
            case GTE -> compareNumbers(left, right) >= 0;
            case LT -> compareNumbers(left, right) < 0;
            case LTE -> compareNumbers(left, right) <= 0;
            case LIKE -> left != null && right != null && left.toString().contains(right.toString());
            case STARTS_WITH -> left != null && right != null && left.toString().startsWith(right.toString());
            case ENDS_WITH -> left != null && right != null && left.toString().endsWith(right.toString());
            case CONTAINS -> contains(left, right);
            case IN -> contains(right, left);
            case NOT_IN -> !contains(right, left);
            case INTERSECTS -> intersects(left, right);
            case EXISTS -> left != null;
            case IS_EMPTY -> isEmpty(left);
            case IS_NULL -> left == null;
            // 时间窗口操作符当前只为 DSL 前向兼容保留，尚未接入运行时执行。
            case BEFORE, AFTER, BETWEEN_TIME -> false;
        };
    }

    /**
     * 将左右值转成 BigDecimal 后执行数值比较。
     */
    private int compareNumbers(Object left, Object right) {
        BigDecimal leftNumber = asBigDecimal(left);
        BigDecimal rightNumber = asBigDecimal(right);
        if (leftNumber == null || rightNumber == null) {
            return Integer.MIN_VALUE;
        }
        return leftNumber.compareTo(rightNumber);
    }

    /**
     * 将运行时数值转换为 BigDecimal，无法转换时返回 null。
     */
    private BigDecimal asBigDecimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        return null;
    }

    /**
     * 判断集合、Map 或字符串是否包含目标值。
     */
    private boolean contains(Object container, Object value) {
        if (container instanceof Collection<?> collection) {
            return collection.contains(value);
        }
        if (container instanceof Map<?, ?> map) {
            return map.containsKey(value);
        }
        return container != null && value != null && container.toString().contains(value.toString());
    }

    /**
     * 判断两个集合是否存在交集。
     */
    private boolean intersects(Object left, Object right) {
        if (!(left instanceof Collection<?> leftCollection) || !(right instanceof Collection<?> rightCollection)) {
            return false;
        }
        return leftCollection.stream().anyMatch(rightCollection::contains);
    }

    /**
     * 判断字符串、集合或 Map 是否为空。
     */
    private boolean isEmpty(Object value) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (value == null) {
            return false;
        }
        if (value instanceof CharSequence sequence) {
            return sequence.isEmpty();
        }
        if (value instanceof Collection<?> collection) {
            return collection.isEmpty();
        }
        if (value instanceof Map<?, ?> map) {
            return map.isEmpty();
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return false;
    }

    /**
     * EvaluationContext 承载对应领域的业务规则、流程编排和结果转换。
     */
    private static final class EvaluationContext {
        /**
         * 保存 resolver 对应的风控状态或配置。
         */
        private final RiskFeatureResolver resolver;
        private final List<RiskRuleEvidence> evidence = new ArrayList<>();
        private final List<String> missingFeatures = new ArrayList<>();


        /**
         * 构造评估上下文，未提供解析器时按所有操作数缺失处理。
         */
        private EvaluationContext(RiskFeatureResolver resolver) {
            this.resolver = resolver == null ? operand -> RiskResolvedValue.missing() : resolver;
        }

        /**
         * 解析操作数并记录缺失的特征键。
         */
        private RiskResolvedValue resolve(RiskRuleOperand operand) {
            RiskResolvedValue value = resolver.resolve(operand);
            RiskResolvedValue resolved = value == null ? RiskResolvedValue.missing() : value;
            if (!resolved.present() && operand instanceof RiskRuleOperand.FeatureOperand feature) {
                // 只把特征缺失写入 missingFeatures；主体、事件和上下文字段缺失可从证据路径观察。
                missingFeatures.add(feature.key());
            }
            return resolved;
        }
    }
}
