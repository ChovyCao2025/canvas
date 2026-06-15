package org.chovy.canvas.cdp.domain;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.chovy.canvas.cdp.api.CdpUserReadFacade.CdpUserCanvasSummaryView;
import org.chovy.canvas.cdp.api.CdpUserReadFacade.CdpUserInsightView;
import org.chovy.canvas.cdp.api.CdpUserReadFacade.CdpUserProfileView;
import org.chovy.canvas.cdp.api.CdpUserReadFacade.CdpUserRowView;
import org.chovy.canvas.cdp.api.CdpUserReadFacade.CdpUserTagSummaryView;

public class CdpUserReadCatalog {

    private static final LocalDateTime FIRST_SEEN = LocalDateTime.parse("2026-06-01T10:00:00");
    private static final LocalDateTime LAST_SEEN = LocalDateTime.parse("2026-06-12T10:00:00");

    private final List<UserEntry> users = List.of(
            new UserEntry(
                    7L,
                    new CdpUserProfileView(
                            "user-alice",
                            "Alice Chen",
                            "138****0001",
                            "alice@example.com",
                            "ACTIVE",
                            "{\"tier\":\"gold\"}",
                            FIRST_SEEN,
                            LAST_SEEN),
                    List.of(tag("vip", "VIP", "gold")),
                    List.of(canvas(100L, "Welcome Journey", 5, 4, 1, "SUCCESS")),
                    5,
                    4,
                    1,
                    "SUCCESS"),
            new UserEntry(
                    7L,
                    new CdpUserProfileView(
                            "user-bob",
                            "Bob Li",
                            "139****0002",
                            "bob@example.com",
                            "ACTIVE",
                            "{\"tier\":\"silver\"}",
                            FIRST_SEEN.minusDays(2),
                            LAST_SEEN.minusDays(1)),
                    List.of(tag("lifecycle", "Lifecycle", "new")),
                    List.of(canvas(101L, "Retention Journey", 2, 1, 1, "FAILED")),
                    2,
                    1,
                    1,
                    "FAILED"));

    public List<CdpUserRowView> listUsers(Long tenantId, String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);
        return users.stream()
                .filter(user -> Objects.equals(user.tenantId(), tenantId))
                .filter(user -> normalizedKeyword == null || matches(user.profile(), normalizedKeyword))
                .sorted(Comparator.comparing((UserEntry user) -> user.profile().lastSeenAt()).reversed()
                        .thenComparing(user -> user.profile().userId(), Comparator.reverseOrder()))
                .map(this::toRow)
                .toList();
    }

    public CdpUserProfileView getUser(Long tenantId, String userId) {
        String normalizedUserId = requireUserId(userId);
        return findUser(tenantId, normalizedUserId).profile();
    }

    public CdpUserInsightView getInsight(Long tenantId, String userId) {
        String normalizedUserId = requireUserId(userId);
        UserEntry user = findUser(tenantId, normalizedUserId);
        return new CdpUserInsightView(
                user.profile().userId(),
                user.profile(),
                user.tags(),
                user.canvasRows());
    }

    private UserEntry findUser(Long tenantId, String userId) {
        return users.stream()
                .filter(user -> Objects.equals(user.tenantId(), tenantId))
                .filter(user -> user.profile().userId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("CDP user not found: " + userId));
    }

    private CdpUserRowView toRow(UserEntry user) {
        CdpUserProfileView profile = user.profile();
        String displayName = profile.displayName() == null || profile.displayName().isBlank()
                ? profile.userId()
                : profile.displayName();
        return new CdpUserRowView(
                profile.userId(),
                displayName,
                user.executionCount(),
                user.successCount(),
                user.failedCount(),
                user.latestStatus() == null || user.latestStatus().isBlank() ? "-" : user.latestStatus(),
                user.canvasRows().isEmpty() ? null : user.canvasRows().getFirst().firstEnteredAt(),
                profile.lastSeenAt(),
                user.tags());
    }

    private static boolean matches(CdpUserProfileView profile, String keyword) {
        return contains(profile.userId(), keyword) || contains(profile.displayName(), keyword);
    }

    private static boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private static String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim().toLowerCase(Locale.ROOT);
    }

    private static String requireUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        return userId.trim();
    }

    private static CdpUserTagSummaryView tag(String tagCode, String tagName, String tagValue) {
        return new CdpUserTagSummaryView(
                tagCode,
                tagName,
                tagValue,
                "STRING",
                "MANUAL",
                "ACTIVE",
                LocalDateTime.parse("2026-06-02T10:00:00"),
                null,
                LAST_SEEN);
    }

    private static CdpUserCanvasSummaryView canvas(
            Long canvasId,
            String canvasName,
            long executionCount,
            long successCount,
            long failedCount,
            String latestStatus) {
        String finalCanvasName = canvasName == null || canvasName.isBlank() ? "画布#" + canvasId : canvasName;
        return new CdpUserCanvasSummaryView(
                canvasId,
                finalCanvasName,
                executionCount,
                successCount,
                failedCount,
                latestStatus == null || latestStatus.isBlank() ? "-" : latestStatus,
                LocalDateTime.parse("2026-06-03T10:00:00"),
                LAST_SEEN);
    }

    private record UserEntry(
            Long tenantId,
            CdpUserProfileView profile,
            List<CdpUserTagSummaryView> tags,
            List<CdpUserCanvasSummaryView> canvasRows,
            long executionCount,
            long successCount,
            long failedCount,
            String latestStatus) {
    }
}
