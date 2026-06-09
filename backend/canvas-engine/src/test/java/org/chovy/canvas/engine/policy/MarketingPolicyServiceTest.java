package org.chovy.canvas.engine.policy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.chovy.canvas.dal.dataobject.CustomerChannelDO;
import org.chovy.canvas.dal.dataobject.MarketingConsentDO;
import org.chovy.canvas.dal.dataobject.MarketingSuppressionDO;
import org.chovy.canvas.dal.mapper.CustomerChannelMapper;
import org.chovy.canvas.dal.mapper.CustomerProfileMapper;
import org.chovy.canvas.dal.mapper.MarketingConsentMapper;
import org.chovy.canvas.dal.mapper.MarketingSuppressionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingPolicyServiceTest {

    private MarketingConsentMapper consentMapper;
    private MarketingSuppressionMapper suppressionMapper;
    private CustomerChannelMapper channelMapper;
    private MarketingPolicyService service;

    @BeforeAll
    static void initMyBatisPlusTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, MarketingConsentDO.class);
        TableInfoHelper.initTableInfo(assistant, MarketingSuppressionDO.class);
        TableInfoHelper.initTableInfo(assistant, CustomerChannelDO.class);
    }

    @BeforeEach
    void setUp() {
        consentMapper = mock(MarketingConsentMapper.class);
        suppressionMapper = mock(MarketingSuppressionMapper.class);
        channelMapper = mock(CustomerChannelMapper.class);
        service = new MarketingPolicyService(
                mock(CustomerProfileMapper.class),
                channelMapper,
                consentMapper,
                suppressionMapper,
                mock(StringRedisTemplate.class));
    }

    @Test
    void allowsExplicitConsentWhenUserOptedIn() {
        when(consentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(consent(MarketingConsentDO.OPT_IN));

        MarketingPolicyService.PolicyDecision decision = service.consentAllowed("user-1", "sms", true);

        assertThat(decision.allowed()).isTrue();
    }

    @Test
    void blocksOptOutConsent() {
        when(consentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(consent(MarketingConsentDO.OPT_OUT));

        MarketingPolicyService.PolicyDecision decision = service.consentAllowed("user-1", "sms", true);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reasonCode()).isEqualTo("MARKETING_OPT_OUT");
    }

    @Test
    void blocksMissingConsentWhenExplicitConsentIsRequired() {
        when(consentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        MarketingPolicyService.PolicyDecision decision = service.consentAllowed("user-1", "email", true);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reasonCode()).isEqualTo("NO_MARKETING_CONSENT");
    }

    @Test
    void allowsMissingConsentWhenExplicitConsentIsNotRequired() {
        when(consentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        MarketingPolicyService.PolicyDecision decision = service.consentAllowed("user-1", "push", false);

        assertThat(decision.allowed()).isTrue();
    }

    @Test
    void blocksWhenActiveSuppressionExists() {
        when(suppressionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        MarketingPolicyService.PolicyDecision decision = service.suppressionAllowed("user-1", "sms");

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reasonCode()).isEqualTo("MARKETING_SUPPRESSED");
    }

    @Test
    void allowsWhenNoActiveOrUnexpiredSuppressionExists() {
        when(suppressionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        MarketingPolicyService.PolicyDecision decision = service.suppressionAllowed("user-1", "email");

        assertThat(decision.allowed()).isTrue();
    }

    @Test
    void suppressionQueryIncludesExpiredAndAllChannelRules() {
        when(suppressionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        service.suppressionAllowed("user-1", "sms");

        ArgumentCaptor<LambdaQueryWrapper<MarketingSuppressionDO>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(suppressionMapper).selectCount(captor.capture());
        LambdaQueryWrapper<MarketingSuppressionDO> wrapper = captor.getValue();
        String sql = wrapper.getCustomSqlSegment().toUpperCase();
        assertThat(sql).contains("EXPIRES_AT", "IS NULL", ">", "CHANNEL");
        assertThat(wrapper.getParamNameValuePairs().values()).contains("SMS", "ALL");
    }

    @Test
    void tenantAwareConsentQueryIncludesTenantPredicate() {
        when(consentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(consent(MarketingConsentDO.OPT_IN));

        service.consentAllowed(7L, "user-1", "sms", true);

        ArgumentCaptor<LambdaQueryWrapper<MarketingConsentDO>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(consentMapper).selectOne(captor.capture());
        assertThat(captor.getValue().getCustomSqlSegment().toUpperCase()).contains("TENANT_ID");
        assertThat(captor.getValue().getParamNameValuePairs().values()).contains(7L, "user-1", "SMS");
    }

    @Test
    void tenantAwareSuppressionQueryIncludesTenantPredicate() {
        when(suppressionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        service.suppressionAllowed(7L, "user-1", "sms");

        ArgumentCaptor<LambdaQueryWrapper<MarketingSuppressionDO>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(suppressionMapper).selectCount(captor.capture());
        assertThat(captor.getValue().getCustomSqlSegment().toUpperCase()).contains("TENANT_ID");
        assertThat(captor.getValue().getParamNameValuePairs().values()).contains(7L, "user-1", "SMS", "ALL");
    }

    @Test
    void tenantAwareChannelQueryIncludesTenantPredicate() {
        when(channelMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(channel("SMS", "13800000000"));

        MarketingPolicyService.PolicyDecision decision = service.channelAvailable(7L, "user-1", "sms");

        assertThat(decision.allowed()).isTrue();
        ArgumentCaptor<LambdaQueryWrapper<CustomerChannelDO>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(channelMapper).selectOne(captor.capture());
        assertThat(captor.getValue().getCustomSqlSegment().toUpperCase()).contains("TENANT_ID");
        assertThat(captor.getValue().getParamNameValuePairs().values()).contains(7L, "user-1", "SMS", 1);
    }

    private MarketingConsentDO consent(String status) {
        MarketingConsentDO consent = new MarketingConsentDO();
        consent.setConsentStatus(status);
        return consent;
    }

    private CustomerChannelDO channel(String channel, String address) {
        CustomerChannelDO row = new CustomerChannelDO();
        row.setChannel(channel);
        row.setAddress(address);
        row.setEnabled(1);
        return row;
    }
}
