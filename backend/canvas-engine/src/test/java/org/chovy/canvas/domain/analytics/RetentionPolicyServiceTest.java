package org.chovy.canvas.domain.analytics;

import org.chovy.canvas.dal.dataobject.AnalyticsRetentionPolicyDO;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RetentionPolicyServiceTest {

    @Test
    void migrationCreatesRetentionPolicyAndRunTables() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V133__analytics_retention_policy.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS analytics_retention_policy")
                .contains("record_kind")
                .contains("retention_days")
                .contains("action")
                .contains("max_batch_size")
                .contains("CREATE TABLE IF NOT EXISTS analytics_retention_run")
                .contains("dry_run")
                .contains("archived_count")
                .contains("deleted_count");
    }

    @Test
    void tenantOverrideFallsBackToPlatformDefaultPolicy() {
        RetentionPolicyService.PolicyRepository policies = mock(RetentionPolicyService.PolicyRepository.class);
        when(policies.findPolicy(7L, "EVENT")).thenReturn(Optional.empty());
        when(policies.findPolicy(0L, "EVENT")).thenReturn(Optional.of(policy(0L, "EVENT", 120, "ARCHIVE", 250)));
        RetentionPolicyService service = serviceWithPolicies(policies);

        RetentionPolicyService.PolicyInput input = service.resolvePolicy(7L, "EVENT", false);

        assertThat(input.tenantId()).isEqualTo(7L);
        assertThat(input.recordKind()).isEqualTo("EVENT");
        assertThat(input.retentionDays()).isEqualTo(120);
        assertThat(input.action()).isEqualTo("ARCHIVE");
        assertThat(input.maxBatchSize()).isEqualTo(250);
    }

    @Test
    void tenantOverrideMustStayWithinPlatformBounds() {
        RetentionPolicyService service = service();

        assertThatThrownBy(() -> service.validate(
                new RetentionPolicyService.PolicyInput(0L, "EVENT", 1, "DELETE", 100, false)))
                .hasMessageContaining("below platform minimum");
        assertThatThrownBy(() -> service.validate(
                new RetentionPolicyService.PolicyInput(0L, "EVENT", 1000, "DELETE", 100, false)))
                .hasMessageContaining("above platform maximum");
        assertThatThrownBy(() -> service.validate(
                new RetentionPolicyService.PolicyInput(0L, "EVENT", 90, "DELETE", 0, false)))
                .hasMessageContaining("maxBatchSize must be positive");
        assertThatThrownBy(() -> service.validate(
                new RetentionPolicyService.PolicyInput(0L, "MESSAGE", 90, "DELETE", 100, false)))
                .hasMessageContaining("unsupported record kind");
    }

    @Test
    void dryRunCountsEligibleRowsWithoutMutating() {
        RetentionPolicyService.RetentionTargetRepository targets = mock(RetentionPolicyService.RetentionTargetRepository.class);
        when(targets.countEligible(0L, "TRACE", 90, true)).thenReturn(42L);
        RetentionPolicyService.RunRepository runs = mock(RetentionPolicyService.RunRepository.class);
        RetentionPolicyService service = serviceWithTargets(targets, runs);

        RetentionPolicyService.RunResult result = service.run(
                new RetentionPolicyService.PolicyInput(0L, "TRACE", 90, "ARCHIVE", 100, true));

        assertThat(result.scannedCount()).isEqualTo(42);
        assertThat(result.archivedCount()).isZero();
        assertThat(result.deletedCount()).isZero();
        assertThat(result.skippedCount()).isZero();
        verify(targets, never()).archiveBatch(any(), any(), eq(90), eq(100));
        verify(targets, never()).deleteBatch(any(), any(), eq(90), eq(100));
        verify(runs).record(result);
    }

    @Test
    void archiveAndDeleteAreLimitedToMaxBatchSizeAndSkipLegalHold() {
        RetentionPolicyService.RetentionTargetRepository targets = mock(RetentionPolicyService.RetentionTargetRepository.class);
        when(targets.countEligible(0L, "EVENT", 90, true)).thenReturn(12L);
        when(targets.archiveBatch(0L, "EVENT", 90, 10)).thenReturn(10);
        when(targets.countEligible(0L, "TRACE", 180, true)).thenReturn(8L);
        when(targets.deleteBatch(0L, "TRACE", 180, 5)).thenReturn(5);
        RetentionPolicyService service = serviceWithTargets(targets, mock(RetentionPolicyService.RunRepository.class));

        RetentionPolicyService.RunResult archive = service.run(
                new RetentionPolicyService.PolicyInput(0L, "EVENT", 90, "ARCHIVE", 10, false));
        RetentionPolicyService.RunResult delete = service.run(
                new RetentionPolicyService.PolicyInput(0L, "TRACE", 180, "DELETE", 5, false));

        assertThat(archive.scannedCount()).isEqualTo(12);
        assertThat(archive.archivedCount()).isEqualTo(10);
        assertThat(archive.skippedCount()).isEqualTo(2);
        assertThat(delete.deletedCount()).isEqualTo(5);
        assertThat(delete.skippedCount()).isEqualTo(3);
        verify(targets).archiveBatch(0L, "EVENT", 90, 10);
        verify(targets).deleteBatch(0L, "TRACE", 180, 5);
    }

    private RetentionPolicyService service() {
        return serviceWithTargets(
                mock(RetentionPolicyService.RetentionTargetRepository.class),
                mock(RetentionPolicyService.RunRepository.class));
    }

    private RetentionPolicyService serviceWithPolicies(RetentionPolicyService.PolicyRepository policies) {
        return new RetentionPolicyService(
                policies,
                mock(RetentionPolicyService.RetentionTargetRepository.class),
                mock(RetentionPolicyService.RunRepository.class),
                new RetentionPolicyService.Bounds(7, 730, 90, 1000));
    }

    private RetentionPolicyService serviceWithTargets(RetentionPolicyService.RetentionTargetRepository targets,
                                                     RetentionPolicyService.RunRepository runs) {
        return new RetentionPolicyService(
                mock(RetentionPolicyService.PolicyRepository.class),
                targets,
                runs,
                new RetentionPolicyService.Bounds(7, 730, 90, 1000));
    }

    private AnalyticsRetentionPolicyDO policy(Long tenantId,
                                              String recordKind,
                                              int retentionDays,
                                              String action,
                                              int maxBatchSize) {
        AnalyticsRetentionPolicyDO row = new AnalyticsRetentionPolicyDO();
        row.setTenantId(tenantId);
        row.setRecordKind(recordKind);
        row.setRetentionDays(retentionDays);
        row.setAction(action);
        row.setMaxBatchSize(maxBatchSize);
        row.setLegalHoldBehavior("SKIP");
        row.setEnabled(true);
        return row;
    }
}
