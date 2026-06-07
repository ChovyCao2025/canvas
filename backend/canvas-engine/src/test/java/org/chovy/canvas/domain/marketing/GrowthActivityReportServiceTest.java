package org.chovy.canvas.domain.marketing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.GrowthActivityDO;
import org.chovy.canvas.dal.dataobject.GrowthActivityEventDO;
import org.chovy.canvas.dal.dataobject.GrowthActivityParticipantDO;
import org.chovy.canvas.dal.dataobject.GrowthReferralRelationDO;
import org.chovy.canvas.dal.dataobject.GrowthRewardGrantDO;
import org.chovy.canvas.dal.dataobject.GrowthTaskProgressDO;
import org.chovy.canvas.dal.mapper.GrowthActivityEventMapper;
import org.chovy.canvas.dal.mapper.GrowthActivityMapper;
import org.chovy.canvas.dal.mapper.GrowthActivityParticipantMapper;
import org.chovy.canvas.dal.mapper.GrowthReferralRelationMapper;
import org.chovy.canvas.dal.mapper.GrowthRewardGrantMapper;
import org.chovy.canvas.dal.mapper.GrowthTaskProgressMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GrowthActivityReportServiceTest {

    @Test
    void summarizeAggregatesFunnelGrantCostConversionReferralAndTaskMetrics() {
        Harness harness = harness();
        when(harness.activityMapper.selectById(10L)).thenReturn(activity(10L, 7L));
        when(harness.participantMapper.selectList(any())).thenReturn(List.of(
                participant(1L, 7L, 10L, "ACTIVE"),
                participant(2L, 7L, 10L, "ACTIVE"),
                participant(3L, 7L, 10L, "EXITED"),
                participant(4L, 8L, 10L, "ACTIVE")));
        when(harness.referralRelationMapper.selectList(any())).thenReturn(List.of(
                referral(11L, 7L, 10L, "QUALIFIED"),
                referral(12L, 7L, 10L, "PENDING"),
                referral(13L, 7L, 10L, "REJECTED"),
                referral(14L, 8L, 10L, "QUALIFIED")));
        when(harness.grantMapper.selectList(any())).thenReturn(List.of(
                grant(101L, 7L, 10L, "RESERVED", "5.00"),
                grant(102L, 7L, 10L, "SUCCESS", "10.00"),
                grant(103L, 7L, 10L, "REDEEMED", "20.00"),
                grant(104L, 7L, 10L, "FAILED", "0.00"),
                grant(105L, 8L, 10L, "SUCCESS", "999.00")));
        when(harness.taskProgressMapper.selectList(any())).thenReturn(List.of(
                task(201L, 7L, 10L, "COMPLETED"),
                task(202L, 7L, 10L, "IN_PROGRESS"),
                task(203L, 8L, 10L, "COMPLETED")));
        when(harness.eventMapper.selectList(any())).thenReturn(List.of(
                conversion(301L, 7L, 10L, "{\"amount\":100.50,\"currency\":\"CNY\"}"),
                conversion(302L, 7L, 10L, "{\"conversionAmount\":49.50}"),
                event(303L, 7L, 10L, "GRANT_TRANSITION", "{}"),
                conversion(304L, 8L, 10L, "{\"amount\":999.00}")));

        GrowthActivityReportView view = harness.service.summarize(7L, 10L);

        assertThat(view.activityId()).isEqualTo(10L);
        assertThat(view.participation().totalParticipants()).isEqualTo(3);
        assertThat(view.participation().activeParticipants()).isEqualTo(2);
        assertThat(view.referral().totalRelations()).isEqualTo(3);
        assertThat(view.referral().qualifiedRelations()).isEqualTo(1);
        assertThat(view.grants().totalGrants()).isEqualTo(4);
        assertThat(view.grants().reservedGrants()).isEqualTo(1);
        assertThat(view.grants().successGrants()).isEqualTo(1);
        assertThat(view.grants().redeemedGrants()).isEqualTo(1);
        assertThat(view.grants().failedGrants()).isEqualTo(1);
        assertThat(view.grants().totalCost()).isEqualByComparingTo("35.00");
        assertThat(view.conversion().conversionCount()).isEqualTo(2);
        assertThat(view.conversion().conversionAmount()).isEqualByComparingTo("150.00");
        assertThat(view.conversion().roi()).isEqualByComparingTo("4.2857");
        assertThat(view.task().totalProgress()).isEqualTo(2);
        assertThat(view.task().completedProgress()).isEqualTo(1);
    }

    @Test
    void summarizeRejectsForeignActivity() {
        Harness harness = harness();
        when(harness.activityMapper.selectById(10L)).thenReturn(activity(10L, 8L));

        assertThatThrownBy(() -> harness.service.summarize(7L, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("growth activity does not belong to tenant");
    }

    private static Harness harness() {
        GrowthActivityMapper activityMapper = mock(GrowthActivityMapper.class);
        GrowthActivityParticipantMapper participantMapper = mock(GrowthActivityParticipantMapper.class);
        GrowthRewardGrantMapper grantMapper = mock(GrowthRewardGrantMapper.class);
        GrowthReferralRelationMapper referralRelationMapper = mock(GrowthReferralRelationMapper.class);
        GrowthTaskProgressMapper taskProgressMapper = mock(GrowthTaskProgressMapper.class);
        GrowthActivityEventMapper eventMapper = mock(GrowthActivityEventMapper.class);
        return new Harness(activityMapper, participantMapper, grantMapper, referralRelationMapper,
                taskProgressMapper, eventMapper, new GrowthActivityReportService(
                activityMapper,
                participantMapper,
                grantMapper,
                referralRelationMapper,
                taskProgressMapper,
                eventMapper,
                new ObjectMapper()));
    }

    private static GrowthActivityDO activity(Long id, Long tenantId) {
        GrowthActivityDO row = new GrowthActivityDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setStatus("ACTIVE");
        return row;
    }

    private static GrowthActivityParticipantDO participant(Long id, Long tenantId, Long activityId, String status) {
        GrowthActivityParticipantDO row = new GrowthActivityParticipantDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setActivityId(activityId);
        row.setStatus(status);
        return row;
    }

    private static GrowthReferralRelationDO referral(Long id, Long tenantId, Long activityId, String status) {
        GrowthReferralRelationDO row = new GrowthReferralRelationDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setActivityId(activityId);
        row.setStatus(status);
        return row;
    }

    private static GrowthRewardGrantDO grant(Long id, Long tenantId, Long activityId, String status, String cost) {
        GrowthRewardGrantDO row = new GrowthRewardGrantDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setActivityId(activityId);
        row.setStatus(status);
        row.setCostAmount(new BigDecimal(cost));
        return row;
    }

    private static GrowthTaskProgressDO task(Long id, Long tenantId, Long activityId, String status) {
        GrowthTaskProgressDO row = new GrowthTaskProgressDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setActivityId(activityId);
        row.setStatus(status);
        return row;
    }

    private static GrowthActivityEventDO conversion(Long id, Long tenantId, Long activityId, String payload) {
        return event(id, tenantId, activityId, "CONVERSION_EVIDENCE", payload);
    }

    private static GrowthActivityEventDO event(Long id, Long tenantId, Long activityId, String type, String payload) {
        GrowthActivityEventDO row = new GrowthActivityEventDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setActivityId(activityId);
        row.setEventType(type);
        row.setPayloadJson(payload);
        return row;
    }

    private record Harness(
            GrowthActivityMapper activityMapper,
            GrowthActivityParticipantMapper participantMapper,
            GrowthRewardGrantMapper grantMapper,
            GrowthReferralRelationMapper referralRelationMapper,
            GrowthTaskProgressMapper taskProgressMapper,
            GrowthActivityEventMapper eventMapper,
            GrowthActivityReportService service) {
    }
}
