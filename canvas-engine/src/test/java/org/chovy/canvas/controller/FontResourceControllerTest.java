package org.chovy.canvas.web;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class FontResourceControllerTest {

    @Test
    void bundledFontFamilyReturnsFontResource() {
        FontResourceController controller = new FontResourceController();

        StepVerifier.create(controller.font("photonpay"))
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getHeaders().getContentType())
                            .isEqualTo(MediaType.parseMediaType("font/ttf"));
                    Resource body = response.getBody();
                    assertThat(body).isNotNull();
                    assertThat(body.exists()).isTrue();
                    assertThat(body.getFilename()).isEqualTo("OPPOSans-Regular.ttf");
                })
                .verifyComplete();
    }

    @Test
    void unknownFontFamilyReturnsNotFound() {
        FontResourceController controller = new FontResourceController();

        StepVerifier.create(controller.font("missing"))
                .assertNext(response -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND))
                .verifyComplete();
    }
}
