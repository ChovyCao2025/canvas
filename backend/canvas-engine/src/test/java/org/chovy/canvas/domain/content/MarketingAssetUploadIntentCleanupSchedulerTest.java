package org.chovy.canvas.domain.content;

import org.chovy.canvas.common.tenant.RoleNames;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingAssetUploadIntentCleanupSchedulerTest {

    @Test
    void disabledSchedulerDoesNotMutateUploadIntents() {
        MarketingAssetUploadService uploadService = mock(MarketingAssetUploadService.class);
        MarketingAssetUploadIntentCleanupScheduler scheduler =
                new MarketingAssetUploadIntentCleanupScheduler(uploadService, false, 8L, 100, "cleanup");

        MarketingAssetUploadService.UploadIntentCleanupResult result = scheduler.runScheduledOnce();

        assertThat(result.expired()).isZero();
        verify(uploadService, never()).expireStalePendingUploads(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void enabledSchedulerRunsTenantScopedCleanup() {
        MarketingAssetUploadService uploadService = mock(MarketingAssetUploadService.class);
        when(uploadService.expireStalePendingUploads(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new MarketingAssetUploadService.UploadIntentCleanupResult(
                        2, 2, LocalDateTime.of(2026, 6, 6, 8, 0)));
        MarketingAssetUploadIntentCleanupScheduler scheduler =
                new MarketingAssetUploadIntentCleanupScheduler(uploadService, true, 8L, 25, "cleanup");

        MarketingAssetUploadService.UploadIntentCleanupResult result = scheduler.runScheduledOnce();

        assertThat(result.expired()).isEqualTo(2);
        verify(uploadService).expireStalePendingUploads(
                argThat(tenant -> tenant.tenantId().equals(8L)
                        && RoleNames.TENANT_ADMIN.equals(tenant.role())
                        && "cleanup".equals(tenant.username())),
                argThat(command -> command.limit().equals(25) && command.actor().equals("cleanup")));
    }
}
