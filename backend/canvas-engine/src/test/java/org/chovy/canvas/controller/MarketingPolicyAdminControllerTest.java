package org.chovy.canvas.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.chovy.canvas.dal.dataobject.CustomerChannelDO;
import org.chovy.canvas.dal.dataobject.MarketingConsentDO;
import org.chovy.canvas.dal.dataobject.MarketingSuppressionDO;
import org.chovy.canvas.dal.mapper.CustomerChannelMapper;
import org.chovy.canvas.dal.mapper.MarketingConsentMapper;
import org.chovy.canvas.dal.mapper.MarketingSuppressionMapper;
import org.chovy.canvas.web.MarketingPolicyAdminController;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MarketingPolicyAdminControllerTest {

    @BeforeAll
    static void initMyBatisPlusTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, MarketingConsentDO.class);
        TableInfoHelper.initTableInfo(assistant, MarketingSuppressionDO.class);
        TableInfoHelper.initTableInfo(assistant, CustomerChannelDO.class);
    }

    @Test
    void upsertConsentNormalizesChannelAndStatus() {
        MarketingConsentMapper consentMapper = mock(MarketingConsentMapper.class);
        MarketingPolicyAdminController controller = controller(consentMapper);
        MarketingPolicyAdminController.ConsentReq req =
                new MarketingPolicyAdminController.ConsentReq("user-1", "sms", "opt_out", "manual");

        controller.upsertConsent(req).block();

        ArgumentCaptor<MarketingConsentDO> captor = ArgumentCaptor.forClass(MarketingConsentDO.class);
        verify(consentMapper).insert(captor.capture());
        assertThat(captor.getValue().getChannel()).isEqualTo("SMS");
        assertThat(captor.getValue().getConsentStatus()).isEqualTo("OPT_OUT");
    }

    @Test
    void listPolicyStateReadsConsentSuppressionAndChannel() {
        MarketingConsentMapper consentMapper = mock(MarketingConsentMapper.class);
        MarketingSuppressionMapper suppressionMapper = mock(MarketingSuppressionMapper.class);
        CustomerChannelMapper channelMapper = mock(CustomerChannelMapper.class);
        MarketingPolicyAdminController controller =
                new MarketingPolicyAdminController(consentMapper, suppressionMapper, channelMapper);

        MarketingPolicyAdminController.PolicyState state = controller.policyState("user-1", "email").block().getData();

        assertThat(state.userId()).isEqualTo("user-1");
        assertThat(state.channel()).isEqualTo("EMAIL");
        verify(consentMapper).selectOne(any());
        verify(suppressionMapper).selectList(any());
        verify(channelMapper).selectOne(any());
    }

    private MarketingPolicyAdminController controller(MarketingConsentMapper consentMapper) {
        return new MarketingPolicyAdminController(
                consentMapper,
                mock(MarketingSuppressionMapper.class),
                mock(CustomerChannelMapper.class));
    }
}
