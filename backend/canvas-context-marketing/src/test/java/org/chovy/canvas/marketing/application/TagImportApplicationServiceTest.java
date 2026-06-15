package org.chovy.canvas.marketing.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.TagImportFacade;
import org.junit.jupiter.api.Test;

class TagImportApplicationServiceTest {

    @Test
    void apiPushCreatesTenantScopedBatchAndCountsRowValidationResults() {
        TagImportFacade service = new TagImportApplicationService();

        Map<String, Object> result = service.apiPush(42L, List.of(
                row("email", "user@example.test", "tier", "vip"),
                row("email", "", "tier", "vip"),
                row("mobile", "13800000000", "tier", "gold")));

        assertThat(result)
                .containsEntry("batchId", 1L)
                .containsEntry("status", "PARTIAL_SUCCESS")
                .containsEntry("totalRows", 3)
                .containsEntry("successRows", 2)
                .containsEntry("failedRows", 1);
        assertThat(service.listBatches(42L))
                .singleElement()
                .satisfies(batch -> assertThat(batch)
                        .containsEntry("tenantId", 42L)
                        .containsEntry("id", 1L)
                        .containsEntry("sourceType", "API_PUSH")
                        .containsEntry("status", "PARTIAL_SUCCESS"));
        assertThat(service.listErrors(42L, 1L))
                .singleElement()
                .satisfies(error -> assertThat(error)
                        .containsEntry("rowNo", 2)
                        .containsEntry("errorCode", "ROW_ERROR"));
        assertThat(service.listBatches(7L)).hasSize(2);
    }

    @Test
    void excelImportAndTemplateKeepLegacyResultAndDownloadContracts() {
        TagImportFacade service = new TagImportApplicationService();

        byte[] template = service.excelTemplate();
        Map<String, Object> result = service.importExcel(7L, "tags.xlsx",
                "idType,idValue,tagCode,tagValue\nemail,user@example.test,tier,vip\n"
                        .getBytes(StandardCharsets.UTF_8));

        assertThat(new String(template, StandardCharsets.UTF_8))
                .contains("idType", "idValue", "tagCode", "tagValue", "tagTime");
        assertThat(result)
                .containsEntry("status", "SUCCESS")
                .containsEntry("totalRows", 1)
                .containsEntry("successRows", 1)
                .containsEntry("failedRows", 0);
        assertThat(service.listBatches(7L))
                .extracting(batch -> batch.get("id"))
                .containsExactly(7003L, 7002L, 7001L);
    }

    @Test
    void batchErrorsAreSortedByRowNoThenIdAndTenantScoped() {
        TagImportFacade service = new TagImportApplicationService();

        Map<String, Object> result = service.apiPush(9L, List.of(
                row("", "missing-type@example.test", "tier", "vip"),
                row("email", "", "tier", "vip"),
                row("email", "missing-tag@example.test", "", "vip")));

        assertThat(result)
                .containsEntry("status", "FAILED")
                .containsEntry("failedRows", 3);
        assertThat(service.listErrors(9L, 1L))
                .extracting(error -> error.get("rowNo"))
                .containsExactly(1, 2, 3);
        assertThat(service.listErrors(42L, 1L)).isEmpty();
    }

    private static Map<String, Object> row(String idType, String idValue, String tagCode, String tagValue) {
        return Map.of("idType", idType, "idValue", idValue, "tagCode", tagCode, "tagValue", tagValue);
    }
}
