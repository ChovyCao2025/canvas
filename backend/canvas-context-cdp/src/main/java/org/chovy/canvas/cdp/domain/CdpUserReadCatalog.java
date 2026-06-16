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

/**
 * 维护 CdpUserRead 的内存目录和查询视图。
 */
public class CdpUserReadCatalog {

    /**
     * 执行 parse 对应的 CDP 业务操作。
     */
    private static final LocalDateTime FIRST_SEEN = LocalDateTime.parse("2026-06-01T10:00:00");

    /**
     * 执行 parse 对应的 CDP 业务操作。
     */
    private static final LocalDateTime LAST_SEEN = LocalDateTime.parse("2026-06-12T10:00:00");

    /**
     * 执行 of 对应的 CDP 业务操作。
     */
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
                    /**
                     * "FAILED"))。
                     */
                    "FAILED"));

    /**
     * 查询Users列表。
     */
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

    /**
     * 返回user。
     */
    public CdpUserProfileView getUser(Long tenantId, String userId) {
        String normalizedUserId = requireUserId(userId);
        return findUser(tenantId, normalizedUserId).profile();
    }

    /**
     * 返回insight。
     */
    public CdpUserInsightView getInsight(Long tenantId, String userId) {
        String normalizedUserId = requireUserId(userId);
        UserEntry user = findUser(tenantId, normalizedUserId);
        return new CdpUserInsightView(
                user.profile().userId(),
                user.profile(),
                user.tags(),
                user.canvasRows());
    }

    /**
     * 查找User。
     */
    private UserEntry findUser(Long tenantId, String userId) {
        return users.stream()
                .filter(user -> Objects.equals(user.tenantId(), tenantId))
                .filter(user -> user.profile().userId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("CDP user not found: " + userId));
    }

    /**
     * 转换为Row。
     */
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

    /**
     * 执行 matches 对应的 CDP 业务操作。
     */
    private static boolean matches(CdpUserProfileView profile, String keyword) {
        return contains(profile.userId(), keyword) || contains(profile.displayName(), keyword);
    }

    /**
     * 执行 contains 对应的 CDP 业务操作。
     */
    private static boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    /**
     * 归一化Keyword。
     */
    private static String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 读取并校验必填的User Id。
     */
    private static String requireUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        return userId.trim();
    }

    /**
     * 执行 tag 对应的 CDP 业务操作。
     */
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

    /**
     * 执行 canvas 对应的 CDP 业务操作。
     */
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

    /**
     * 表示 UserEntry 的业务数据或处理组件。
     */
    private static final class UserEntry {

        /**
         * 租户标识。
         */
        private final Long tenantId;

        /**
         * profile。
         */
        private final CdpUserProfileView profile;

        /**
         * tags。
         */
        private final List<CdpUserTagSummaryView> tags;

        /**
         * canvas Rows。
         */
        private final List<CdpUserCanvasSummaryView> canvasRows;

        /**
         * execution Count。
         */
        private final long executionCount;

        /**
         * success Count。
         */
        private final long successCount;

        /**
         * failed Count。
         */
        private final long failedCount;

        /**
         * latest Status。
         */
        private final String latestStatus;

        /**
         * 使用记录字段创建 UserEntry。
         */
        private UserEntry(
                Long tenantId,
                CdpUserProfileView profile,
                List<CdpUserTagSummaryView> tags,
                List<CdpUserCanvasSummaryView> canvasRows,
                long executionCount,
                long successCount,
                long failedCount,
                String latestStatus) {
            this.tenantId = tenantId;
            this.profile = profile;
            this.tags = tags;
            this.canvasRows = canvasRows;
            this.executionCount = executionCount;
            this.successCount = successCount;
            this.failedCount = failedCount;
            this.latestStatus = latestStatus;
        }

        /**
         * 返回租户标识。
         */
        public Long tenantId() {
            return tenantId;
        }

        /**
         * 返回profile。
         */
        public CdpUserProfileView profile() {
            return profile;
        }

        /**
         * 返回tags。
         */
        public List<CdpUserTagSummaryView> tags() {
            return tags;
        }

        /**
         * 返回canvas Rows。
         */
        public List<CdpUserCanvasSummaryView> canvasRows() {
            return canvasRows;
        }

        /**
         * 返回execution Count。
         */
        public long executionCount() {
            return executionCount;
        }

        /**
         * 返回success Count。
         */
        public long successCount() {
            return successCount;
        }

        /**
         * 返回failed Count。
         */
        public long failedCount() {
            return failedCount;
        }

        /**
         * 返回latest Status。
         */
        public String latestStatus() {
            return latestStatus;
        }

        /**
         * 按所有字段比较 UserEntry。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            UserEntry that = (UserEntry) o;
            return java.util.Objects.equals(tenantId, that.tenantId)
                    && java.util.Objects.equals(profile, that.profile)
                    && java.util.Objects.equals(tags, that.tags)
                    && java.util.Objects.equals(canvasRows, that.canvasRows)
                    && java.util.Objects.equals(executionCount, that.executionCount)
                    && java.util.Objects.equals(successCount, that.successCount)
                    && java.util.Objects.equals(failedCount, that.failedCount)
                    && java.util.Objects.equals(latestStatus, that.latestStatus);
        }

        /**
         * 根据所有字段计算 UserEntry 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(tenantId, profile, tags, canvasRows, executionCount, successCount, failedCount, latestStatus);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "UserEntry[" + "tenantId=" + tenantId + ", profile=" + profile + ", tags=" + tags + ", canvasRows=" + canvasRows + ", executionCount=" + executionCount + ", successCount=" + successCount + ", failedCount=" + failedCount + ", latestStatus=" + latestStatus + "]";
        }
    }
}
