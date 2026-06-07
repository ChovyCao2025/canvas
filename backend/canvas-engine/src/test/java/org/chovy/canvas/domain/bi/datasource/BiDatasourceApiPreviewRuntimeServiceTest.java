package org.chovy.canvas.domain.bi.datasource;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.BiDatasourceSchemaSnapshotDO;
import org.chovy.canvas.dal.dataobject.DataSourceConfigDO;
import org.chovy.canvas.dal.mapper.BiDatasourceSchemaSnapshotMapper;
import org.chovy.canvas.dal.mapper.DataSourceConfigMapper;
import org.chovy.canvas.domain.datasource.DataSourceCredentialCipher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiDatasourceApiPreviewRuntimeServiceTest {

    @Test
    void previewsApiDatasourceRowsByExecutingConfiguredHttpJsonRequest() {
        DataSourceConfigMapper mapper = mock(DataSourceConfigMapper.class);
        DataSourceCredentialCipher cipher = new DataSourceCredentialCipher(DataSourceCredentialCipher.DEFAULT_SECRET);
        DataSourceConfigDO source = apiSource(cipher.encrypt("bearer-token"));
        when(mapper.selectById(81L)).thenReturn(source);
        AtomicReference<BiDatasourceRuntimeService.ApiHttpRequest> requestSeen = new AtomicReference<>();
        BiDatasourceRuntimeService service = new BiDatasourceRuntimeService(
                mapper,
                null,
                cipher,
                httpRequest -> {
                    requestSeen.set(httpRequest);
                    return new BiDatasourceRuntimeService.ApiHttpResponse(
                            200,
                            "OK",
                            "{\"data\":{\"items\":[{\"order_id\":\"O-1\",\"amount\":12.5,\"paid\":true},{\"order_id\":\"O-2\",\"amount\":7,\"paid\":false}]}}");
                });

        BiDatasourceApiPreview preview = service.previewApiData(81L, 7L, new BiDatasourceApiPreviewRequest(
                Map.of(
                        "tenantId", "t-7",
                        "PageIndex", "2",
                        "campaignId", "cmp-9"),
                50));

        assertThat(requestSeen.get().method()).isEqualTo("POST");
        assertThat(requestSeen.get().url()).isEqualTo("https://api.example.com/orders?source=canvas&page=2");
        assertThat(requestSeen.get().headers()).containsEntry("Authorization", "Bearer bearer-token");
        assertThat(requestSeen.get().headers()).containsEntry("X-Tenant", "t-7");
        assertThat(requestSeen.get().body()).isEqualTo("{\"campaign\":\"cmp-9\"}");
        assertThat(preview.sourceKey()).isEqualTo("api-81");
        assertThat(preview.connectorType()).isEqualTo("API");
        assertThat(preview.columns()).extracting("key").containsExactly("order_id", "amount", "paid");
        assertThat(preview.columns()).extracting("dataType").containsExactly("STRING", "NUMBER", "BOOLEAN");
        assertThat(preview.rows()).hasSize(2);
        assertThat(preview.rows().get(0)).containsEntry("order_id", "O-1").containsEntry("amount", 12.5);
        assertThat(preview.rowCount()).isEqualTo(2);
        assertThat(preview.truncated()).isFalse();
    }

    @Test
    void enforcesQuickBiApiPreviewRowAndColumnLimits() {
        DataSourceConfigMapper mapper = mock(DataSourceConfigMapper.class);
        DataSourceCredentialCipher cipher = new DataSourceCredentialCipher(DataSourceCredentialCipher.DEFAULT_SECRET);
        DataSourceConfigDO source = apiSource(cipher.encrypt("bearer-token"));
        source.setConnectorConfigJson("""
                {"requestMethod":"GET","authType":"NONE","headers":[],"parameters":[],"responseRowsPath":"$","responseFormat":"JSON"}
                """);
        when(mapper.selectById(81L)).thenReturn(source);
        BiDatasourceRuntimeService service = new BiDatasourceRuntimeService(
                mapper,
                null,
                cipher,
                httpRequest -> new BiDatasourceRuntimeService.ApiHttpResponse(200, "OK", largeJsonArray(1002, 101)));

        BiDatasourceApiPreview preview = service.previewApiData(81L, 7L, new BiDatasourceApiPreviewRequest(Map.of(), 2000));

        assertThat(preview.rows()).hasSize(1000);
        assertThat(preview.columns()).hasSize(100);
        assertThat(preview.rowCount()).isEqualTo(1000);
        assertThat(preview.truncated()).isTrue();
    }

    @Test
    void syncsApiSchemaUsingRuntimeVariablesAndPersistsInferredApiResponseTable() {
        DataSourceConfigMapper mapper = mock(DataSourceConfigMapper.class);
        BiDatasourceSchemaSnapshotMapper snapshotMapper = mock(BiDatasourceSchemaSnapshotMapper.class);
        DataSourceCredentialCipher cipher = new DataSourceCredentialCipher(DataSourceCredentialCipher.DEFAULT_SECRET);
        DataSourceConfigDO source = apiSource(cipher.encrypt("bearer-token"));
        when(mapper.selectById(81L)).thenReturn(source);
        AtomicReference<BiDatasourceRuntimeService.ApiHttpRequest> requestSeen = new AtomicReference<>();
        BiDatasourceRuntimeService service = new BiDatasourceRuntimeService(
                mapper,
                snapshotMapper,
                null,
                cipher,
                new ObjectMapper(),
                (jdbcSource, password) -> null,
                httpRequest -> {
                    requestSeen.set(httpRequest);
                    return new BiDatasourceRuntimeService.ApiHttpResponse(
                            200,
                            "OK",
                            "{\"data\":{\"items\":[{\"order_id\":\"O-1\",\"amount\":12.5,\"paid\":true}]}}");
                });

        BiDatasourceSchemaSnapshotView snapshot = service.syncSchema(
                81L,
                7L,
                "alice",
                100,
                new BiDatasourceApiPreviewRequest(
                        Map.of(
                                "tenantId", "t-7",
                                "PageIndex", "3",
                                "campaignId", "cmp-1"),
                        20));

        assertThat(requestSeen.get().url()).isEqualTo("https://api.example.com/orders?source=canvas&page=3");
        assertThat(requestSeen.get().headers()).containsEntry("X-Tenant", "t-7");
        assertThat(requestSeen.get().body()).isEqualTo("{\"campaign\":\"cmp-1\"}");
        assertThat(snapshot.sourceKey()).isEqualTo("api-81");
        assertThat(snapshot.connectorType()).isEqualTo("API");
        assertThat(snapshot.tables()).singleElement().satisfies(table -> {
            assertThat(table.name()).isEqualTo("api_response");
            assertThat(table.tableType()).isEqualTo("HTTP_JSON");
            assertThat(table.columns()).extracting("name").containsExactly("order_id", "amount", "paid");
        });
        ArgumentCaptor<BiDatasourceSchemaSnapshotDO> row = ArgumentCaptor.forClass(BiDatasourceSchemaSnapshotDO.class);
        verify(snapshotMapper).insert(row.capture());
        assertThat(row.getValue().getTenantId()).isEqualTo(7L);
        assertThat(row.getValue().getSourceKey()).isEqualTo("api-81");
        assertThat(row.getValue().getConnectorType()).isEqualTo("API");
        assertThat(row.getValue().getSchemaJson()).contains("api_response").contains("order_id").contains("amount");
    }

    private static DataSourceConfigDO apiSource(String encryptedPassword) {
        DataSourceConfigDO source = new DataSourceConfigDO();
        source.setId(81L);
        source.setTenantId(7L);
        source.setName("Orders API");
        source.setType("API");
        source.setConnectorType("API");
        source.setConnectionMode("EXTRACT");
        source.setDriverClassName("HTTP_JSON");
        source.setUrl("https://api.example.com/orders?source=canvas");
        source.setUsername("Authorization");
        source.setPassword(encryptedPassword);
        source.setConnectorConfigJson("""
                {"requestMethod":"POST","authType":"BEARER","headers":[{"name":"X-Tenant","value":"{{tenantId}}","variable":true}],"parameters":[{"name":"page","value":"${PageIndex}","variable":true}],"bodyTemplate":"{\\"campaign\\":\\"{{campaignId}}\\"}","responseRowsPath":"$.data.items","responseFormat":"JSON"}
                """);
        return source;
    }

    private static String largeJsonArray(int rows, int columns) {
        StringBuilder builder = new StringBuilder("[");
        for (int row = 0; row < rows; row++) {
            if (row > 0) builder.append(',');
            Map<String, Object> value = new LinkedHashMap<>();
            int rowIndex = row;
            IntStream.rangeClosed(1, columns).forEach(column -> value.put("c" + column, rowIndex * column));
            builder.append("{");
            int index = 0;
            for (Map.Entry<String, Object> entry : value.entrySet()) {
                if (index++ > 0) builder.append(',');
                builder.append('"').append(entry.getKey()).append("\":").append(entry.getValue());
            }
            builder.append("}");
        }
        return builder.append("]").toString();
    }
}
