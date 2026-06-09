package org.chovy.canvas.domain.risk.runtime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 风控决策合并器，将规则命中、名单命中和影子信号归并为最终动作、分数、风险带和追踪证据。
 */
public class RiskDecisionMerger {

    /**
     * 合并一次决策中的所有信号，并按缺失特征、名单优先级和动作优先级得到最终决策。
     */
    public RiskMergedDecision merge(RiskDecisionMergeRequest request) {
        RiskDecisionMergeRequest safeRequest = request == null ? RiskDecisionMergeRequest.enforce(List.of()) : request;
        List<RiskDecisionSignal> shadowSignals = safeRequest.signals().stream()
                .filter(signal -> signal.shadowSignal() || signal.action() == RiskDecisionAction.SHADOW_ONLY)
                .sorted(stableSignalOrder())
                .toList();
        List<RiskDecisionSignal> effectiveSignals = safeRequest.signals().stream()
                .filter(signal -> !signal.shadowSignal() && signal.action() != RiskDecisionAction.SHADOW_ONLY)
                .sorted(stableSignalOrder())
                .toList();

        if (!safeRequest.missingFeatures().isEmpty()) {
            // 运行时输入缺失视为依赖失败，交给失败策略决定放行、复核或阻断。
            return failureDecision(safeRequest);
        }

        List<RiskDecisionSignal> selectedSignals = selectEffectiveSignals(effectiveSignals);
        int score = clamp(selectedSignals.stream().mapToInt(RiskDecisionSignal::scoreDelta).sum());
        RiskDecisionAction action = selectedSignals.stream()
                .map(RiskDecisionSignal::action)
                .max(Comparator.comparingInt(this::priority))
                .orElse(RiskDecisionAction.ALLOW);

        return new RiskMergedDecision(
                action,
                score,
                band(score),
                selectedSignals.stream().map(RiskDecisionSignal::reason).toList(),
                selectedSignals.stream().map(RiskDecisionSignal::label).filter(label -> label != null && !label.isBlank()).toList(),
                selectedSignals,
                shadowSignals);
    }

    /**
     * 根据失败策略生成兜底决策，并把缺失特征转换为可追踪原因。
     */
    private RiskMergedDecision failureDecision(RiskDecisionMergeRequest request) {
        RiskDecisionAction action = switch (request.failPolicy()) {
            case FAIL_OPEN -> RiskDecisionAction.ALLOW;
            case FAIL_REVIEW -> RiskDecisionAction.REVIEW;
            case FAIL_CLOSED -> RiskDecisionAction.BLOCK;
        };
        List<String> reasons = request.missingFeatures().stream()
                .map(feature -> feature.startsWith("RUNTIME_FAILURE:")
                        ? feature
                        : "MISSING_FEATURE:" + feature)
                .toList();
        return new RiskMergedDecision(action, 0, RiskBand.LOW, reasons, List.of(), List.of(), List.of());
    }

    /**
     * 从有效信号中挑选真正参与决策的集合，处理合规黑名单和白名单的覆盖语义。
     */
    private List<RiskDecisionSignal> selectEffectiveSignals(List<RiskDecisionSignal> signals) {
        List<RiskDecisionSignal> complianceBlocks = signals.stream()
                .filter(signal -> signal.listType() == RiskListType.COMPLIANCE_BLACK)
                .toList();
        if (!complianceBlocks.isEmpty()) {
            // 合规黑名单优先级最高，会覆盖包括白名单在内的所有其他信号。
            return complianceBlocks;
        }
        List<RiskDecisionSignal> whiteListSignals = signals.stream()
                .filter(signal -> signal.listType() == RiskListType.WHITE)
                .toList();
        if (!whiteListSignals.isEmpty()) {
            // 白名单压制普通规则和名单信号，但不会覆盖上面的合规阻断。
            return whiteListSignals;
        }
        return new ArrayList<>(signals);
    }

    /**
     * 返回稳定排序规则，保证原因、标签和审计明细在重放时顺序一致。
     */
    private Comparator<RiskDecisionSignal> stableSignalOrder() {
        // 排序字段只使用稳定业务属性，避免集合迭代顺序影响账本重放和差异比对。
        return Comparator.comparingInt(RiskDecisionSignal::order)
                .thenComparing(RiskDecisionSignal::source, Comparator.nullsLast(String::compareTo))
                .thenComparing(RiskDecisionSignal::reason, Comparator.nullsLast(String::compareTo));
    }

    /**
     * 将风控动作映射为合并时的优先级，数值越高越严格。
     */
    private int priority(RiskDecisionAction action) {
        return switch (action) {
            case BLOCK -> 60;
            case VERIFY -> 50;
            case REVIEW -> 40;
            case LIMIT -> 30;
            case DELAY -> 20;
            case ALLOW -> 10;
            case SHADOW_ONLY -> 0;
        };
    }

    /**
     * 将累计风险分限制在 0 到 100 的对外分值区间。
     */
    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }

    /**
     * 根据最终分值划分低、中、高风险带。
     */
    private RiskBand band(int score) {
        if (score >= 85) {
            return RiskBand.HIGH;
        }
        if (score >= 50) {
            return RiskBand.MEDIUM;
        }
        return RiskBand.LOW;
    }
}
