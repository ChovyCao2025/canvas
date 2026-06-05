package org.chovy.canvas.web;

import jakarta.validation.Validation;
import org.chovy.canvas.dto.CanvasCreateReq;
import org.chovy.canvas.dto.audience.AudiencePreviewReq;
import org.chovy.canvas.dto.cdp.CdpBatchTagReq;
import org.chovy.canvas.dto.datasource.DataSourceConfigReq;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiRequestValidationTest {

    private final jakarta.validation.Validator validator = Validation
            .buildDefaultValidatorFactory()
            .getValidator();

    @Test
    void canvasCreateRequiresName() {
        CanvasCreateReq req = new CanvasCreateReq();

        assertThat(validator.validate(req))
                .anySatisfy(v -> assertThat(v.getPropertyPath().toString()).isEqualTo("name"));
    }

    @Test
    void dataSourceRequestRequiresPasswordAndJdbcType() {
        DataSourceConfigReq req = new DataSourceConfigReq(
                null,
                "warehouse",
                "HTTP",
                "jdbc:mysql://localhost:3306/cdp",
                "cdp_app",
                "",
                null,
                null,
                1,
                null);

        assertThat(validator.validate(req))
                .extracting(v -> v.getPropertyPath().toString())
                .contains("password", "type");
    }

    @Test
    void audiencePreviewCapsSampleLimit() {
        AudiencePreviewReq req = new AudiencePreviewReq("CDP_TAG", "{\"logic\":\"AND\"}", 101);

        assertThat(validator.validate(req))
                .anySatisfy(v -> assertThat(v.getPropertyPath().toString()).isEqualTo("sampleLimit"));
    }

    @Test
    void batchTagRequiresUsers() {
        CdpBatchTagReq req = new CdpBatchTagReq("BATCH_SET", "vip", "true", List.of(), "reason", "admin");

        assertThat(validator.validate(req))
                .anySatisfy(v -> assertThat(v.getPropertyPath().toString()).isEqualTo("userIds"));
    }
}
