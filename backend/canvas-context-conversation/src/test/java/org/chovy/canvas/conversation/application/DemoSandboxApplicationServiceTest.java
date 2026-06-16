package org.chovy.canvas.conversation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.conversation.api.DemoSandboxFacade;
import org.junit.jupiter.api.Test;

/**
 * 验证演示沙箱应用服务的单元测试。
 */
class DemoSandboxApplicationServiceTest {

    /**
     * 固定演示沙箱测试时钟。
     */
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-14T05:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    /**
     * 验证沙箱安装、重置和过期查询按租户隔离。
     */
    @Test
    void installsResetsAndFindsExpiredSandboxesByTenant() {
        DemoSandboxFacade service = new DemoSandboxApplicationService(CLOCK);

        DemoSandboxFacade.SandboxView active = service.install(
                new DemoSandboxFacade.InstallCommand(7L, " Welcome Demo ", 3), "installer");
        DemoSandboxFacade.SandboxView expired = service.install(
                new DemoSandboxFacade.InstallCommand(8L, "Expired Demo", 0), "installer");
        DemoSandboxFacade.ResetResult reset = service.reset(7L, "operator-1");

        assertThat(active).returns(7L, DemoSandboxFacade.SandboxView::tenantId)
                .returns("Welcome Demo", DemoSandboxFacade.SandboxView::demoName)
                .returns("ACTIVE", DemoSandboxFacade.SandboxView::status)
                .returns("installer", DemoSandboxFacade.SandboxView::installedBy);
        assertThat(expired.status()).isEqualTo("EXPIRED");
        assertThat(reset).returns(7L, DemoSandboxFacade.ResetResult::tenantId)
                .returns("RESET", DemoSandboxFacade.ResetResult::status)
                .returns("operator-1", DemoSandboxFacade.ResetResult::resetBy);
        assertThat(service.expired()).extracting(DemoSandboxFacade.SandboxView::tenantId)
                .containsExactly(8L);
    }

    /**
     * 验证演示会话回复保留沙箱渠道和原始载荷。
     */
    @Test
    void recordsConversationRepliesWithSandboxChannelAndPayload() {
        DemoSandboxFacade service = new DemoSandboxApplicationService(CLOCK);

        DemoSandboxFacade.ConversationReplyResult reply = service.reply(7L,
                new DemoSandboxFacade.ConversationReplyCommand(1L, 2L, "exec-1", "user-1",
                        "msg-1", "event-1", "hello", "greeting", Map.of("locale", "en-US")),
                "agent-1");

        assertThat(reply).returns(7L, DemoSandboxFacade.ConversationReplyResult::tenantId)
                .returns("SANDBOX", DemoSandboxFacade.ConversationReplyResult::channel)
                .returns("exec-1", DemoSandboxFacade.ConversationReplyResult::executionId)
                .returns("user-1", DemoSandboxFacade.ConversationReplyResult::userId)
                .returns("hello", DemoSandboxFacade.ConversationReplyResult::text)
                .returns("agent-1", DemoSandboxFacade.ConversationReplyResult::createdBy);
        assertThat(reply.attributes()).containsEntry("locale", "en-US");
    }

    /**
     * 验证沙箱默认值和参数校验保持兼容行为。
     */
    @Test
    void validationAndDefaultsFollowLegacyCompatibility() {
        DemoSandboxFacade service = new DemoSandboxApplicationService(CLOCK);

        DemoSandboxFacade.SandboxView sandbox = service.install(
                new DemoSandboxFacade.InstallCommand(9L, null, -5), "");

        assertThat(sandbox).returns("SANDBOX", DemoSandboxFacade.SandboxView::demoName)
                .returns("EXPIRED", DemoSandboxFacade.SandboxView::status)
                .returns("system", DemoSandboxFacade.SandboxView::installedBy);

        assertThatThrownBy(() -> service.install(null, "operator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("install request is required");
        assertThatThrownBy(() -> service.reset(null, "operator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId is required");
        assertThatThrownBy(() -> service.reply(7L, null, "operator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("conversation reply request is required");
        assertThatThrownBy(() -> service.reply(7L,
                new DemoSandboxFacade.ConversationReplyCommand(null, null, " ", "user-1",
                        null, null, null, null, Map.of()), "operator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("executionId is required");
    }
}
