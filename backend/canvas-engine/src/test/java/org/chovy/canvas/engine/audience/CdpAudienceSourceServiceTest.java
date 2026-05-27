package org.chovy.canvas.engine.audience;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CdpUserIdentityDO;
import org.chovy.canvas.dal.dataobject.CdpUserProfileDO;
import org.chovy.canvas.dal.dataobject.CdpUserTagDO;
import org.chovy.canvas.dal.dataobject.IdentityTypeDO;
import org.chovy.canvas.dal.dataobject.TagDefinitionDO;
import org.chovy.canvas.dal.mapper.CdpUserIdentityMapper;
import org.chovy.canvas.dal.mapper.CdpUserProfileMapper;
import org.chovy.canvas.dal.mapper.CdpUserTagMapper;
import org.chovy.canvas.dal.mapper.IdentityTypeMapper;
import org.chovy.canvas.dal.mapper.TagDefinitionMapper;
import org.chovy.canvas.dto.audience.AudienceSourceFieldDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CdpAudienceSourceServiceTest {

    private CdpUserTagMapper userTagMapper;
    private CdpUserProfileMapper profileMapper;
    private CdpUserIdentityMapper identityMapper;
    private TagDefinitionMapper tagDefinitionMapper;
    private IdentityTypeMapper identityTypeMapper;
    private CdpAudienceSourceService service;

    @BeforeEach
    void setUp() {
        userTagMapper = mock(CdpUserTagMapper.class);
        profileMapper = mock(CdpUserProfileMapper.class);
        identityMapper = mock(CdpUserIdentityMapper.class);
        tagDefinitionMapper = mock(TagDefinitionMapper.class);
        identityTypeMapper = mock(IdentityTypeMapper.class);
        service = new CdpAudienceSourceService(
                userTagMapper,
                profileMapper,
                identityMapper,
                tagDefinitionMapper,
                identityTypeMapper,
                new ObjectMapper()
        );
    }

    @Test
    void listSourceFieldsReturnsEnabledTagDefinitionsForCdpTag() {
        TagDefinitionDO tag = new TagDefinitionDO();
        tag.setTagCode("high_value");
        tag.setName("高价值用户");
        tag.setValueType("STRING");
        when(tagDefinitionMapper.selectList(any(Wrapper.class))).thenReturn(List.of(tag));

        var fields = service.listSourceFields("CDP_TAG");

        assertThat(fields).hasSize(1);
        assertThat(fields.get(0).name()).isEqualTo("high_value");
        assertThat(fields.get(0).label()).isEqualTo("高价值用户");
        assertThat(fields.get(0).valueType()).isEqualTo("STRING");
    }

    @Test
    void listSourceFieldsDiscoversProfilePropertyKeys() {
        when(profileMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                profile("u1", "{\"city\":\"Shanghai\",\"level\":3}")
        ));

        var fields = service.listSourceFields("CDP_PROFILE");

        assertThat(fields).extracting(AudienceSourceFieldDTO::name)
                .contains("displayName", "status", "city", "level");
    }

    @Test
    void resolveUserIdsByCdpTagEvaluatesMultipleTagsPerUser() {
        CdpUserTagDO vip = tag("u1", "high_value", "VIP");
        CdpUserTagDO risk = tag("u1", "churn_risk", "HIGH");
        CdpUserTagDO normal = tag("u2", "high_value", "NORMAL");
        CdpUserTagDO otherRisk = tag("u3", "churn_risk", "HIGH");
        when(userTagMapper.selectList(any(Wrapper.class))).thenReturn(List.of(vip, risk, normal, otherRisk));
        when(tagDefinitionMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                tagDefinition("high_value", "高价值用户", "STRING"),
                tagDefinition("churn_risk", "流失风险", "STRING")
        ));

        String ruleJson = """
                {
                  "logic":"AND",
                  "conditions":[
                    {"field":"high_value","op":"=","value":"VIP"},
                    {"field":"churn_risk","op":"IN","value":["HIGH","MEDIUM"]}
                  ]
                }
                """;

        assertThat(service.resolveUserIds("CDP_TAG", ruleJson))
                .containsExactly("u1");
    }

    @Test
    void resolveUserIdsByCdpIdentityMatchesTypeAndValue() {
        CdpUserIdentityDO mobile = new CdpUserIdentityDO();
        mobile.setUserId("u1");
        mobile.setIdentityType("mobile");
        mobile.setIdentityValue("13812345678");
        CdpUserIdentityDO email = new CdpUserIdentityDO();
        email.setUserId("u2");
        email.setIdentityType("email");
        email.setIdentityValue("user@example.com");
        when(identityMapper.selectList(any(Wrapper.class))).thenReturn(List.of(mobile, email));
        when(identityTypeMapper.selectList(any(Wrapper.class))).thenReturn(List.of(identityType("MOBILE")));

        String ruleJson = """
                {
                  "logic":"AND",
                  "conditions":[
                    {"field":"MOBILE","op":"=","value":"13812345678"}
                  ]
                }
                """;

        assertThat(service.resolveUserIds("CDP_IDENTITY", ruleJson))
                .containsExactly("u1");
    }

    @Test
    void resolveUserIdsByCdpIdentityMatchesAnyValueForMultiValueType() {
        CdpUserIdentityDO firstEmail = identity("u1", "EMAIL", "a@example.com");
        CdpUserIdentityDO secondEmail = identity("u1", "EMAIL", "b@example.com");
        CdpUserIdentityDO otherEmail = identity("u2", "EMAIL", "c@example.com");
        when(identityMapper.selectList(any(Wrapper.class))).thenReturn(List.of(firstEmail, secondEmail, otherEmail));
        when(identityTypeMapper.selectList(any(Wrapper.class))).thenReturn(List.of(identityType("EMAIL")));

        String ruleJson = """
                {
                  "logic":"AND",
                  "conditions":[
                    {"field":"EMAIL","op":"=","value":"a@example.com"}
                  ]
                }
                """;

        assertThat(service.resolveUserIds("CDP_IDENTITY", ruleJson))
                .containsExactly("u1");
    }

    @Test
    void resolveUserIdsIgnoresDisabledTagDefinitions() {
        when(userTagMapper.selectList(any(Wrapper.class))).thenReturn(List.of(tag("u1", "disabled_tag", "VIP")));
        when(tagDefinitionMapper.selectList(any(Wrapper.class))).thenReturn(List.of(tagDefinition("enabled_tag", "启用标签", "STRING")));

        String ruleJson = """
                {
                  "logic":"AND",
                  "conditions":[
                    {"field":"disabled_tag","op":"=","value":"VIP"}
                  ]
                }
                """;

        assertThat(service.resolveUserIds("CDP_TAG", ruleJson)).isEmpty();
    }

    @Test
    void resolveUserIdsIgnoresDisabledIdentityTypes() {
        when(identityMapper.selectList(any(Wrapper.class))).thenReturn(List.of(identity("u1", "EMAIL", "a@example.com")));
        when(identityTypeMapper.selectList(any(Wrapper.class))).thenReturn(List.of(identityType("MOBILE")));

        String ruleJson = """
                {
                  "logic":"AND",
                  "conditions":[
                    {"field":"EMAIL","op":"=","value":"a@example.com"}
                  ]
                }
                """;

        assertThat(service.resolveUserIds("CDP_IDENTITY", ruleJson)).isEmpty();
    }

    @Test
    void resolveUserIdsByCdpProfileMatchesPropertiesJson() {
        CdpUserProfileDO shanghai = profile("u1", "{\"city\":\"Shanghai\",\"level\":3}");
        CdpUserProfileDO beijing = profile("u2", "{\"city\":\"Beijing\",\"level\":1}");
        when(profileMapper.selectList(any(Wrapper.class))).thenReturn(List.of(shanghai, beijing));

        String ruleJson = """
                {
                  "logic":"AND",
                  "conditions":[
                    {"field":"city","op":"=","value":"Shanghai"},
                    {"field":"level","op":">=","value":2}
                  ]
                }
                """;

        assertThat(service.resolveUserIds("CDP_PROFILE", ruleJson))
                .containsExactly("u1");
    }

    @Test
    void resolveUserIdsByCdpProfileTreatsMissingNumericFieldAsNotMatched() {
        when(profileMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                profile("u1", "{\"city\":\"Shanghai\"}"),
                profile("u2", "{\"city\":\"Beijing\",\"level\":3}")
        ));

        String ruleJson = """
                {
                  "logic":"AND",
                  "conditions":[
                    {"field":"level","op":">=","value":2}
                  ]
                }
                """;

        assertThat(service.resolveUserIds("CDP_PROFILE", ruleJson))
                .containsExactly("u2");
    }

    @Test
    void resolveUserIdsTreatsEmptyOrGroupAsMatchAllLikeExistingAudienceEngines() {
        when(profileMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                profile("u1", "{\"city\":\"Shanghai\"}"),
                profile("u2", "{\"city\":\"Beijing\"}")
        ));

        String ruleJson = """
                {
                  "logic":"OR",
                  "conditions":[],
                  "groups":[]
                }
                """;

        assertThat(service.resolveUserIds("CDP_PROFILE", ruleJson))
                .containsExactly("u1", "u2");
    }

    private CdpUserTagDO tag(String userId, String tagCode, String tagValue) {
        CdpUserTagDO tag = new CdpUserTagDO();
        tag.setUserId(userId);
        tag.setTagCode(tagCode);
        tag.setTagValue(tagValue);
        tag.setStatus("ACTIVE");
        return tag;
    }

    private CdpUserIdentityDO identity(String userId, String identityType, String identityValue) {
        CdpUserIdentityDO identity = new CdpUserIdentityDO();
        identity.setUserId(userId);
        identity.setIdentityType(identityType);
        identity.setIdentityValue(identityValue);
        return identity;
    }

    private TagDefinitionDO tagDefinition(String tagCode, String name, String valueType) {
        TagDefinitionDO tag = new TagDefinitionDO();
        tag.setTagCode(tagCode);
        tag.setName(name);
        tag.setValueType(valueType);
        tag.setEnabled(1);
        return tag;
    }

    private IdentityTypeDO identityType(String code) {
        IdentityTypeDO identityType = new IdentityTypeDO();
        identityType.setCode(code);
        identityType.setName(code);
        identityType.setEnabled(1);
        return identityType;
    }

    private CdpUserProfileDO profile(String userId, String propertiesJson) {
        CdpUserProfileDO profile = new CdpUserProfileDO();
        profile.setUserId(userId);
        profile.setPropertiesJson(propertiesJson);
        profile.setStatus("ACTIVE");
        return profile;
    }
}
