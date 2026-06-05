package org.chovy.canvas.domain.marketing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CdpUserProfileDO;
import org.chovy.canvas.dal.dataobject.CustomerChannelDO;
import org.chovy.canvas.dal.dataobject.MarketingConsentDO;
import org.chovy.canvas.dal.dataobject.MarketingFormDefinitionDO;
import org.chovy.canvas.dal.dataobject.MarketingFormSubmissionDO;
import org.chovy.canvas.dal.mapper.CdpUserProfileMapper;
import org.chovy.canvas.dal.mapper.CustomerChannelMapper;
import org.chovy.canvas.dal.mapper.MarketingConsentMapper;
import org.chovy.canvas.dal.mapper.MarketingFormDefinitionMapper;
import org.chovy.canvas.dal.mapper.MarketingFormSubmissionMapper;
import org.chovy.canvas.domain.cdp.CdpUserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingFormServiceTest {

    @Test
    void createsActiveFormWithNormalizedJsonAndPublicKey() {
        MarketingFormDefinitionMapper definitionMapper = mock(MarketingFormDefinitionMapper.class);
        doAnswer(invocation -> {
            MarketingFormDefinitionDO row = invocation.getArgument(0);
            row.setId(12L);
            return 1;
        }).when(definitionMapper).insert(any(MarketingFormDefinitionDO.class));

        MarketingFormService.FormDefinitionView view = service(definitionMapper).create(7L,
                new MarketingFormService.FormDefinitionCommand(
                        "Signup Form",
                        "报名表单",
                        "收集活动线索",
                        "[{\"key\":\"email\"}]",
                        "{\"triggerEventCode\":\"form_signup\"}",
                        "报名成功",
                        true,
                        "tester"));

        ArgumentCaptor<MarketingFormDefinitionDO> captor = ArgumentCaptor.forClass(MarketingFormDefinitionDO.class);
        verify(definitionMapper).insert(captor.capture());
        assertThat(view.id()).isEqualTo(12L);
        assertThat(view.publicKey()).isEqualTo("signup-form");
        assertThat(view.status()).isEqualTo(MarketingFormService.STATUS_ACTIVE);
        assertThat(captor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(captor.getValue().getFieldSchemaJson()).contains("\"email\"");
    }

    @Test
    void publicSubmitCapturesLeadAndWritesProfileChannelsAndConsent() {
        MarketingFormDefinitionMapper definitionMapper = mock(MarketingFormDefinitionMapper.class);
        MarketingFormSubmissionMapper submissionMapper = mock(MarketingFormSubmissionMapper.class);
        CdpUserService cdpUserService = mock(CdpUserService.class);
        CdpUserProfileMapper profileMapper = mock(CdpUserProfileMapper.class);
        CustomerChannelMapper channelMapper = mock(CustomerChannelMapper.class);
        MarketingConsentMapper consentMapper = mock(MarketingConsentMapper.class);
        MarketingFormDefinitionDO definition = formDefinition();
        when(definitionMapper.selectOne(any())).thenReturn(definition);
        CdpUserProfileDO profile = new CdpUserProfileDO();
        profile.setTenantId(7L);
        profile.setUserId("email:lead@example.com");
        profile.setDisplayName("lead@example.com");
        when(cdpUserService.ensureUserByIdentity(
                eq(7L), eq("EMAIL"), eq("lead@example.com"), eq("MARKETING_FORM"), eq("signup")))
                .thenReturn(profile);

        MarketingFormService.SubmitResult result = new MarketingFormService(
                definitionMapper, submissionMapper, cdpUserService, profileMapper,
                channelMapper, consentMapper, new ObjectMapper()).submit("signup",
                new MarketingFormService.PublicSubmitCommand(
                        Map.of(
                                "email", "Lead@Example.com",
                                "phone", "13800000000",
                                "name", "Alice",
                                "marketingConsent", true),
                        Map.of("utm_source", "newsletter"),
                        "anon-1",
                        "idem-1",
                        null,
                        null,
                        "JUnit",
                        "hash"));

        ArgumentCaptor<MarketingFormSubmissionDO> submissionCaptor =
                ArgumentCaptor.forClass(MarketingFormSubmissionDO.class);
        ArgumentCaptor<CdpUserProfileDO> profileCaptor = ArgumentCaptor.forClass(CdpUserProfileDO.class);
        ArgumentCaptor<CustomerChannelDO> channelCaptor = ArgumentCaptor.forClass(CustomerChannelDO.class);
        ArgumentCaptor<MarketingConsentDO> consentCaptor = ArgumentCaptor.forClass(MarketingConsentDO.class);
        verify(submissionMapper).insert(submissionCaptor.capture());
        verify(profileMapper).updateById(profileCaptor.capture());
        verify(channelMapper, times(2)).insert(channelCaptor.capture());
        verify(consentMapper, times(2)).insert(consentCaptor.capture());

        assertThat(result.userId()).isEqualTo("email:lead@example.com");
        assertThat(submissionCaptor.getValue().getPublicKey()).isEqualTo("signup");
        assertThat(submissionCaptor.getValue().getConsentChannel()).isEqualTo("EMAIL,SMS");
        assertThat(submissionCaptor.getValue().getResponseJson()).contains("Lead@Example.com");
        assertThat(profileCaptor.getValue().getDisplayName()).isEqualTo("Alice");
        assertThat(profileCaptor.getValue().getEmail()).isEqualTo("lead@example.com");
        assertThat(channelCaptor.getAllValues()).extracting(CustomerChannelDO::getChannel)
                .containsExactlyInAnyOrder("EMAIL", "SMS");
        assertThat(consentCaptor.getAllValues()).extracting(MarketingConsentDO::getConsentStatus)
                .containsExactly(MarketingConsentDO.OPT_IN, MarketingConsentDO.OPT_IN);
        assertThat(consentCaptor.getAllValues()).extracting(MarketingConsentDO::getSource)
                .containsOnly("marketing_form:signup");
    }

    @Test
    void inactivePublicFormRejectsSubmissions() {
        MarketingFormDefinitionMapper definitionMapper = mock(MarketingFormDefinitionMapper.class);
        MarketingFormSubmissionMapper submissionMapper = mock(MarketingFormSubmissionMapper.class);
        MarketingFormDefinitionDO definition = formDefinition();
        definition.setStatus(MarketingFormService.STATUS_INACTIVE);
        when(definitionMapper.selectOne(any())).thenReturn(definition);

        MarketingFormService formService = service(definitionMapper, submissionMapper);

        assertThatThrownBy(() -> formService.submit("signup",
                new MarketingFormService.PublicSubmitCommand(Map.of("email", "lead@example.com"),
                        Map.of(), null, "idem-1", null, null, null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not active");
        verify(submissionMapper, never()).insert(any(MarketingFormSubmissionDO.class));
    }

    private MarketingFormService service(MarketingFormDefinitionMapper definitionMapper) {
        return service(definitionMapper, mock(MarketingFormSubmissionMapper.class));
    }

    private MarketingFormService service(MarketingFormDefinitionMapper definitionMapper,
                                         MarketingFormSubmissionMapper submissionMapper) {
        return new MarketingFormService(
                definitionMapper,
                submissionMapper,
                mock(CdpUserService.class),
                mock(CdpUserProfileMapper.class),
                mock(CustomerChannelMapper.class),
                mock(MarketingConsentMapper.class),
                new ObjectMapper());
    }

    private MarketingFormDefinitionDO formDefinition() {
        MarketingFormDefinitionDO definition = new MarketingFormDefinitionDO();
        definition.setId(9L);
        definition.setTenantId(7L);
        definition.setPublicKey("signup");
        definition.setName("报名表单");
        definition.setStatus(MarketingFormService.STATUS_ACTIVE);
        definition.setFieldSchemaJson("[{\"key\":\"email\"}]");
        definition.setSubmitActionJson("{}");
        definition.setSuccessMessage("提交成功");
        return definition;
    }
}
