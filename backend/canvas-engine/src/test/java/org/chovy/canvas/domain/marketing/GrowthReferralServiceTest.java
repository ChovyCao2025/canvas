package org.chovy.canvas.domain.marketing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.GrowthActivityDO;
import org.chovy.canvas.dal.dataobject.GrowthReferralCodeDO;
import org.chovy.canvas.dal.dataobject.GrowthReferralRelationDO;
import org.chovy.canvas.dal.mapper.GrowthActivityMapper;
import org.chovy.canvas.dal.mapper.GrowthReferralCodeMapper;
import org.chovy.canvas.dal.mapper.GrowthReferralRelationMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GrowthReferralServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-07T04:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    @Test
    void generateCodeCreatesStableTenantScopedCodeForParticipant() {
        Harness harness = harness();
        when(harness.activityMapper.selectById(10L)).thenReturn(activity(10L, 7L));
        when(harness.codeMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            GrowthReferralCodeDO row = invocation.getArgument(0);
            row.setId(500L);
            return 1;
        }).when(harness.codeMapper).insert(any(GrowthReferralCodeDO.class));

        GrowthReferralCodeView view = harness.service.generateCode(7L, 10L, 200L, "operator-1");

        assertThat(view.id()).isEqualTo(500L);
        assertThat(view.code()).isEqualTo("G10P200");
        assertThat(view.status()).isEqualTo("ACTIVE");
        verify(harness.codeMapper).insert(argThat((GrowthReferralCodeDO row) ->
                row.getTenantId().equals(7L)
                        && row.getActivityId().equals(10L)
                        && row.getParticipantId().equals(200L)
                        && row.getCode().equals("G10P200")
                        && row.getCreatedBy().equals("operator-1")));
    }

    @Test
    void upsertRelationResolvesActiveCodeAndStoresRiskEvidence() {
        Harness harness = harness();
        when(harness.activityMapper.selectById(10L)).thenReturn(activity(10L, 7L));
        when(harness.codeMapper.selectOne(any())).thenReturn(code(500L, 7L, 10L, 200L, "G10P200", "ACTIVE"));
        when(harness.relationMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            GrowthReferralRelationDO row = invocation.getArgument(0);
            row.setId(700L);
            return 1;
        }).when(harness.relationMapper).insert(any(GrowthReferralRelationDO.class));

        GrowthReferralRelationView view = harness.service.upsertRelation(7L, 10L, new GrowthReferralRelationCommand(
                " G10P200 ",
                " invitee-1 ",
                Map.of("ipRisk", "LOW")), "operator-2");

        assertThat(view.id()).isEqualTo(700L);
        assertThat(view.referrerParticipantId()).isEqualTo(200L);
        assertThat(view.inviteeUserId()).isEqualTo("invitee-1");
        assertThat(view.status()).isEqualTo("PENDING");
        assertThat(view.riskEvidence()).containsEntry("ipRisk", "LOW");
        verify(harness.relationMapper).insert(argThat((GrowthReferralRelationDO row) ->
                row.getTenantId().equals(7L)
                        && row.getActivityId().equals(10L)
                        && row.getReferralCodeId().equals(500L)
                        && row.getReferrerParticipantId().equals(200L)
                        && row.getInviteeUserId().equals("invitee-1")
                        && row.getRiskEvidenceJson().contains("ipRisk")
                        && row.getCreatedBy().equals("operator-2")
                        && row.getUpdatedBy().equals("operator-2")));
    }

    @Test
    void upsertRelationReturnsExistingInviteeRelationAndRejectsInactiveCode() {
        Harness harness = harness();
        when(harness.activityMapper.selectById(10L)).thenReturn(activity(10L, 7L));
        when(harness.codeMapper.selectOne(any())).thenReturn(code(500L, 7L, 10L, 200L, "G10P200", "ACTIVE"));
        when(harness.relationMapper.selectOne(any())).thenReturn(relation(700L, 7L, 10L, 500L, 200L, "invitee-1", "PENDING"));

        assertThat(harness.service.upsertRelation(7L, 10L, new GrowthReferralRelationCommand(
                "G10P200", "invitee-1", Map.of()), "operator-2").id()).isEqualTo(700L);
        verify(harness.relationMapper, never()).insert(any(GrowthReferralRelationDO.class));

        when(harness.codeMapper.selectOne(any())).thenReturn(code(501L, 7L, 10L, 201L, "OLD", "PAUSED"));
        assertThatThrownBy(() -> harness.service.upsertRelation(7L, 10L, new GrowthReferralRelationCommand(
                "OLD", "invitee-2", Map.of()), "operator-2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("referral code is not active");
    }

    @Test
    void qualifyRelationCreatesInviterAndInviteeRewardGrants() {
        Harness harness = harness();
        GrowthReferralRelationDO relation = relation(700L, 7L, 10L, 500L, 200L, "invitee-1", "PENDING");
        when(harness.relationMapper.selectById(700L)).thenReturn(relation);
        when(harness.rewardGrantService.createGrant(7L, 10L, new GrowthRewardGrantCommand(
                100L,
                200L,
                700L,
                null,
                "REFERRAL_INVITER",
                "referral:700:inviter",
                Map.of("role", "INVITER", "inviteeUserId", "invitee-1"),
                BigDecimal.ZERO), "operator-3")).thenReturn(grantView(900L));
        when(harness.rewardGrantService.createGrant(7L, 10L, new GrowthRewardGrantCommand(
                101L,
                null,
                700L,
                null,
                "REFERRAL_INVITEE",
                "referral:700:invitee",
                Map.of("role", "INVITEE", "inviteeUserId", "invitee-1"),
                BigDecimal.ZERO), "operator-3")).thenReturn(grantView(901L));

        GrowthReferralRelationView view = harness.service.qualifyRelation(7L, 700L, new GrowthReferralQualificationCommand(
                100L,
                101L,
                Map.of("riskDecision", "PASS")), "operator-3");

        assertThat(view.status()).isEqualTo("QUALIFIED");
        assertThat(view.inviterRewardGrantId()).isEqualTo(900L);
        assertThat(view.inviteeRewardGrantId()).isEqualTo(901L);
        assertThat(view.riskEvidence()).containsEntry("riskDecision", "PASS");
        verify(harness.relationMapper).updateById(argThat((GrowthReferralRelationDO row) ->
                row.getId().equals(700L)
                        && row.getStatus().equals("QUALIFIED")
                        && row.getInviterRewardGrantId().equals(900L)
                        && row.getInviteeRewardGrantId().equals(901L)
                        && row.getRiskEvidenceJson().contains("riskDecision")
                        && row.getUpdatedBy().equals("operator-3")));
    }

    @Test
    void listCodesAndRelationsUseTenantAndActivityScope() {
        Harness harness = harness();
        when(harness.activityMapper.selectById(10L)).thenReturn(activity(10L, 7L));
        when(harness.codeMapper.selectList(any())).thenReturn(List.of(
                code(500L, 7L, 10L, 200L, "G10P200", "ACTIVE")));
        when(harness.relationMapper.selectList(any())).thenReturn(List.of(
                relation(700L, 7L, 10L, 500L, 200L, "invitee-1", "QUALIFIED")));

        List<GrowthReferralCodeView> codes = harness.service.listCodes(7L, 10L);
        List<GrowthReferralRelationView> relations = harness.service.listRelations(7L, 10L);

        assertThat(codes).singleElement().satisfies(code -> {
            assertThat(code.code()).isEqualTo("G10P200");
            assertThat(code.participantId()).isEqualTo(200L);
        });
        assertThat(relations).singleElement().satisfies(relation -> {
            assertThat(relation.inviteeUserId()).isEqualTo("invitee-1");
            assertThat(relation.status()).isEqualTo("QUALIFIED");
        });
        verify(harness.codeMapper).selectList(any());
        verify(harness.relationMapper).selectList(any());
    }

    private static Harness harness() {
        GrowthActivityMapper activityMapper = mock(GrowthActivityMapper.class);
        GrowthReferralCodeMapper codeMapper = mock(GrowthReferralCodeMapper.class);
        GrowthReferralRelationMapper relationMapper = mock(GrowthReferralRelationMapper.class);
        GrowthRewardGrantService rewardGrantService = mock(GrowthRewardGrantService.class);
        return new Harness(activityMapper, codeMapper, relationMapper, rewardGrantService,
                new GrowthReferralService(activityMapper, codeMapper, relationMapper, rewardGrantService,
                        new ObjectMapper(), CLOCK));
    }

    private static GrowthActivityDO activity(Long id, Long tenantId) {
        GrowthActivityDO row = new GrowthActivityDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setActivityType("REFERRAL_INVITE");
        row.setStatus("ACTIVE");
        return row;
    }

    private static GrowthReferralCodeDO code(Long id, Long tenantId, Long activityId, Long participantId, String code, String status) {
        GrowthReferralCodeDO row = new GrowthReferralCodeDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setActivityId(activityId);
        row.setParticipantId(participantId);
        row.setCode(code);
        row.setStatus(status);
        row.setCreatedBy("operator");
        return row;
    }

    private static GrowthReferralRelationDO relation(Long id,
                                                     Long tenantId,
                                                     Long activityId,
                                                     Long codeId,
                                                     Long referrerParticipantId,
                                                     String inviteeUserId,
                                                     String status) {
        GrowthReferralRelationDO row = new GrowthReferralRelationDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setActivityId(activityId);
        row.setReferralCodeId(codeId);
        row.setReferrerParticipantId(referrerParticipantId);
        row.setInviteeUserId(inviteeUserId);
        row.setStatus(status);
        row.setRiskEvidenceJson("{}");
        row.setCreatedBy("operator");
        row.setUpdatedBy("operator");
        return row;
    }

    private static GrowthRewardGrantView grantView(Long id) {
        return new GrowthRewardGrantView(
                id,
                7L,
                10L,
                100L,
                200L,
                700L,
                null,
                "REFERRAL",
                "RESERVED",
                "idem",
                Map.of(),
                Map.of(),
                BigDecimal.ZERO,
                "operator",
                "operator",
                null,
                null);
    }

    private record Harness(
            GrowthActivityMapper activityMapper,
            GrowthReferralCodeMapper codeMapper,
            GrowthReferralRelationMapper relationMapper,
            GrowthRewardGrantService rewardGrantService,
            GrowthReferralService service) {
    }
}
