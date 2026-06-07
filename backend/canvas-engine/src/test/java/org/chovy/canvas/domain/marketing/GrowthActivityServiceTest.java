package org.chovy.canvas.domain.marketing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.GrowthActivityDO;
import org.chovy.canvas.dal.mapper.GrowthActivityMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
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

class GrowthActivityServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-07T02:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    @Test
    void upsertActivityNormalizesAndInsertsTenantScopedRecord() {
        Harness harness = harness();
        when(harness.mapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            GrowthActivityDO row = invocation.getArgument(0);
            row.setId(100L);
            return 1;
        }).when(harness.mapper).insert(any(GrowthActivityDO.class));

        GrowthActivityView view = harness.service.upsertActivity(7L, new GrowthActivityCommand(
                " Invite Spring 2026! ",
                " Invite spring ",
                "referral_invite",
                "draft",
                10L,
                "acquisition",
                " Growth ",
                LocalDateTime.parse("2026-06-01T00:00:00"),
                LocalDateTime.parse("2026-06-30T23:59:00"),
                "PRIVATE_DOMAIN",
                Map.of("segment", "new-user"),
                "risk-policy-1",
                "exp-1",
                "dashboard-1",
                Map.of("reward", "both-side")), "operator-1");

        assertThat(view.id()).isEqualTo(100L);
        assertThat(view.activityKey()).isEqualTo("invite-spring-2026");
        assertThat(view.activityType()).isEqualTo("REFERRAL_INVITE");
        assertThat(view.status()).isEqualTo("DRAFT");
        assertThat(view.objective()).isEqualTo("ACQUISITION");
        assertThat(view.riskPolicyRef()).isEqualTo("risk-policy-1");
        assertThat(view.experimentRef()).isEqualTo("exp-1");
        assertThat(view.dashboardRef()).isEqualTo("dashboard-1");
        assertThat(view.metadata()).containsEntry("reward", "both-side");
        verify(harness.mapper).insert(argThat((GrowthActivityDO row) ->
                row.getTenantId().equals(7L)
                        && row.getActivityKey().equals("invite-spring-2026")
                        && row.getActivityName().equals("Invite spring")
                        && row.getActivityType().equals("REFERRAL_INVITE")
                        && row.getCampaignId().equals(10L)
                        && row.getOwnerTeam().equals("Growth")
                        && row.getAudienceRefsJson().contains("\"segment\"")
                        && row.getRiskPolicyRef().equals("risk-policy-1")
                        && row.getExperimentRef().equals("exp-1")
                        && row.getDashboardRef().equals("dashboard-1")
                        && row.getMetadataJson().contains("\"reward\"")
                        && row.getCreatedBy().equals("operator-1")
                        && row.getUpdatedBy().equals("operator-1")));
    }

    @Test
    void upsertActivityUpdatesExistingTenantKeyAndRejectsUnsupportedType() {
        Harness harness = harness();
        GrowthActivityDO existing = activity(10L, 7L, "invite-spring", "REFERRAL_INVITE", "DRAFT");
        when(harness.mapper.selectOne(any())).thenReturn(existing);

        GrowthActivityView view = harness.service.upsertActivity(7L, new GrowthActivityCommand(
                "invite-spring",
                "Invite spring v2",
                "benefit_promotion",
                "paused",
                null,
                "retention",
                null,
                null,
                null,
                null,
                Map.of(),
                null,
                null,
                null,
                Map.of()), "operator-2");

        assertThat(view.id()).isEqualTo(10L);
        assertThat(view.activityType()).isEqualTo("BENEFIT_PROMOTION");
        assertThat(view.status()).isEqualTo("PAUSED");
        verify(harness.mapper).updateById(argThat((GrowthActivityDO row) ->
                row.getId().equals(10L)
                        && row.getActivityName().equals("Invite spring v2")
                        && row.getUpdatedBy().equals("operator-2")));

        assertThatThrownBy(() -> harness.service.upsertActivity(7L, new GrowthActivityCommand(
                "bad",
                "Bad",
                "flash_sale",
                "draft",
                null,
                null,
                null,
                null,
                null,
                null,
                Map.of(),
                null,
                null,
                null,
                Map.of()), "operator-2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported activity type");
        verify(harness.mapper, never()).insert(argThat((GrowthActivityDO row) -> "bad".equals(row.getActivityKey())));
    }

    @Test
    void listAndDetailEnforceTenantScope() {
        Harness harness = harness();
        when(harness.mapper.selectList(any())).thenReturn(List.of(
                activity(10L, 7L, "invite-spring", "REFERRAL_INVITE", "ACTIVE")));
        when(harness.mapper.selectById(10L)).thenReturn(
                activity(10L, 7L, "invite-spring", "REFERRAL_INVITE", "ACTIVE"));
        when(harness.mapper.selectById(20L)).thenReturn(
                activity(20L, 8L, "foreign", "REFERRAL_INVITE", "ACTIVE"));

        assertThat(harness.service.listActivities(7L, "referral_invite", "active", 20))
                .singleElement()
                .satisfies(row -> assertThat(row.activityKey()).isEqualTo("invite-spring"));
        assertThat(harness.service.getActivity(7L, 10L).tenantId()).isEqualTo(7L);

        assertThatThrownBy(() -> harness.service.getActivity(7L, 20L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("growth activity does not belong to tenant");
    }

    @Test
    void lifecycleTransitionsPublishPauseAndCloseActivity() {
        Harness harness = harness();
        GrowthActivityDO row = activity(10L, 7L, "invite-spring", "REFERRAL_INVITE", "DRAFT");
        when(harness.mapper.selectById(10L)).thenReturn(row);

        assertThat(harness.service.publishActivity(7L, 10L, "operator-1").status()).isEqualTo("ACTIVE");
        verify(harness.mapper).updateById(argThat((GrowthActivityDO updated) ->
                updated.getStatus().equals("ACTIVE") && updated.getUpdatedBy().equals("operator-1")));

        row.setStatus("ACTIVE");
        assertThat(harness.service.pauseActivity(7L, 10L, "operator-2").status()).isEqualTo("PAUSED");

        row.setStatus("PAUSED");
        assertThat(harness.service.closeActivity(7L, 10L, "operator-3").status()).isEqualTo("CLOSED");

        row.setStatus("CLOSED");
        assertThatThrownBy(() -> harness.service.publishActivity(7L, 10L, "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot publish activity from status CLOSED");
    }

    private static Harness harness() {
        GrowthActivityMapper mapper = mock(GrowthActivityMapper.class);
        return new Harness(mapper, new GrowthActivityService(mapper, new ObjectMapper(), CLOCK));
    }

    private static GrowthActivityDO activity(Long id, Long tenantId, String key, String type, String status) {
        GrowthActivityDO row = new GrowthActivityDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setActivityKey(key);
        row.setActivityName(key);
        row.setActivityType(type);
        row.setStatus(status);
        row.setObjective("ACQUISITION");
        row.setAudienceRefsJson("{}");
        row.setRiskPolicyRef("risk-policy-1");
        row.setExperimentRef("exp-1");
        row.setDashboardRef("dashboard-1");
        row.setMetadataJson("{}");
        row.setCreatedBy("operator-1");
        row.setUpdatedBy("operator-1");
        return row;
    }

    private record Harness(GrowthActivityMapper mapper, GrowthActivityService service) {
    }
}
