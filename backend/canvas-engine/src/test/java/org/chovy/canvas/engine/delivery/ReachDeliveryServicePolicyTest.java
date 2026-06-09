package org.chovy.canvas.engine.delivery;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.MessageSendRecordDO;
import org.chovy.canvas.dal.mapper.MessageSendRecordMapper;
import org.chovy.canvas.engine.policy.MarketingPolicyService;
import org.chovy.canvas.engine.policy.MarketingPolicyService.PolicyDecision;
import org.chovy.canvas.infrastructure.http.ExternalHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReachDeliveryServicePolicyTest {

    private final AtomicInteger reachCalls = new AtomicInteger();
    private MessageSendRecordMapper recordMapper;
    private MarketingPolicyService policyService;
    private ReachDeliveryService service;

    @BeforeEach
    void setUp() {
        recordMapper = mock(MessageSendRecordMapper.class);
        policyService = mock(MarketingPolicyService.class);
        when(recordMapper.insert(any(MessageSendRecordDO.class))).thenAnswer(invocation -> {
            MessageSendRecordDO record = invocation.getArgument(0);
            record.setId(100L);
            return 1;
        });
        when(recordMapper.updateById(any(MessageSendRecordDO.class))).thenReturn(1);
        service = new ReachDeliveryService(
                recordMapper,
                new ObjectMapper(),
                new RecordingExternalHttpClient(),
                policyService);
    }

    @Test
    void optOutCreatesSkippedRecordAndDoesNotCallReachPlatform() {
        when(policyService.consentAllowed(1L, "user-1", "SMS", true))
                .thenReturn(PolicyDecision.blocked("MARKETING_OPT_OUT", "opted out"));

        ReachDeliveryService.DeliveryResult result = service.send(requestWithPolicy("SMS")).block();

        assertThat(result).isNotNull();
        assertThat(result.sent()).isFalse();
        assertThat(result.errorMessage()).contains("MARKETING_OPT_OUT");
        verify(recordMapper).updateById(argThat((MessageSendRecordDO record) ->
                MessageSendRecordDO.STATUS_SKIPPED.equals(record.getStatus())
                        && record.getErrorMessage().contains("MARKETING_OPT_OUT")));
        assertThat(reachCalls).hasValue(0);
    }

    @Test
    void duplicateSkippedRecordDoesNotConsumeFrequencyAgain() {
        when(recordMapper.selectOne(any())).thenReturn(existingSkippedRecord());

        ReachDeliveryService.DeliveryResult result = service.send(requestWithPolicy("EMAIL")).block();

        assertThat(result).isNotNull();
        assertThat(result.sent()).isFalse();
        assertThat(result.duplicate()).isTrue();
        verify(policyService, never()).consumeFrequency(
                any(), any(), any(), any(), any(), anyInt(), any(Duration.class));
        assertThat(reachCalls).hasValue(0);
    }

    @Test
    void allowedPolicyCallsReachPlatform() {
        allowAllPolicyChecks("PUSH");

        ReachDeliveryService.DeliveryResult result = service.send(requestWithPolicy("PUSH")).block();

        assertThat(result).isNotNull();
        assertThat(result.sent()).isTrue();
        assertThat(result.externalMessageId()).isEqualTo("msg-1");
        assertThat(reachCalls).hasValue(1);
    }

    private void allowAllPolicyChecks(String channel) {
        when(policyService.consentAllowed(1L, "user-1", channel, true)).thenReturn(PolicyDecision.allow());
        when(policyService.suppressionAllowed(1L, "user-1", channel)).thenReturn(PolicyDecision.allow());
        when(policyService.channelAvailable(1L, "user-1", channel)).thenReturn(PolicyDecision.allow());
        when(policyService.quietHoursAllowed("user-1", "22:00", "08:00", "USER_LOCAL"))
                .thenReturn(PolicyDecision.allow());
        when(policyService.consumeFrequency(
                eq("user-1"), eq(20L), eq("node-1"), eq("JOURNEY"), eq(channel), eq(1), any(Duration.class)))
                .thenReturn(PolicyDecision.allow());
    }

    private ReachDeliveryService.DeliveryRequest requestWithPolicy(String channel) {
        return service.request(
                1L,
                "exec-1",
                20L,
                "user-1",
                "node-1",
                channel,
                "tpl-1",
                Map.of("body", "hello"),
                Map.of(),
                "idem-" + channel,
                ReachDeliveryService.PolicyOptions.defaults());
    }

    private MessageSendRecordDO existingSkippedRecord() {
        MessageSendRecordDO record = new MessageSendRecordDO();
        record.setId(99L);
        record.setStatus(MessageSendRecordDO.STATUS_SKIPPED);
        record.setErrorMessage("MARKETING_OPT_OUT: opted out");
        return record;
    }

    private final class RecordingExternalHttpClient implements ExternalHttpClient {
        @Override
        public Mono<Map<String, Object>> postJson(String integrationName, String path, Map<String, Object> payload) {
            reachCalls.incrementAndGet();
            return Mono.just(Map.of("messageId", "msg-1"));
        }
    }
}
