package org.chovy.canvas.risk.domain.runtime;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * 风控名单匹配器，使用主体哈希匹配有效名单条目，并转换为决策信号。
 */
public class RiskListMatcher {

    private final RiskListEntryRepository repository;
    private final RiskSubjectHasher hasher;
    private final Clock clock;

    /**
     * 创建名单匹配器，时钟为空时使用 UTC 系统时钟。
     */
    public RiskListMatcher(RiskListEntryRepository repository, RiskSubjectHasher hasher, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.hasher = Objects.requireNonNull(hasher, "hasher must not be null");
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    /**
     * 对指定原始主体执行名单匹配，返回命中结果或未命中结果。
     */
    public RiskListMatchResult match(Long tenantId, String listKey, String rawSubject) {
        String subjectHash = hasher.hash(rawSubject);
        // 仓储查询只使用哈希值，匹配器不会持久化原始主体。
        return repository.findActiveEntry(tenantId, listKey, subjectHash)
                .filter(this::effectiveNow)
                .map(entry -> RiskListMatchResult.matched(
                        subjectHash,
                        entry.subjectMasked(),
                        signalFor(entry)))
                .orElseGet(() -> RiskListMatchResult.none(subjectHash));
    }

    /**
     * 判断名单条目当前是否处于生效窗口内。
     */
    private boolean effectiveNow(RiskListEntry entry) {
        Instant now = clock.instant();
        boolean started = entry.effectiveFrom() == null || !entry.effectiveFrom().isAfter(now);
        boolean notExpired = entry.expiresAt() == null || entry.expiresAt().isAfter(now);
        return started && notExpired;
    }

    /**
     * 将名单条目转换为统一的风控决策信号。
     */
    private RiskDecisionSignal signalFor(RiskListEntry entry) {
        // 名单类型先直接映射为动作和分值，再由合并器处理跨信号优先级。
        RiskDecisionAction action = switch (entry.listType()) {
            case COMPLIANCE_BLACK, BLACK -> RiskDecisionAction.BLOCK;
            case WHITE -> RiskDecisionAction.ALLOW;
            case GRAY -> RiskDecisionAction.REVIEW;
            case OBSERVE -> RiskDecisionAction.SHADOW_ONLY;
        };
        int score = switch (entry.listType()) {
            case COMPLIANCE_BLACK -> 100;
            case BLACK -> 90;
            case GRAY -> 50;
            case WHITE, OBSERVE -> 0;
        };
        RiskDecisionSignal signal = RiskDecisionSignal
                .effective("list:" + entry.listKey(), entry.reason(), action, score)
                .fromList(entry.listType());
        if (entry.listType() == RiskListType.OBSERVE) {
            return signal.shadowOnly();
        }
        return signal;
    }
}
