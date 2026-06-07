package org.chovy.canvas.domain.marketing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.GrowthActivityDO;
import org.chovy.canvas.dal.dataobject.GrowthTaskDefinitionDO;
import org.chovy.canvas.dal.dataobject.GrowthTaskProgressDO;
import org.chovy.canvas.dal.mapper.GrowthActivityMapper;
import org.chovy.canvas.dal.mapper.GrowthTaskDefinitionMapper;
import org.chovy.canvas.dal.mapper.GrowthTaskProgressMapper;
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

class GrowthTaskServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-07T05:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    @Test
    void upsertTaskDefinitionNormalizesAndStoresCompletionPolicy() {
        Harness harness = harness();
        when(harness.activityMapper.selectById(10L)).thenReturn(activity(10L, 7L));
        when(harness.definitionMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            GrowthTaskDefinitionDO row = invocation.getArgument(0);
            row.setId(800L);
            return 1;
        }).when(harness.definitionMapper).insert(any(GrowthTaskDefinitionDO.class));

        GrowthTaskDefinitionView view = harness.service.upsertTaskDefinition(7L, 10L, new GrowthTaskDefinitionCommand(
                " Daily Login ",
                "event_count",
                "event",
                "once",
                100L,
                new BigDecimal("3"),
                "active",
                Map.of("eventType", "LOGIN")), "operator-1");

        assertThat(view.id()).isEqualTo(800L);
        assertThat(view.taskKey()).isEqualTo("daily-login");
        assertThat(view.completionPolicy()).isEqualTo("EVENT");
        assertThat(view.resetPolicy()).isEqualTo("ONCE");
        assertThat(view.rule()).containsEntry("eventType", "LOGIN");
        verify(harness.definitionMapper).insert(argThat((GrowthTaskDefinitionDO row) ->
                row.getTenantId().equals(7L)
                        && row.getActivityId().equals(10L)
                        && row.getTaskKey().equals("daily-login")
                        && row.getTaskType().equals("EVENT_COUNT")
                        && row.getCompletionPolicy().equals("EVENT")
                        && row.getResetPolicy().equals("ONCE")
                        && row.getRewardPoolId().equals(100L)
                        && row.getTargetValue().compareTo(new BigDecimal("3")) == 0
                        && row.getCreatedBy().equals("operator-1")
                        && row.getUpdatedBy().equals("operator-1")));
    }

    @Test
    void recordProgressCreatesProgressAndCompletesWithOneTimeReward() {
        Harness harness = harness();
        GrowthTaskDefinitionDO task = task(800L, 7L, 10L, "daily-login", new BigDecimal("3"), 100L, "ONCE");
        when(harness.definitionMapper.selectById(800L)).thenReturn(task);
        when(harness.progressMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            GrowthTaskProgressDO row = invocation.getArgument(0);
            row.setId(900L);
            return 1;
        }).when(harness.progressMapper).insert(any(GrowthTaskProgressDO.class));
        when(harness.rewardGrantService.createGrant(7L, 10L, new GrowthRewardGrantCommand(
                100L,
                200L,
                null,
                900L,
                "TASK_COMPLETION",
                "task:900:completion",
                Map.of("taskKey", "daily-login", "progressValue", new BigDecimal("3")),
                BigDecimal.ZERO), "operator-2")).thenReturn(grantView(950L));

        GrowthTaskProgressView view = harness.service.recordProgress(7L, 10L, new GrowthTaskProgressCommand(
                800L,
                200L,
                new BigDecimal("3"),
                "event-1",
                Map.of("eventType", "LOGIN")), "operator-2");

        assertThat(view.id()).isEqualTo(900L);
        assertThat(view.status()).isEqualTo("COMPLETED");
        assertThat(view.rewardGrantId()).isEqualTo(950L);
        assertThat(view.evidence()).containsEntry("eventType", "LOGIN");
        verify(harness.progressMapper).insert(argThat((GrowthTaskProgressDO row) ->
                row.getTenantId().equals(7L)
                        && row.getActivityId().equals(10L)
                        && row.getParticipantId().equals(200L)
                        && row.getTaskId().equals(800L)
                        && row.getProgressValue().compareTo(new BigDecimal("3")) == 0
                        && row.getTargetValue().compareTo(new BigDecimal("3")) == 0
                        && row.getStatus().equals("COMPLETED")
                        && row.getRewardGrantId().equals(950L)
                        && row.getEvidenceJson().contains("LOGIN")
                        && row.getUpdatedBy().equals("operator-2")));
    }

    @Test
    void recordProgressAccumulatesExistingProgressWithoutDuplicateReward() {
        Harness harness = harness();
        GrowthTaskDefinitionDO task = task(800L, 7L, 10L, "daily-login", new BigDecimal("3"), 100L, "ONCE");
        GrowthTaskProgressDO progress = progress(900L, 7L, 10L, 800L, 200L, new BigDecimal("1"), "IN_PROGRESS");
        when(harness.definitionMapper.selectById(800L)).thenReturn(task);
        when(harness.progressMapper.selectOne(any())).thenReturn(progress);

        GrowthTaskProgressView view = harness.service.recordProgress(7L, 10L, new GrowthTaskProgressCommand(
                800L,
                200L,
                new BigDecimal("1"),
                "event-2",
                Map.of()), "operator-2");

        assertThat(view.status()).isEqualTo("IN_PROGRESS");
        assertThat(view.progressValue()).isEqualByComparingTo(new BigDecimal("2"));
        verify(harness.rewardGrantService, never()).createGrant(any(), any(), any(), any());

        progress.setProgressValue(new BigDecimal("3"));
        progress.setStatus("COMPLETED");
        progress.setRewardGrantId(950L);
        GrowthTaskProgressView completed = harness.service.recordProgress(7L, 10L, new GrowthTaskProgressCommand(
                800L,
                200L,
                BigDecimal.ONE,
                "event-3",
                Map.of()), "operator-2");

        assertThat(completed.rewardGrantId()).isEqualTo(950L);
        verify(harness.rewardGrantService, never()).createGrant(any(), any(), any(), any());
    }

    @Test
    void resetProgressAllowsRepeatableTaskCompletion() {
        Harness harness = harness();
        GrowthTaskDefinitionDO task = task(800L, 7L, 10L, "weekly-share", BigDecimal.ONE, 100L, "MANUAL_RESET");
        GrowthTaskProgressDO progress = progress(900L, 7L, 10L, 800L, 200L, BigDecimal.ONE, "COMPLETED");
        progress.setRewardGrantId(950L);
        when(harness.definitionMapper.selectById(800L)).thenReturn(task);
        when(harness.progressMapper.selectById(900L)).thenReturn(progress);

        GrowthTaskProgressView view = harness.service.resetProgress(7L, 900L, "operator-3");

        assertThat(view.status()).isEqualTo("IN_PROGRESS");
        assertThat(view.progressValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(view.rewardGrantId()).isNull();
        verify(harness.progressMapper).updateById(argThat((GrowthTaskProgressDO row) ->
                row.getId().equals(900L)
                        && row.getProgressValue().compareTo(BigDecimal.ZERO) == 0
                        && row.getStatus().equals("IN_PROGRESS")
                        && row.getRewardGrantId() == null
                        && row.getUpdatedBy().equals("operator-3")));
    }

    @Test
    void rejectsForeignActivityTaskDefinition() {
        Harness harness = harness();
        when(harness.definitionMapper.selectById(800L)).thenReturn(task(800L, 8L, 10L, "daily-login", BigDecimal.ONE, 100L, "ONCE"));

        assertThatThrownBy(() -> harness.service.recordProgress(7L, 10L, new GrowthTaskProgressCommand(
                800L,
                200L,
                BigDecimal.ONE,
                "event",
                Map.of()), "operator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("growth task definition does not belong to activity");
    }

    @Test
    void listDefinitionsAndProgressUseTenantAndActivityScope() {
        Harness harness = harness();
        when(harness.activityMapper.selectById(10L)).thenReturn(activity(10L, 7L));
        when(harness.definitionMapper.selectList(any())).thenReturn(List.of(
                task(800L, 7L, 10L, "daily-login", new BigDecimal("3"), 100L, "DAILY")));
        when(harness.progressMapper.selectList(any())).thenReturn(List.of(
                progress(900L, 7L, 10L, 800L, 200L, new BigDecimal("3"), "COMPLETED")));

        List<GrowthTaskDefinitionView> definitions = harness.service.listTaskDefinitions(7L, 10L);
        List<GrowthTaskProgressView> progress = harness.service.listTaskProgress(7L, 10L);

        assertThat(definitions).singleElement().satisfies(task -> {
            assertThat(task.taskKey()).isEqualTo("daily-login");
            assertThat(task.targetValue()).isEqualByComparingTo("3");
        });
        assertThat(progress).singleElement().satisfies(row -> {
            assertThat(row.participantId()).isEqualTo(200L);
            assertThat(row.status()).isEqualTo("COMPLETED");
        });
        verify(harness.definitionMapper).selectList(any());
        verify(harness.progressMapper).selectList(any());
    }

    private static Harness harness() {
        GrowthActivityMapper activityMapper = mock(GrowthActivityMapper.class);
        GrowthTaskDefinitionMapper definitionMapper = mock(GrowthTaskDefinitionMapper.class);
        GrowthTaskProgressMapper progressMapper = mock(GrowthTaskProgressMapper.class);
        GrowthRewardGrantService rewardGrantService = mock(GrowthRewardGrantService.class);
        return new Harness(activityMapper, definitionMapper, progressMapper, rewardGrantService,
                new GrowthTaskService(activityMapper, definitionMapper, progressMapper, rewardGrantService,
                        new ObjectMapper(), CLOCK));
    }

    private static GrowthActivityDO activity(Long id, Long tenantId) {
        GrowthActivityDO row = new GrowthActivityDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setActivityType("TASK_INCENTIVE");
        row.setStatus("ACTIVE");
        return row;
    }

    private static GrowthTaskDefinitionDO task(Long id,
                                               Long tenantId,
                                               Long activityId,
                                               String key,
                                               BigDecimal targetValue,
                                               Long rewardPoolId,
                                               String resetPolicy) {
        GrowthTaskDefinitionDO row = new GrowthTaskDefinitionDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setActivityId(activityId);
        row.setTaskKey(key);
        row.setTaskType("EVENT_COUNT");
        row.setCompletionPolicy("EVENT");
        row.setResetPolicy(resetPolicy);
        row.setRewardPoolId(rewardPoolId);
        row.setTargetValue(targetValue);
        row.setStatus("ACTIVE");
        row.setRuleJson("{}");
        row.setCreatedBy("operator");
        row.setUpdatedBy("operator");
        return row;
    }

    private static GrowthTaskProgressDO progress(Long id,
                                                 Long tenantId,
                                                 Long activityId,
                                                 Long taskId,
                                                 Long participantId,
                                                 BigDecimal progressValue,
                                                 String status) {
        GrowthTaskProgressDO row = new GrowthTaskProgressDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setActivityId(activityId);
        row.setTaskId(taskId);
        row.setParticipantId(participantId);
        row.setProgressValue(progressValue);
        row.setTargetValue(new BigDecimal("3"));
        row.setStatus(status);
        row.setEvidenceJson("{}");
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
                null,
                900L,
                "TASK_COMPLETION",
                "RESERVED",
                "task:900:completion",
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
            GrowthTaskDefinitionMapper definitionMapper,
            GrowthTaskProgressMapper progressMapper,
            GrowthRewardGrantService rewardGrantService,
            GrowthTaskService service) {
    }
}
