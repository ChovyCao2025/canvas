package org.chovy.canvas.risk.domain.dsl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 风控规则校验器，检查 DSL 结构、特征可用性、名单主体类型和运行时安全边界。
 */
public class RiskRuleValidator {

    /**
     * 保存 MAX_DEPTH 对应的风控状态或配置。
     */
    private static final int MAX_DEPTH = 5;

    /**
     * 保存 MAX_CONDITIONS 对应的风控状态或配置。
     */
    private static final int MAX_CONDITIONS = 100;


    /**
     * 保存 factorCatalog 对应的风控状态或配置。
     */
    private final RiskFactorCatalog factorCatalog;

    /**
     * 保存 listCatalog 对应的风控状态或配置。
     */
    private final RiskListCatalog listCatalog;


    /**
     * 构造规则校验器，缺失目录时使用空目录以便返回明确的未知特征/名单错误。
     */
    public RiskRuleValidator(RiskFactorCatalog factorCatalog, RiskListCatalog listCatalog) {
        this.factorCatalog = factorCatalog == null ? key -> Optional.empty() : factorCatalog;
        this.listCatalog = listCatalog == null ? key -> Optional.empty() : listCatalog;
    }

    /**
     * 校验完整规则树，并返回是否通过及所有可定位错误。
     */
    public RiskRuleValidationResult validate(RiskRuleGroupNode root, RiskRuntimeMode mode) {
        List<RiskValidationError> errors = new ArrayList<>();
        // 未显式指定运行模式时按 ENFORCE 校验，采用最严格的在线执行约束。
        validateGroup(root, mode == null ? RiskRuntimeMode.ENFORCE : mode, "$", 1, errors, new Counter());
        return new RiskRuleValidationResult(errors.isEmpty(), errors);
    }

    /**
     * 递归校验规则组深度、条件数量和子组结构。
     */
    private void validateGroup(RiskRuleGroupNode group,
                               RiskRuntimeMode mode,
                               String path,
                               int depth,
                               List<RiskValidationError> errors,
                               Counter counter) {
        if (group == null) {
            return;
        }
        if (depth > MAX_DEPTH) {
            errors.add(new RiskValidationError(path, RiskValidationErrorCode.MAX_DEPTH_EXCEEDED,
                    "risk rule nesting depth exceeds " + MAX_DEPTH));
        }
        List<RiskRuleConditionNode> conditions = group.conditions();
        for (int i = 0; i < conditions.size(); i++) {
            // 条件数量限制作用于整棵规则树，而不是单个分组，防止最坏情况评估成本失控。
            counter.conditions++;
            if (counter.conditions > MAX_CONDITIONS) {
                errors.add(new RiskValidationError(path + ".conditions[" + i + "]",
                        RiskValidationErrorCode.MAX_CONDITIONS_EXCEEDED,
                        "risk rule condition count exceeds " + MAX_CONDITIONS));
                break;
            }
            validateCondition(conditions.get(i), mode, path + ".conditions[" + i + "]", errors);
        }
        List<RiskRuleGroupNode> groups = group.groups();
        for (int i = 0; i < groups.size(); i++) {
            validateGroup(groups.get(i), mode, path + ".groups[" + i + "]", depth + 1, errors, counter);
        }
    }

    /**
     * 校验单条条件中两侧操作数、名单引用和类型兼容性。
     */
    private void validateCondition(RiskRuleConditionNode condition,
                                   RiskRuntimeMode mode,
                                   String path,
                                   List<RiskValidationError> errors) {
        Optional<RiskFactorDefinition> leftFactor = validateFeature(condition.left(), mode, path + ".left", errors);
        Optional<RiskFactorDefinition> rightFactor = validateFeature(condition.right(), mode, path + ".right", errors);
        validateList(condition.left(), path + ".left", condition.right(), path + ".right", errors);
        validateList(condition.right(), path + ".right", condition.left(), path + ".left", errors);
        validateTypeCompatibility(condition, path, errors, leftFactor.or(() -> rightFactor));
    }

    /**
     * 校验特征操作数是否存在，并检查其是否允许在当前运行模式使用。
     */
    private Optional<RiskFactorDefinition> validateFeature(RiskRuleOperand operand,
                                                           RiskRuntimeMode mode,
                                                           String path,
                                                           List<RiskValidationError> errors) {
        if (!(operand instanceof RiskRuleOperand.FeatureOperand feature)) {
            return Optional.empty();
        }
        Optional<RiskFactorDefinition> definition = factorCatalog.findByKey(feature.key());
        if (definition.isEmpty()) {
            errors.add(new RiskValidationError(path + ".key", RiskValidationErrorCode.UNKNOWN_FEATURE,
                    "unknown risk feature: " + feature.key()));
            return Optional.empty();
        }
        // ENFORCE 模式只能引用可在线解析的特征，避免决策链路等待离线数据。
        if (mode == RiskRuntimeMode.ENFORCE
                && definition.get().availability() == RiskFeatureAvailability.OFFLINE_ONLY) {
            errors.add(new RiskValidationError(path + ".key", RiskValidationErrorCode.FEATURE_OFFLINE_ONLY,
                    "offline-only feature cannot be used in enforce mode: " + feature.key()));
        }
        return definition;
    }

    /**
     * 校验名单操作数是否存在，并确认被比较主体与名单主体类型一致。
     */
    private void validateList(RiskRuleOperand candidate,
                              String candidatePath,
                              RiskRuleOperand counterpart,
                              String counterpartPath,
                              List<RiskValidationError> errors) {
        if (!(candidate instanceof RiskRuleOperand.ListOperand list)) {
            return;
        }
        Optional<RiskListDefinition> definition = listCatalog.findByKey(list.key());
        if (definition.isEmpty()) {
            errors.add(new RiskValidationError(candidatePath + ".key", RiskValidationErrorCode.UNKNOWN_LIST,
                    "unknown risk list: " + list.key()));
            return;
        }
        RiskSubjectType counterpartSubject = subjectType(counterpart);
        if (counterpartSubject != null && counterpartSubject != definition.get().subjectType()) {
            // 名单匹配只有在比较双方使用同一主体哈希域时才安全。
            errors.add(new RiskValidationError(candidatePath + ".key",
                    RiskValidationErrorCode.LIST_SUBJECT_TYPE_MISMATCH,
                    "risk list subject type does not match " + counterpartPath));
        }
    }

    /**
     * 根据 SUBJECT 操作数路径推断主体标识类型。
     */
    private RiskSubjectType subjectType(RiskRuleOperand operand) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (operand instanceof RiskRuleOperand.SubjectOperand subject) {
            String path = subject.path() == null ? "" : subject.path();
            if ("userId".equalsIgnoreCase(path) || "user_id".equalsIgnoreCase(path)) {
                return RiskSubjectType.USER_ID;
            }
            if ("deviceId".equalsIgnoreCase(path) || "device_id".equalsIgnoreCase(path)) {
                return RiskSubjectType.DEVICE_ID;
            }
            if ("ip".equalsIgnoreCase(path)) {
                return RiskSubjectType.IP;
            }
            if ("email".equalsIgnoreCase(path)) {
                return RiskSubjectType.EMAIL;
            }
            if ("phone".equalsIgnoreCase(path)) {
                return RiskSubjectType.PHONE;
            }
            if ("card".equalsIgnoreCase(path)) {
                return RiskSubjectType.CARD;
            }
            return RiskSubjectType.GENERIC;
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return null;
    }

    /**
     * 校验特征类型和右侧字面量类型是否能被当前操作符安全比较。
     */
    private void validateTypeCompatibility(RiskRuleConditionNode condition,
                                           String path,
                                           List<RiskValidationError> errors,
                                           Optional<RiskFactorDefinition> factorDefinition) {
        if (factorDefinition.isEmpty() || !(condition.right() instanceof RiskRuleOperand.LiteralOperand literal)) {
            return;
        }
        // 目录类型检查可在发布前发现配置错误，避免运行时隐式转换导致规则静默不命中。
        if (isNumericOperator(condition.op())
                /**
                 * 判断业务条件是否成立。
                 *
                 * @return 返回布尔判断结果。
                 */
                && isNumericType(factorDefinition.get().valueType())
                && !isNumber(literal.value())) {
            errors.add(new RiskValidationError(path + ".right.value", RiskValidationErrorCode.TYPE_MISMATCH,
                    "numeric comparison requires numeric literal"));
        }
    }

    /**
     * 判断操作符是否要求数值比较。
     */
    private boolean isNumericOperator(RiskRuleOperator operator) {
        return operator == RiskRuleOperator.GT
                || operator == RiskRuleOperator.GTE
                || operator == RiskRuleOperator.LT
                || operator == RiskRuleOperator.LTE;
    }

    /**
     * 判断目录声明的特征值类型是否为数值类型。
     */
    private boolean isNumericType(RiskValueType type) {
        return type == RiskValueType.INTEGER || type == RiskValueType.DECIMAL;
    }

    /**
     * 判断字面量值是否为可用于数值比较的 Java 类型。
     */
    private boolean isNumber(Object value) {
        return value instanceof Number || value instanceof BigDecimal;
    }

    /**
     * Counter 承载对应领域的业务规则、流程编排和结果转换。
     */
    private static final class Counter {
        /**
         * 保存 conditions 对应的风控状态或配置。
         */
        private int conditions;
    }
}
