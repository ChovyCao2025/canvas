package org.chovy.canvas.web.marketing;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.TagImportFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;

class TagImportControllerCompatibilityTest {

    @Test
    void apiPushBatchesAndErrorsUseLegacyEnvelopeAndTenantHeader() {
        RecordingTagImportFacade facade = new RecordingTagImportFacade();
        WebTestClient client = webClient(facade);

        client.post()
                .uri("/canvas/tag-imports/api-push")
                .header("X-Tenant-Id", "42")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("rows", List.of(row("email", "user@example.test", "tier", "vip"))))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.batchId").isEqualTo(1001)
                .jsonPath("$.data.status").isEqualTo("SUCCESS")
                .jsonPath("$.data.totalRows").isEqualTo(1);

        assertThat(facade.lastTenantId).isEqualTo(42L);
        assertThat(facade.lastRows).hasSize(1);

        client.get()
                .uri("/canvas/tag-imports/batches")
                .header("X-Tenant-Id", "42")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].id").isEqualTo(2)
                .jsonPath("$.data[1].id").isEqualTo(1);

        client.get()
                .uri("/canvas/tag-imports/batches/9/errors")
                .header("X-Tenant-Id", "42")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].batchId").isEqualTo(9)
                .jsonPath("$.data[0].rowNo").isEqualTo(2);
    }

    @Test
    void excelTemplateReturnsAttachmentWithNonEmptyXlsxCompatibleBody() {
        byte[] body = webClient(new RecordingTagImportFacade())
                .get()
                .uri("/canvas/tag-imports/excel-template")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueMatches("Content-Disposition", ".*tag-import-template\\.xlsx.*")
                .expectHeader().contentTypeCompatibleWith(
                        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .expectBody(byte[].class)
                .returnResult()
                .getResponseBody();

        assertThat(body).isNotNull().isNotEmpty();
    }

    @Test
    void excelMultipartRouteForwardsFileNameAndBytesToFacade() {
        RecordingTagImportFacade facade = new RecordingTagImportFacade();
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", "idType,idValue,tagCode\nemail,user@example.test,tier\n"
                        .getBytes(StandardCharsets.UTF_8))
                .filename("tags.csv")
                .contentType(MediaType.APPLICATION_OCTET_STREAM);

        webClient(facade)
                .post()
                .uri("/canvas/tag-imports/excel")
                .header("X-Tenant-Id", "77")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(bodyBuilder.build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.status").isEqualTo("SUCCESS")
                .jsonPath("$.data.totalRows").isEqualTo(1);

        assertThat(facade.lastTenantId).isEqualTo(77L);
        assertThat(facade.lastFileName).isEqualTo("tags.csv");
        assertThat(new String(facade.lastBytes, StandardCharsets.UTF_8)).contains("user@example.test");
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingTagImportFacade facade = new RecordingTagImportFacade();
        facade.failApiPush = true;

        webClient(facade)
                .post()
                .uri("/canvas/tag-imports/api-push")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("rows", List.of()))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("rows are required")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(TagImportFacade facade) {
        return WebTestClient.bindToController(new TagImportController(facade)).build();
    }

    private static Map<String, Object> row(String idType, String idValue, String tagCode, String tagValue) {
        return Map.of("idType", idType, "idValue", idValue, "tagCode", tagCode, "tagValue", tagValue);
    }

    private static final class RecordingTagImportFacade implements TagImportFacade {
        private Long lastTenantId;
        private List<Map<String, Object>> lastRows = List.of();
        private String lastFileName;
        private byte[] lastBytes = new byte[0];
        private boolean failApiPush;

        @Override
        public Map<String, Object> apiPush(Long tenantId, List<Map<String, Object>> rows) {
            if (failApiPush) {
                throw new IllegalArgumentException("rows are required");
            }
            lastTenantId = tenantId;
            lastRows = new ArrayList<>(rows);
            return result(1001L, rows.size());
        }

        @Override
        public byte[] excelTemplate() {
            return "template".getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public Map<String, Object> importExcel(Long tenantId, String fileName, byte[] bytes) {
            lastTenantId = tenantId;
            lastFileName = fileName;
            lastBytes = bytes.clone();
            return result(1002L, 1);
        }

        @Override
        public List<Map<String, Object>> listBatches(Long tenantId) {
            lastTenantId = tenantId;
            return List.of(batch(2L), batch(1L));
        }

        @Override
        public List<Map<String, Object>> listErrors(Long tenantId, Long batchId) {
            lastTenantId = tenantId;
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("id", 1L);
            error.put("batchId", batchId);
            error.put("rowNo", 2);
            error.put("errorCode", "ROW_ERROR");
            error.put("errorMsg", "idValue is required");
            return List.of(error);
        }

        private static Map<String, Object> result(Long batchId, int rows) {
            return Map.of("batchId", batchId, "status", "SUCCESS", "totalRows", rows,
                    "successRows", rows, "failedRows", 0);
        }

        private static Map<String, Object> batch(Long id) {
            return Map.of("tenantId", 42L, "id", id, "sourceType", "API_PUSH", "status", "SUCCESS");
        }
    }
}
