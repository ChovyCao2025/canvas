package org.chovy.canvas.domain.policy;

import org.chovy.canvas.dal.dataobject.CustomerChannelDO;
import org.chovy.canvas.dal.dataobject.MarketingConsentDO;
import org.chovy.canvas.dal.dataobject.MarketingSuppressionDO;
import org.chovy.canvas.dal.mapper.CustomerChannelMapper;
import org.chovy.canvas.dal.mapper.MarketingConsentMapper;
import org.chovy.canvas.dal.mapper.MarketingSuppressionMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingPreferenceCenterServiceTest {

    @Test
    void buildsCombinedReportAndSummary() {
        MarketingConsentMapper consentMapper = mock(MarketingConsentMapper.class);
        CustomerChannelMapper channelMapper = mock(CustomerChannelMapper.class);
        MarketingSuppressionMapper suppressionMapper = mock(MarketingSuppressionMapper.class);
        when(consentMapper.selectList(any())).thenReturn(List.of(
                consent("EMAIL", MarketingConsentDO.OPT_IN),
                consent("SMS", MarketingConsentDO.OPT_OUT)));
        when(channelMapper.selectList(any())).thenReturn(List.of(
                channel("EMAIL", "u@example.com", 1, 1),
                channel("SMS", "", 1, 0),
                channel("PUSH", "device-1", 0, 1)));
        when(suppressionMapper.selectList(any())).thenReturn(List.of(
                suppression(11L, "SMS", 1, null),
                suppression(12L, "EMAIL", 1, LocalDateTime.now().minusDays(1))));

        MarketingPreferenceCenterService.PreferenceReport report =
                service(consentMapper, channelMapper, suppressionMapper).report(7L, "user-1");

        assertThat(report.userId()).isEqualTo("user-1");
        assertThat(report.summary().totalChannels()).isEqualTo(3);
        assertThat(report.summary().optInCount()).isEqualTo(1);
        assertThat(report.summary().optOutCount()).isEqualTo(1);
        assertThat(report.summary().reachableChannelCount()).isEqualTo(1);
        assertThat(report.summary().activeSuppressionCount()).isEqualTo(1);
        assertThat(report.suppressions()).extracting(MarketingPreferenceCenterService.SuppressionRow::state)
                .containsExactly("ACTIVE", "EXPIRED");
    }

    @Test
    void upsertsConsentAndChannelAndCreatesSuppressionWithNormalizedChannels() {
        MarketingConsentMapper consentMapper = mock(MarketingConsentMapper.class);
        CustomerChannelMapper channelMapper = mock(CustomerChannelMapper.class);
        MarketingSuppressionMapper suppressionMapper = mock(MarketingSuppressionMapper.class);
        CustomerChannelDO existingChannel = channel("SMS", "old", 1, 0);
        existingChannel.setId(9L);
        when(consentMapper.selectOne(any())).thenReturn(null);
        when(channelMapper.selectOne(any())).thenReturn(existingChannel);

        MarketingPreferenceCenterService preferenceService =
                service(consentMapper, channelMapper, suppressionMapper);
        preferenceService.upsertConsent(7L, "user-1",
                new MarketingPreferenceCenterService.ConsentUpdateCommand("email", "opt_in", "operator"));
        preferenceService.upsertChannel(7L, "user-1",
                new MarketingPreferenceCenterService.ChannelUpdateCommand("sms", "13800000000", true, true, "{\"type\":\"mobile\"}"));
        preferenceService.addSuppression(7L, "user-1",
                new MarketingPreferenceCenterService.SuppressionCreateCommand("all", "complaint", true, null));

        ArgumentCaptor<MarketingConsentDO> consentCaptor = ArgumentCaptor.forClass(MarketingConsentDO.class);
        ArgumentCaptor<CustomerChannelDO> channelCaptor = ArgumentCaptor.forClass(CustomerChannelDO.class);
        ArgumentCaptor<MarketingSuppressionDO> suppressionCaptor = ArgumentCaptor.forClass(MarketingSuppressionDO.class);
        verify(consentMapper).insert(consentCaptor.capture());
        verify(channelMapper).updateById(channelCaptor.capture());
        verify(suppressionMapper).insert(suppressionCaptor.capture());

        assertThat(consentCaptor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(consentCaptor.getValue().getChannel()).isEqualTo("EMAIL");
        assertThat(consentCaptor.getValue().getConsentStatus()).isEqualTo(MarketingConsentDO.OPT_IN);
        assertThat(channelCaptor.getValue().getChannel()).isEqualTo("SMS");
        assertThat(channelCaptor.getValue().getAddress()).isEqualTo("13800000000");
        assertThat(channelCaptor.getValue().getVerified()).isEqualTo(1);
        assertThat(suppressionCaptor.getValue().getChannel()).isEqualTo("ALL");
        assertThat(suppressionCaptor.getValue().getReason()).isEqualTo("complaint");
    }

    @Test
    void deactivatesSuppressionInsideTenantScope() {
        MarketingSuppressionMapper suppressionMapper = mock(MarketingSuppressionMapper.class);
        MarketingSuppressionDO row = suppression(99L, "EMAIL", 1, null);
        when(suppressionMapper.selectOne(any())).thenReturn(row);

        service(mock(MarketingConsentMapper.class), mock(CustomerChannelMapper.class), suppressionMapper)
                .deactivateSuppression(7L, 99L);

        ArgumentCaptor<MarketingSuppressionDO> captor = ArgumentCaptor.forClass(MarketingSuppressionDO.class);
        verify(suppressionMapper).updateById(captor.capture());
        assertThat(captor.getValue().getActive()).isZero();
    }

    private MarketingPreferenceCenterService service(MarketingConsentMapper consentMapper,
                                                     CustomerChannelMapper channelMapper,
                                                     MarketingSuppressionMapper suppressionMapper) {
        return new MarketingPreferenceCenterService(consentMapper, channelMapper, suppressionMapper);
    }

    private MarketingConsentDO consent(String channel, String status) {
        MarketingConsentDO row = new MarketingConsentDO();
        row.setTenantId(7L);
        row.setUserId("user-1");
        row.setChannel(channel);
        row.setConsentStatus(status);
        row.setSource("operator");
        return row;
    }

    private CustomerChannelDO channel(String channel, String address, int enabled, int verified) {
        CustomerChannelDO row = new CustomerChannelDO();
        row.setTenantId(7L);
        row.setUserId("user-1");
        row.setChannel(channel);
        row.setAddress(address);
        row.setEnabled(enabled);
        row.setVerified(verified);
        return row;
    }

    private MarketingSuppressionDO suppression(Long id, String channel, int active, LocalDateTime expiresAt) {
        MarketingSuppressionDO row = new MarketingSuppressionDO();
        row.setId(id);
        row.setTenantId(7L);
        row.setUserId("user-1");
        row.setChannel(channel);
        row.setReason("operator");
        row.setActive(active);
        row.setExpiresAt(expiresAt);
        row.setCreatedAt(LocalDateTime.of(2026, 6, 5, 9, 0));
        return row;
    }
}
