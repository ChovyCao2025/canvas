package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.collaboration.CanvasCollaborationSummaryService;
import org.chovy.canvas.domain.collaboration.UserWorkspacePreferenceService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasControllerCollaborationTest {

    @Test
    void summaryUsesCurrentTenantAndCanvasId() {
        CanvasCollaborationSummaryService summaries = mock(CanvasCollaborationSummaryService.class);
        UserWorkspacePreferenceService preferences = mock(UserWorkspacePreferenceService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.just(operator()));
        when(summaries.summary(8L, 42L)).thenReturn(new CanvasCollaborationSummaryService.Summary(
                42L,
                List.of(new CanvasCollaborationSummaryService.Presence("operator-1", "Alice", "editing")),
                2,
                3,
                1));

        CanvasCollaborationController controller = new CanvasCollaborationController(summaries, preferences, resolver);

        StepVerifier.create(controller.summary(42L))
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo(0);
                    assertThat(response.getData().canvasId()).isEqualTo(42L);
                    assertThat(response.getData().presence()).hasSize(1);
                    assertThat(response.getData().openCommentCount()).isEqualTo(3);
                })
                .verifyComplete();

        verify(summaries).summary(8L, 42L);
    }

    @Test
    void upsertPreferenceUsesCurrentTenantAndUsername() {
        CanvasCollaborationSummaryService summaries = mock(CanvasCollaborationSummaryService.class);
        UserWorkspacePreferenceService preferences = mock(UserWorkspacePreferenceService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.just(operator()));
        when(preferences.upsertEditorPreference(8L, "operator-1", Map.of("theme", "dark")))
                .thenReturn(new UserWorkspacePreferenceService.Preference("canvas-editor", Map.of("theme", "dark")));

        CanvasCollaborationController controller = new CanvasCollaborationController(summaries, preferences, resolver);

        StepVerifier.create(controller.upsertEditorPreference(Map.of("theme", "dark")))
                .assertNext(response -> assertThat(response.getData().preferenceJson()).containsEntry("theme", "dark"))
                .verifyComplete();

        verify(preferences).upsertEditorPreference(8L, "operator-1", Map.of("theme", "dark"));
    }

    @Test
    void missingTenantContextRejectsRead() {
        CanvasCollaborationSummaryService summaries = mock(CanvasCollaborationSummaryService.class);
        UserWorkspacePreferenceService preferences = mock(UserWorkspacePreferenceService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.error(new SecurityException("AUTH_003: missing tenant context")));

        CanvasCollaborationController controller = new CanvasCollaborationController(summaries, preferences, resolver);

        StepVerifier.create(controller.editorPreference())
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(SecurityException.class)
                        .hasMessageContaining("AUTH_003"))
                .verify();
    }

    private TenantContext operator() {
        return new TenantContext(8L, RoleNames.OPERATOR, "operator-1");
    }
}
