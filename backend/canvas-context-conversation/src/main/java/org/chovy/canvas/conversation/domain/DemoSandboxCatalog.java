package org.chovy.canvas.conversation.domain;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.conversation.api.DemoSandboxFacade.ConversationReplyCommand;
import org.chovy.canvas.conversation.api.DemoSandboxFacade.ConversationReplyResult;
import org.chovy.canvas.conversation.api.DemoSandboxFacade.InstallCommand;
import org.chovy.canvas.conversation.api.DemoSandboxFacade.ResetResult;
import org.chovy.canvas.conversation.api.DemoSandboxFacade.SandboxView;

/**
 * 用内存结构维护演示沙箱安装状态和回复记录。
 */
public class DemoSandboxCatalog {

    /**
     * 演示沙箱使用的固定渠道标识。
     */
    private static final String SANDBOX = "SANDBOX";

    /**
     * 生成演示时间的时钟，测试可注入固定时钟。
     */
    private final Clock clock;

    /**
     * 按租户保存当前沙箱状态。
     */
    private final Map<Long, SandboxRow> sandboxes = new LinkedHashMap<>();

    /**
     * 按回复记录标识保存演示回复。
     */
    private final Map<Long, ConversationReplyResult> replies = new LinkedHashMap<>();

    /**
     * 内存沙箱记录自增序列。
     */
    private long sandboxIds;

    /**
     * 内存回复记录自增序列。
     */
    private long replyIds;

    /**
     * 使用指定时钟创建演示沙箱目录。
     *
     * @param clock 生成业务时间的时钟，null 时使用系统默认时区时钟
     */
    public DemoSandboxCatalog(Clock clock) {
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 安装或覆盖租户的演示沙箱。
     *
     * @param command 安装请求
     * @param actor 操作者
     * @return 安装后的沙箱视图
     */
    public synchronized SandboxView install(InstallCommand command, String actor) {
        if (command == null) {
            throw new IllegalArgumentException("install request is required");
        }
        Long tenantId = requireTenant(command.tenantId());
        LocalDateTime now = now();
        LocalDateTime expiresAt = now.plusDays(command.ttlDays());
        String status = command.ttlDays() <= 0 ? "EXPIRED" : "ACTIVE";
        SandboxRow row = new SandboxRow(++sandboxIds, tenantId, defaultText(command.demoName(), SANDBOX),
                command.ttlDays(), status, actorOrSystem(actor), now, expiresAt);
        // 租户维度只保留一个当前沙箱，重复安装会覆盖旧状态。
        sandboxes.put(tenantId, row);
        return view(row);
    }

    /**
     * 将租户沙箱标记为已重置。
     *
     * @param tenantId 租户标识
     * @param actor 操作者
     * @return 重置结果
     */
    public synchronized ResetResult reset(Long tenantId, String actor) {
        Long scopedTenantId = requireTenant(tenantId);
        SandboxRow row = sandboxes.get(scopedTenantId);
        if (row != null) {
            row.status = "RESET";
        }
        return new ResetResult(scopedTenantId, "RESET", actorOrSystem(actor), now());
    }

    /**
     * 查询当前已经过期的沙箱。
     *
     * @return 过期沙箱视图列表
     */
    public synchronized List<SandboxView> expired() {
        LocalDateTime now = now();
        return sandboxes.values().stream()
                .filter(row -> "EXPIRED".equals(row.status) || !row.expiresAt.isAfter(now))
                .map(DemoSandboxCatalog::view)
                .toList();
    }

    /**
     * 记录一次演示沙箱会话回复。
     *
     * @param tenantId 租户标识
     * @param command 回复请求
     * @param actor 操作者
     * @return 回复记录结果
     */
    public synchronized ConversationReplyResult reply(Long tenantId, ConversationReplyCommand command, String actor) {
        if (command == null) {
            throw new IllegalArgumentException("conversation reply request is required");
        }
        Long scopedTenantId = requireTenant(tenantId);
        String executionId = requireText(command.executionId(), "executionId");
        String userId = requireText(command.userId(), "userId");
        ConversationReplyResult result = new ConversationReplyResult(++replyIds, scopedTenantId, SANDBOX,
                command.canvasId(), command.versionId(), executionId, userId, optional(command.externalMessageId()),
                optional(command.eventId()), optional(command.text()), optional(command.intent()),
                command.attributes() == null ? Map.of() : Map.copyOf(command.attributes()), actorOrSystem(actor),
                now());
        replies.put(result.id(), result);
        return result;
    }

    /**
     * 返回当前业务时间。
     *
     * @return 当前本地时间
     */
    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /**
     * 校验租户标识必填。
     *
     * @param tenantId 租户标识
     * @return 已校验的租户标识
     */
    private static Long requireTenant(Long tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        return tenantId;
    }

    /**
     * 校验必填文本并去除首尾空白。
     *
     * @param value 待校验文本
     * @param field 字段名称
     * @return 已规范化文本
     */
    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    /**
     * 返回去空后的文本，空白时使用默认值。
     *
     * @param value 候选文本
     * @param fallback 默认文本
     * @return 规范化后的文本
     */
    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    /**
     * 将空白文本折叠为 null。
     *
     * @param value 候选文本
     * @return 规范化后的文本或 null
     */
    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * 返回操作者，空白时使用系统身份。
     *
     * @param actor 候选操作者
     * @return 规范化后的操作者
     */
    private static String actorOrSystem(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    /**
     * 将内部沙箱行转换为对外视图。
     *
     * @param row 内部沙箱行
     * @return 沙箱视图
     */
    private static SandboxView view(SandboxRow row) {
        return new SandboxView(row.id, row.tenantId, row.demoName, row.ttlDays, row.status, row.installedBy,
                row.installedAt, row.expiresAt);
    }

    /**
     * 演示沙箱的内部可变状态行。
     */
    private static final class SandboxRow {
        /**
         * 沙箱记录标识。
         */
        private final Long id;

        /**
         * 租户标识。
         */
        private final Long tenantId;

        /**
         * 演示名称。
         */
        private final String demoName;

        /**
         * 有效天数。
         */
        private final int ttlDays;

        /**
         * 当前沙箱状态。
         */
        private String status;

        /**
         * 安装操作者。
         */
        private final String installedBy;

        /**
         * 安装时间。
         */
        private final LocalDateTime installedAt;

        /**
         * 过期时间。
         */
        private final LocalDateTime expiresAt;

        /**
         * 创建内部沙箱状态行。
         *
         * @param id 沙箱记录标识
         * @param tenantId 租户标识
         * @param demoName 演示名称
         * @param ttlDays 有效天数
         * @param status 当前状态
         * @param installedBy 安装操作者
         * @param installedAt 安装时间
         * @param expiresAt 过期时间
         */
        private SandboxRow(Long id, Long tenantId, String demoName, int ttlDays, String status, String installedBy,
                LocalDateTime installedAt, LocalDateTime expiresAt) {
            this.id = id;
            this.tenantId = tenantId;
            this.demoName = demoName;
            this.ttlDays = ttlDays;
            this.status = status;
            this.installedBy = installedBy;
            this.installedAt = installedAt;
            this.expiresAt = expiresAt;
        }
    }
}
