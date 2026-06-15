package org.chovy.canvas.marketing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.PaidMediaFacade;
import org.junit.jupiter.api.Test;

class PaidMediaApplicationServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-14T03:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    @Test
    void upsertsDestinationByProviderAndDestinationKeyWithinTenant() {
        PaidMediaFacade service = new PaidMediaApplicationService(CLOCK);

        PaidMediaFacade.DestinationView first = service.upsertDestination(7L, new PaidMediaFacade.DestinationCommand(
                "meta",
                "vip-buyers",
                "VIP Buyers",
                "acct-1",
                "aud-1",
                List.of("email", "phone", "email"),
                "paid_media",
                null,
                null,
                Map.of("pixelId", "px-1")), "planner");
        PaidMediaFacade.DestinationView updated = service.upsertDestination(7L, new PaidMediaFacade.DestinationCommand(
                "META",
                "vip-buyers",
                "VIP Buyers 2026",
                "acct-2",
                "aud-2",
                List.of("phone"),
                "ads",
                false,
                false,
                Map.of("pixelId", "px-2")), "operator");

        assertThat(first.id()).isEqualTo(updated.id());
        assertThat(updated).returns(7L, PaidMediaFacade.DestinationView::tenantId)
                .returns("META", PaidMediaFacade.DestinationView::provider)
                .returns("vip-buyers", PaidMediaFacade.DestinationView::destinationKey)
                .returns("VIP Buyers 2026", PaidMediaFacade.DestinationView::displayName)
                .returns("acct-2", PaidMediaFacade.DestinationView::accountId)
                .returns("aud-2", PaidMediaFacade.DestinationView::externalAudienceId)
                .returns("ADS", PaidMediaFacade.DestinationView::consentChannel)
                .returns(false, PaidMediaFacade.DestinationView::enforceConsent)
                .returns(false, PaidMediaFacade.DestinationView::enabled);
        assertThat(updated.identifierTypes()).containsExactly("PHONE");
        assertThat(updated.metadata()).containsEntry("pixelId", "px-2");
        assertThat(updated.createdBy()).isEqualTo("planner");
    }

    @Test
    void syncAudienceDeduplicatesUsersAppliesConsentAndFiltersRunsAndMembers() {
        PaidMediaFacade service = new PaidMediaApplicationService(CLOCK);
        PaidMediaFacade.DestinationView destination = service.upsertDestination(7L,
                new PaidMediaFacade.DestinationCommand("google", "lookalike", null, null, null, List.of("email"),
                        "paid_media", true, true, Map.of()), "operator");
        service.registerAudience(7L, 99L, true);
        service.registerProfile(7L, "u-1", "BUYER@EXAMPLE.COM", null);
        service.registerProfile(7L, "u-2", "skip@example.com", null);
        service.grantConsent(7L, "u-1", "PAID_MEDIA");

        PaidMediaFacade.SyncRunView run = service.syncAudience(7L, new PaidMediaFacade.SyncCommand(
                destination.id(),
                99L,
                List.of("u-1", "u-2", "u-1", "missing", " "),
                "op-1",
                Map.of("batch", "june")), "syncer");

        assertThat(run).returns("SUCCESS", PaidMediaFacade.SyncRunView::status)
                .returns(3, PaidMediaFacade.SyncRunView::requestedCount)
                .returns(1, PaidMediaFacade.SyncRunView::eligibleCount)
                .returns(2, PaidMediaFacade.SyncRunView::skippedCount)
                .returns(0, PaidMediaFacade.SyncRunView::failedCount)
                .returns("op-1", PaidMediaFacade.SyncRunView::externalOperationId)
                .returns("syncer", PaidMediaFacade.SyncRunView::createdBy);
        assertThat(run.metadata()).containsEntry("batch", "june");

        assertThat(service.runs(7L, new PaidMediaFacade.RunQuery(destination.id(), 99L, "success", 5)))
                .singleElement()
                .satisfies(row -> assertThat(row.id()).isEqualTo(run.id()));
        assertThat(service.runs(8L, new PaidMediaFacade.RunQuery(null, null, null, 5))).isEmpty();

        List<PaidMediaFacade.MemberView> eligible = service.members(7L,
                new PaidMediaFacade.MemberQuery(run.id(), "eligible", 5));
        assertThat(eligible).singleElement()
                .satisfies(row -> assertThat(row.userId()).isEqualTo("u-1"));

        List<PaidMediaFacade.MemberView> skipped = service.members(7L,
                new PaidMediaFacade.MemberQuery(run.id(), "skipped", 1));
        assertThat(skipped).hasSize(1);
        assertThat(skipped.get(0).reason()).isIn("CONSENT_DENIED", "PROFILE_NOT_FOUND");
    }

    @Test
    void validationDefaultsAndLimitBoundsFollowLegacyCompatibility() {
        PaidMediaFacade service = new PaidMediaApplicationService(CLOCK);
        PaidMediaFacade.DestinationView destination = service.upsertDestination(null,
                new PaidMediaFacade.DestinationCommand("meta", "defaulted", null, null, null, List.of(), null,
                        null, null, null), "");

        assertThat(destination).returns(0L, PaidMediaFacade.DestinationView::tenantId)
                .returns("defaulted", PaidMediaFacade.DestinationView::displayName)
                .returns("PAID_MEDIA", PaidMediaFacade.DestinationView::consentChannel)
                .returns(true, PaidMediaFacade.DestinationView::enforceConsent)
                .returns(true, PaidMediaFacade.DestinationView::enabled)
                .returns("system", PaidMediaFacade.DestinationView::createdBy);
        assertThat(destination.identifierTypes()).containsExactly("EMAIL", "PHONE");

        assertThatThrownBy(() -> service.upsertDestination(7L,
                new PaidMediaFacade.DestinationCommand(" ", "missing-provider", null, null, null, List.of(),
                        null, null, null, Map.of()), "operator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("provider is required");
        assertThatThrownBy(() -> service.syncAudience(7L,
                new PaidMediaFacade.SyncCommand(404L, 1L, List.of("u-1"), null, Map.of()), "operator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("paid-media destination is not found");

        for (int i = 0; i < 3; i++) {
            service.registerAudience(0L, (long) i, true);
            service.syncAudience(0L, new PaidMediaFacade.SyncCommand(destination.id(), (long) i,
                    List.of(), "op-" + i, Map.of()), "operator");
        }
        assertThat(service.runs(0L, new PaidMediaFacade.RunQuery(null, null, null, 0))).hasSize(1);
        assertThat(service.runs(0L, new PaidMediaFacade.RunQuery(null, null, null, 200))).hasSize(3);
    }
}
