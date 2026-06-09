package org.chovy.canvas.domain.risk.runtime;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RiskDecisionMergerTest {

    private final RiskDecisionMerger merger = new RiskDecisionMerger();

    @Test
    void actionPriorityUsesConfiguredOrder() {
        RiskMergedDecision decision = merger.merge(RiskDecisionMergeRequest.enforce(List.of(
                signal("delay", RiskDecisionAction.DELAY, 0),
                signal("limit", RiskDecisionAction.LIMIT, 0),
                signal("review", RiskDecisionAction.REVIEW, 0),
                signal("verify", RiskDecisionAction.VERIFY, 0),
                signal("allow", RiskDecisionAction.ALLOW, 0)
        )));

        assertThat(decision.action()).isEqualTo(RiskDecisionAction.VERIFY);
    }

    @Test
    void scoreIsClampedToZeroAndOneHundred() {
        assertThat(merger.merge(RiskDecisionMergeRequest.enforce(List.of(
                signal("negative", RiskDecisionAction.ALLOW, -20)))).score()).isZero();
        assertThat(merger.merge(RiskDecisionMergeRequest.enforce(List.of(
                signal("large", RiskDecisionAction.REVIEW, 130)))).score()).isEqualTo(100);
    }

    @Test
    void scoreMapsToConfiguredRiskBand() {
        assertThat(merger.merge(RiskDecisionMergeRequest.enforce(List.of(
                signal("low", RiskDecisionAction.ALLOW, 49)))).riskBand()).isEqualTo(RiskBand.LOW);
        assertThat(merger.merge(RiskDecisionMergeRequest.enforce(List.of(
                signal("medium", RiskDecisionAction.REVIEW, 50)))).riskBand()).isEqualTo(RiskBand.MEDIUM);
        assertThat(merger.merge(RiskDecisionMergeRequest.enforce(List.of(
                signal("high", RiskDecisionAction.BLOCK, 85)))).riskBand()).isEqualTo(RiskBand.HIGH);
    }

    @Test
    void missingFeatureUsesFailOpen() {
        RiskMergedDecision decision = merger.merge(RiskDecisionMergeRequest.enforce(List.of())
                .withMissingFeatures(List.of("risk.score"))
                .withFailPolicy(RiskFailPolicy.FAIL_OPEN));

        assertThat(decision.action()).isEqualTo(RiskDecisionAction.ALLOW);
        assertThat(decision.reasons()).contains("MISSING_FEATURE:risk.score");
    }

    @Test
    void missingFeatureUsesFailReview() {
        RiskMergedDecision decision = merger.merge(RiskDecisionMergeRequest.enforce(List.of())
                .withMissingFeatures(List.of("risk.score"))
                .withFailPolicy(RiskFailPolicy.FAIL_REVIEW));

        assertThat(decision.action()).isEqualTo(RiskDecisionAction.REVIEW);
    }

    @Test
    void missingFeatureUsesFailClosed() {
        RiskMergedDecision decision = merger.merge(RiskDecisionMergeRequest.enforce(List.of())
                .withMissingFeatures(List.of("risk.score"))
                .withFailPolicy(RiskFailPolicy.FAIL_CLOSED));

        assertThat(decision.action()).isEqualTo(RiskDecisionAction.BLOCK);
    }

    @Test
    void shadowOnlySignalsDoNotAffectFinalDecision() {
        RiskMergedDecision decision = merger.merge(RiskDecisionMergeRequest.enforce(List.of(
                signal("allow", RiskDecisionAction.ALLOW, 10),
                signal("shadow", RiskDecisionAction.BLOCK, 100).shadowOnly()
        )));

        assertThat(decision.action()).isEqualTo(RiskDecisionAction.ALLOW);
        assertThat(decision.shadowSignals()).hasSize(1);
        assertThat(decision.score()).isEqualTo(10);
    }

    @Test
    void whiteListSuppressesOrdinaryRejectSignals() {
        RiskMergedDecision decision = merger.merge(RiskDecisionMergeRequest.enforce(List.of(
                signal("black", RiskDecisionAction.BLOCK, 95).fromList(RiskListType.BLACK),
                signal("white", RiskDecisionAction.ALLOW, 0).fromList(RiskListType.WHITE)
        )));

        assertThat(decision.action()).isEqualTo(RiskDecisionAction.ALLOW);
        assertThat(decision.reasons()).containsExactly("white");
    }

    @Test
    void complianceBlockOverridesWhiteList() {
        RiskMergedDecision decision = merger.merge(RiskDecisionMergeRequest.enforce(List.of(
                signal("white", RiskDecisionAction.ALLOW, 0).fromList(RiskListType.WHITE),
                signal("compliance", RiskDecisionAction.BLOCK, 100).fromList(RiskListType.COMPLIANCE_BLACK)
        )));

        assertThat(decision.action()).isEqualTo(RiskDecisionAction.BLOCK);
        assertThat(decision.reasons()).containsExactly("compliance");
    }

    @Test
    void reasonsAndLabelsAreStableBySourceGroupRuleOrder() {
        RiskMergedDecision decision = merger.merge(RiskDecisionMergeRequest.enforce(List.of(
                signal("r2", RiskDecisionAction.REVIEW, 20).withOrder(2).withLabel("L2"),
                signal("r1", RiskDecisionAction.REVIEW, 10).withOrder(1).withLabel("L1")
        )));

        assertThat(decision.reasons()).containsExactly("r1", "r2");
        assertThat(decision.labels()).containsExactly("L1", "L2");
    }

    private RiskDecisionSignal signal(String reason, RiskDecisionAction action, int scoreDelta) {
        return RiskDecisionSignal.effective("rule", reason, action, scoreDelta);
    }
}
