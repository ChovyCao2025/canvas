package org.chovy.canvas.platform.application;

import org.chovy.canvas.platform.domain.PlatformWorkstream;
import org.chovy.canvas.platform.domain.PlatformWorkstreamRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 覆盖平台工作流应用服务的状态计算和子规格准入。
 */
class PlatformWorkstreamApplicationServiceTest {

    /**
     * 验证缺少必需子规格的工作流会被标记为阻塞。
     */
    @Test
    void listMarksWorkstreamsWithoutRequiredChildSpecsAsBlocked() {
        PlatformWorkstreamRepository repository = mock(PlatformWorkstreamRepository.class);
        PlatformWorkstreamApplicationService service = new PlatformWorkstreamApplicationService(repository);
        when(repository.list()).thenReturn(List.of(
                new PlatformWorkstream("platformization", "Platformization", "P2", true, null, "Extension points"),
                new PlatformWorkstream(
                        "data-assets",
                        "Data Assets",
                        "P2",
                        true,
                        "docs/product-evolution/specs/p2-016-analytics-event-trace-schema-and-sink.md",
                        "Event pipeline")));

        var statuses = service.statuses();

        assertThat(statuses)
                .extracting(status -> status.status())
                .containsExactly("BLOCKED_CHILD_SPEC_REQUIRED", "READY_FOR_CHILD_EXECUTION");
    }

    /**
     * 验证子规格准入会标准化工作流键并拒绝缺失路径。
     */
    @Test
    void requireExecutableChildSpecNormalizesKeyAndRejectsMissingSpecPath() {
        PlatformWorkstreamRepository repository = mock(PlatformWorkstreamRepository.class);
        PlatformWorkstreamApplicationService service = new PlatformWorkstreamApplicationService(repository);
        when(repository.get("channels")).thenReturn(
                new PlatformWorkstream("channels", "Channels", "P2", true, null, "Adapters"));

        assertThatThrownBy(() -> service.requireExecutableChildSpec(" Channels "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("channels requires a child spec before implementation");
    }

    /**
     * 验证未知或非法工作流键会被拒绝。
     */
    @Test
    void requireExecutableChildSpecRejectsUnknownOrInvalidKeys() {
        PlatformWorkstreamRepository repository = mock(PlatformWorkstreamRepository.class);
        PlatformWorkstreamApplicationService service = new PlatformWorkstreamApplicationService(repository);

        assertThatThrownBy(() -> service.requireExecutableChildSpec("bad key"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid workstream key");
        assertThatThrownBy(() -> service.requireExecutableChildSpec("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown workstream missing");
    }
}
