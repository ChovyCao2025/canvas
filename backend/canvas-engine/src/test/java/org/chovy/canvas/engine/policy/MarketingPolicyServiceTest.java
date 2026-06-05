package org.chovy.canvas.engine.policy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
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
    private MarketingPolicyService service;

    @BeforeAll
    static void initMyBatisPlusTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                MarketingSuppressionDO.class);
    }

    @BeforeEach
    void setUp() {
        consentMapper = mock(MarketingConsentMapper.class);
        suppressionMapper = mock(MarketingSuppressionMapper.class);
        service = new MarketingPolicyService(
                mock(CustomerProfileMapper.class),
                mock(CustomerChannelMapper.class),
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

    private MarketingConsentDO consent(String status) {
        MarketingConsentDO consent = new MarketingConsentDO();
        consent.setConsentStatus(status);
        return consent;
    }
}
