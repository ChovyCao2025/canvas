package org.chovy.canvas.controller;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.content.ContentEntryService;
import org.chovy.canvas.domain.content.ContentTemplateService;
import org.chovy.canvas.domain.content.MarketingAssetService;
import org.chovy.canvas.domain.content.MarketingAssetUploadService;
import org.chovy.canvas.domain.content.MarketingContentReleaseService;
import org.chovy.canvas.web.MarketingContentController;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingContentUploadControllerTest {

    @Test
    void managementApiDoesNotExposeUnsignedUploadCallbacks() {
        boolean exposesUnsignedCallback = Arrays.stream(MarketingContentController.class.getDeclaredMethods())
                .flatMap(method -> Arrays.stream(method.getAnnotationsByType(PostMapping.class)))
                .flatMap(mapping -> Arrays.stream(mapping.value()))
                .anyMatch("/assets/upload-callbacks"::equals);

        assertThat(exposesUnsignedCallback).isFalse();
    }

    @Test
    void uploadIntentEndpointsUseAuthenticatedTenant() {
        MarketingAssetService assetService = mock(MarketingAssetService.class);
        ContentTemplateService templateService = mock(ContentTemplateService.class);
        ContentEntryService entryService = mock(ContentEntryService.class);
        MarketingAssetUploadService uploadService = mock(MarketingAssetUploadService.class);
        MarketingContentReleaseService releaseService = mock(MarketingContentReleaseService.class);
        TenantContextResolver resolver = resolver();
        MarketingAssetUploadService.UploadIntentCommand intentCommand =
                new MarketingAssetUploadService.UploadIntentCommand(
                        "hero-video", "VIDEO", "MUX", "video/mp4", "hero.mp4", 12_000L, "operator-1");
        when(uploadService.createIntent(operator(), intentCommand)).thenReturn(
                new MarketingAssetUploadService.UploadIntentView(
                        "mux-hero-video-upload", "hero-video", "VIDEO", "MUX", "upload-1",
                        "/provider/mux/direct-upload", Map.of("assetKey", "hero-video"),
                        "PENDING", null, null));
        MarketingAssetUploadService.UploadIntentCleanupCommand cleanupCommand =
                new MarketingAssetUploadService.UploadIntentCleanupCommand(50, "operator-1");
        when(uploadService.expireStalePendingUploads(operator(), cleanupCommand)).thenReturn(
                new MarketingAssetUploadService.UploadIntentCleanupResult(
                        2, 2, LocalDateTime.of(2026, 6, 6, 8, 0)));
        MarketingContentController controller = new MarketingContentController(
                assetService, templateService, entryService, uploadService, releaseService, resolver);

        StepVerifier.create(controller.createUploadIntent(intentCommand))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("PENDING"))
                .verifyComplete();
        StepVerifier.create(controller.expireStaleUploadIntents(cleanupCommand))
                .assertNext(response -> assertThat(response.getData().expired()).isEqualTo(2))
                .verifyComplete();

        verify(uploadService).createIntent(operator(), intentCommand);
        verify(uploadService).expireStalePendingUploads(operator(), cleanupCommand);
    }

    private TenantContextResolver resolver() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.just(operator()));
        return resolver;
    }

    private TenantContext operator() {
        return new TenantContext(8L, RoleNames.OPERATOR, "operator-1");
    }
}
