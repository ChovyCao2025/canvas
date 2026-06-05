package org.chovy.canvas.engine.channel;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChannelFallbackServiceTest {

    @Test
    void fallbackSelectsOneLevelReplacementAndRecordsDecision() {
        ChannelFallbackService.PolicyRepository policies = mock(ChannelFallbackService.PolicyRepository.class);
        when(policies.find(0L, "PUSH", "JPUSH")).thenReturn(
                new ChannelFallbackService.FallbackPolicy("SMS", "ALIYUN", true, "PRIMARY_THROTTLED"));
        ChannelFallbackService.DecisionRepository decisions = mock(ChannelFallbackService.DecisionRepository.class);
        ChannelFallbackService service = new ChannelFallbackService(policies, decisions);

        ChannelFallbackService.FallbackDecision decision = service.resolve(0L, "exec-1", "node-1", "PUSH", "JPUSH");

        assertThat(decision.finalChannel()).isEqualTo("SMS");
        assertThat(decision.finalProvider()).isEqualTo("ALIYUN");
        assertThat(decision.reason()).isEqualTo("PRIMARY_THROTTLED");
        verify(decisions).insert(decision);
    }

    @Test
    void fallbackCycleIsRejected() {
        ChannelFallbackService.PolicyRepository policies = mock(ChannelFallbackService.PolicyRepository.class);
        when(policies.find(0L, "PUSH", "JPUSH")).thenReturn(
                new ChannelFallbackService.FallbackPolicy("SMS", "ALIYUN", true, "x"));
        when(policies.find(0L, "SMS", "ALIYUN")).thenReturn(
                new ChannelFallbackService.FallbackPolicy("PUSH", "JPUSH", true, "x"));

        assertThatThrownBy(() -> new ChannelFallbackService(policies, mock(ChannelFallbackService.DecisionRepository.class))
                .validateNoCycle(0L, "PUSH", "JPUSH"))
                .hasMessageContaining("PUSH:JPUSH -> SMS:ALIYUN -> PUSH:JPUSH");
    }

    @Test
    void proposedFallbackPolicyIsValidatedAgainstStoredPolicies() {
        ChannelFallbackService.PolicyRepository policies = mock(ChannelFallbackService.PolicyRepository.class);
        when(policies.find(0L, "SMS", "ALIYUN")).thenReturn(
                new ChannelFallbackService.FallbackPolicy("PUSH", "JPUSH", true, "x"));

        assertThatThrownBy(() -> new ChannelFallbackService(policies, mock(ChannelFallbackService.DecisionRepository.class))
                .validateCandidate(0L, "PUSH", "JPUSH", "SMS", "ALIYUN"))
                .hasMessageContaining("PUSH:JPUSH -> SMS:ALIYUN -> PUSH:JPUSH");
    }
}
