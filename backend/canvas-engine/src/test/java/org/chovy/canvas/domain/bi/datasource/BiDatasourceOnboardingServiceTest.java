package org.chovy.canvas.domain.bi.datasource;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.DataSourceConfigDO;
import org.chovy.canvas.dal.mapper.DataSourceConfigMapper;
import org.chovy.canvas.domain.datasource.DataSourceCredentialCipher;
import org.junit.jupiter.api.Test;

import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BiDatasourceOnboardingServiceTest {

    private static final DataSourceCredentialCipher CIPHER =
            new DataSourceCredentialCipher("test-datasource-credential-secret-32b");

    @Test
    void exposesQuickBiApiDatasourceAsAvailableHttpExtractConnector() {
        BiDatasourceOnboardingService service = new BiDatasourceOnboardingService(mock(DataSourceConfigMapper.class));

        BiDatasourceConnectorCapability api = service.connectorCatalog()
                .stream()
                .filter(connector -> "API".equals(connector.connectorType()))
                .findFirst()
                .orElseThrow();

        assertThat(api.sourceCategory()).isEqualTo("HTTP");
        assertThat(api.capacityCategory()).isEqualTo("HTTP_EXTRACT_SMALL");
        assertThat(api.capacityNote()).contains("bounded preview");
        assertThat(api.supportStatus()).isEqualTo("AVAILABLE");
        assertThat(api.supportedModes()).containsExactly("EXTRACT");
        assertThat(api.supportsTableDataset()).isTrue();
        assertThat(api.supportsCredentials()).isTrue();
        assertThat(api.supportsConnectionTest()).isFalse();
        assertThat(api.supportsSchemaSync()).isFalse();
        assertThat(api.supportsSqlDataset()).isFalse();
    }

    @Test
    void createsApiDatasourceWithSanitizedConnectorConfigAndApiView() {
        DataSourceConfigMapper mapper = mock(DataSourceConfigMapper.class);
        AtomicReference<DataSourceConfigDO> inserted = new AtomicReference<>();
        when(mapper.insert(any(DataSourceConfigDO.class))).thenAnswer(invocation -> {
            DataSourceConfigDO row = invocation.getArgument(0);
            row.setId(81L);
            inserted.set(row);
            return 1;
        });
        BiDatasourceOnboardingService service = new BiDatasourceOnboardingService(
                mapper,
                null,
                new DataSourceCredentialCipher(DataSourceCredentialCipher.DEFAULT_SECRET));

        BiDatasourceOnboardingView view = service.createOnboardingSource(17L, "alice", new BiDatasourceOnboardingCommand(
                "API",
                "Orders API",
                "https://api.example.com/orders?token=url-secret",
                "Authorization",
                "bearer-token",
                null,
                "QuickBI API datasource",
                true,
                "extract",
                Map.of(
                        "requestMethod", "POST",
                        "authType", "BEARER",
                        "headers", List.of(
                                Map.of("name", "Authorization", "value", "Bearer header-secret", "variable", false),
                                Map.of("name", "X-Tenant", "value", "{{tenantId}}", "variable", true)),
                        "parameters", List.of(Map.of("name", "page", "value", "{{page}}", "variable", true)),
                        "bodyTemplate", "{\"campaign\":\"{{campaignId}}\"}",
                        "responseRowsPath", "$.data.items",
                        "responseFormat", "JSON")));

        DataSourceConfigDO row = inserted.get();
        assertThat(row.getTenantId()).isEqualTo(17L);
        assertThat(row.getCreatedBy()).isEqualTo("alice");
        assertThat(row.getType()).isEqualTo("API");
        assertThat(row.getConnectorType()).isEqualTo("API");
        assertThat(row.getConnectionMode()).isEqualTo("EXTRACT");
        assertThat(row.getDriverClassName()).isEqualTo("HTTP_JSON");
        assertThat(row.getConnectorConfigJson())
                .contains("\"requestMethod\":\"POST\"")
                .contains("\"authType\":\"BEARER\"")
                .contains("\"responseRowsPath\":\"$.data.items\"")
                .contains("\"responseFormat\":\"JSON\"")
                .doesNotContain("bearer-token")
                .doesNotContain("header-secret")
                .doesNotContain("url-secret");
        assertThat(row.getPassword())
                .startsWith(DataSourceCredentialCipher.PREFIX)
                .doesNotContain("bearer-token");
        assertThat(view.sourceKey()).isEqualTo("api-81");
        assertThat(view.type()).isEqualTo("API");
        assertThat(view.connectorType()).isEqualTo("API");
        assertThat(view.connectionMode()).isEqualTo("EXTRACT");
        assertThat(view.driverClassName()).isEqualTo("HTTP_JSON");
        assertThat(view.maskedUrl()).isEqualTo("https://api.example.com/orders?token=***");
        assertThat(view.maskedUrl()).doesNotContain("url-secret");
        assertThat(view.supportedModes()).containsExactly("EXTRACT");
        assertThat(view.capabilities()).containsExactly("TABLE_DATASET", "CREDENTIALS");
    }

    @Test
    void exposesApplicationDatasourceWithDedicatedAppExtractCapacityCategory() {
        BiDatasourceOnboardingService service = new BiDatasourceOnboardingService(mock(DataSourceConfigMapper.class));

        BiDatasourceConnectorCapability app = service.connectorCatalog()
                .stream()
                .filter(connector -> "APP_ANALYTICS".equals(connector.connectorType()))
                .findFirst()
                .orElseThrow();

        assertThat(app.sourceCategory()).isEqualTo("APP");
        assertThat(app.capacityCategory()).isEqualTo("APP_EXTRACT_SMALL");
        assertThat(app.supportStatus()).isEqualTo("AVAILABLE");
        assertThat(app.supportedModes()).containsExactly("EXTRACT");
        assertThat(app.supportsTableDataset()).isTrue();
        assertThat(app.supportsCredentials()).isTrue();
        assertThat(app.supportsSqlDataset()).isFalse();
    }

    @Test
    void createsApplicationDatasourceOnHttpJsonExtractRuntimePath() {
        DataSourceConfigMapper mapper = mock(DataSourceConfigMapper.class);
        AtomicReference<DataSourceConfigDO> inserted = new AtomicReference<>();
        when(mapper.insert(any(DataSourceConfigDO.class))).thenAnswer(invocation -> {
            DataSourceConfigDO row = invocation.getArgument(0);
            row.setId(82L);
            inserted.set(row);
            return 1;
        });
        BiDatasourceOnboardingService service = new BiDatasourceOnboardingService(
                mapper,
                null,
                new DataSourceCredentialCipher(DataSourceCredentialCipher.DEFAULT_SECRET));

        BiDatasourceOnboardingView view = service.createOnboardingSource(17L, "alice", new BiDatasourceOnboardingCommand(
                "APP_ANALYTICS",
                "Campaign App",
                "https://app.example.com/openapi/campaigns",
                "Authorization",
                "app-token",
                null,
                "Application datasource",
                true,
                "extract",
                Map.of(
                        "requestMethod", "GET",
                        "authType", "BEARER",
                        "responseRowsPath", "$.campaigns",
                        "responseFormat", "JSON")));

        DataSourceConfigDO row = inserted.get();
        assertThat(row.getType()).isEqualTo("API");
        assertThat(row.getConnectorType()).isEqualTo("APP_ANALYTICS");
        assertThat(row.getConnectionMode()).isEqualTo("EXTRACT");
        assertThat(row.getDriverClassName()).isEqualTo("HTTP_JSON");
        assertThat(row.getConnectorConfigJson())
                .contains("\"requestMethod\":\"GET\"")
                .contains("\"responseRowsPath\":\"$.campaigns\"")
                .doesNotContain("app-token");
        assertThat(view.sourceKey()).isEqualTo("api-82");
        assertThat(view.type()).isEqualTo("API");
        assertThat(view.connectorType()).isEqualTo("APP_ANALYTICS");
        assertThat(view.supportedModes()).containsExactly("EXTRACT");
        assertThat(view.capabilities()).containsExactly("TABLE_DATASET", "CREDENTIALS");
    }

    @Test
    void exposesCsvExcelDatasourceAsAvailableFileExtractConnector() {
        BiDatasourceOnboardingService service = new BiDatasourceOnboardingService(mock(DataSourceConfigMapper.class));

        BiDatasourceConnectorCapability file = service.connectorCatalog()
                .stream()
                .filter(connector -> "CSV_EXCEL".equals(connector.connectorType()))
                .findFirst()
                .orElseThrow();

        assertThat(file.sourceCategory()).isEqualTo("FILE");
        assertThat(file.capacityCategory()).isEqualTo("FILE_EXTRACT_SMALL");
        assertThat(file.supportStatus()).isEqualTo("AVAILABLE");
        assertThat(file.supportedModes()).containsExactly("EXTRACT");
        assertThat(file.supportsTableDataset()).isTrue();
        assertThat(file.supportsCredentials()).isFalse();
        assertThat(file.supportsConnectionTest()).isFalse();
        assertThat(file.supportsSchemaSync()).isFalse();
        assertThat(file.supportsSqlDataset()).isFalse();
    }

    @Test
    void createsCsvExcelDatasourceWithoutCredentialsAndSanitizesFileConfig() {
        DataSourceConfigMapper mapper = mock(DataSourceConfigMapper.class);
        AtomicReference<DataSourceConfigDO> inserted = new AtomicReference<>();
        when(mapper.insert(any(DataSourceConfigDO.class))).thenAnswer(invocation -> {
            DataSourceConfigDO row = invocation.getArgument(0);
            row.setId(91L);
            inserted.set(row);
            return 1;
        });
        BiDatasourceOnboardingService service = new BiDatasourceOnboardingService(
                mapper,
                null,
                new DataSourceCredentialCipher(DataSourceCredentialCipher.DEFAULT_SECRET));

        BiDatasourceOnboardingView view = service.createOnboardingSource(17L, "alice", new BiDatasourceOnboardingCommand(
                "CSV_EXCEL",
                "Upload extract",
                "",
                "",
                "",
                null,
                "CSV datasource",
                true,
                "EXTRACT",
                Map.of(
                        "fileName", " orders.xlsx ",
                        "fileType", " xlsx ",
                        "sheetName", " Orders ",
                        "delimiter", "|",
                        "headerRow", true,
                        "encoding", " utf-8 ",
                        "accessToken", "file-secret")));

        DataSourceConfigDO row = inserted.get();
        assertThat(row.getTenantId()).isEqualTo(17L);
        assertThat(row.getType()).isEqualTo("FILE");
        assertThat(row.getConnectorType()).isEqualTo("CSV_EXCEL");
        assertThat(row.getConnectionMode()).isEqualTo("EXTRACT");
        assertThat(row.getUrl()).isEqualTo("file://orders.xlsx");
        assertThat(row.getUsername()).isEqualTo("file_upload");
        assertThat(row.getPassword()).isEqualTo("");
        assertThat(row.getDriverClassName()).isEqualTo("FILE_UPLOAD");
        assertThat(row.getConnectorConfigJson())
                .contains("\"fileName\":\"orders.xlsx\"")
                .contains("\"fileType\":\"XLSX\"")
                .contains("\"sheetName\":\"Orders\"")
                .contains("\"delimiter\":\"|\"")
                .contains("\"headerRow\":true")
                .contains("\"encoding\":\"UTF-8\"")
                .doesNotContain("file-secret");
        assertThat(view.sourceKey()).isEqualTo("file-91");
        assertThat(view.type()).isEqualTo("FILE");
        assertThat(view.connectorType()).isEqualTo("CSV_EXCEL");
        assertThat(view.connectionMode()).isEqualTo("EXTRACT");
        assertThat(view.driverClassName()).isEqualTo("FILE_UPLOAD");
        assertThat(view.maskedUsername()).isEqualTo("fi***ad");
        assertThat(view.supportedModes()).containsExactly("EXTRACT");
        assertThat(view.capabilities()).containsExactly("TABLE_DATASET");
    }

    @Test
    void testsApiHttpJsonConnectionWithoutOpeningJdbcConnection() {
        DataSourceConfigMapper mapper = mock(DataSourceConfigMapper.class);
        when(mapper.selectById(81L)).thenReturn(apiRuntimeSource("""
                {"requestMethod":"GET","authType":"BEARER","responseRowsPath":"$.data.items","responseFormat":"JSON"}
                """));
        AtomicReference<BiDatasourceRuntimeService.ApiHttpRequest> requestSeen = new AtomicReference<>();
        BiDatasourceRuntimeService service = new BiDatasourceRuntimeService(
                mapper,
                null,
                null,
                CIPHER,
                new ObjectMapper(),
                failIfJdbcOpened(),
                request -> {
                    requestSeen.set(request);
                    return new BiDatasourceRuntimeService.ApiHttpResponse(200, "OK", "{\"ok\":true}");
                });

        BiDatasourceConnectionTestResult result = service.testConnection(81L, 17L);

        assertThat(result.sourceKey()).isEqualTo("api-81");
        assertThat(result.connectorType()).isEqualTo("API");
        assertThat(result.success()).isTrue();
        assertThat(result.message()).isEqualTo("connection ok");
        assertThat(result.databaseProductName()).isEqualTo("HTTP JSON");
        assertThat(result.databaseProductVersion()).isEqualTo("200 OK");
        assertThat(requestSeen.get().method()).isEqualTo("GET");
        assertThat(requestSeen.get().url()).isEqualTo("https://api.example.com/orders?token=url-secret");
        assertThat(requestSeen.get().headers()).containsEntry("Authorization", "Bearer bearer-token");
    }

    @Test
    void previewsApiHttpJsonSchemaFromConfiguredRowsPath() {
        DataSourceConfigMapper mapper = mock(DataSourceConfigMapper.class);
        when(mapper.selectById(81L)).thenReturn(apiRuntimeSource("""
                {"requestMethod":"POST","authType":"BEARER","responseRowsPath":"$.data.items","responseFormat":"JSON",
                 "headers":[{"name":"X-Tenant","value":"tenant-17","variable":false}],
                 "bodyTemplate":"{\\"page\\":1}"}
                """));
        AtomicReference<BiDatasourceRuntimeService.ApiHttpRequest> requestSeen = new AtomicReference<>();
        BiDatasourceRuntimeService service = new BiDatasourceRuntimeService(
                mapper,
                null,
                null,
                CIPHER,
                new ObjectMapper(),
                failIfJdbcOpened(),
                request -> {
                    requestSeen.set(request);
                    return new BiDatasourceRuntimeService.ApiHttpResponse(200, "OK", """
                            {"data":{"items":[
                              {"order_id":42,"amount":19.95,"buyer":"Alice","paid":true},
                              {"order_id":43,"amount":7.50,"buyer":"Bob","paid":false}
                            ]}}
                            """);
                });

        BiDatasourceSchemaPreview preview = service.previewSchema(81L, 17L, 50);

        assertThat(requestSeen.get().method()).isEqualTo("POST");
        assertThat(requestSeen.get().headers()).containsEntry("Authorization", "Bearer bearer-token");
        assertThat(requestSeen.get().headers()).containsEntry("X-Tenant", "tenant-17");
        assertThat(requestSeen.get().body()).isEqualTo("{\"page\":1}");
        assertThat(preview.sourceKey()).isEqualTo("api-81");
        assertThat(preview.connectorType()).isEqualTo("API");
        assertThat(preview.tables()).singleElement().satisfies(table -> {
            assertThat(table.name()).isEqualTo("api_response");
            assertThat(table.tableType()).isEqualTo("HTTP_JSON");
            assertThat(table.columns()).extracting(BiDatasourceColumnPreview::name)
                    .containsExactly("order_id", "amount", "buyer", "paid");
            assertThat(table.columns()).extracting(BiDatasourceColumnPreview::typeName)
                    .containsExactly("BIGINT", "DOUBLE", "VARCHAR", "BOOLEAN");
            assertThat(table.columns()).extracting(BiDatasourceColumnPreview::dataType)
                    .containsExactly(Types.BIGINT, Types.DOUBLE, Types.VARCHAR, Types.BOOLEAN);
        });
    }

    private static DataSourceConfigDO apiRuntimeSource(String connectorConfigJson) {
        DataSourceConfigDO row = new DataSourceConfigDO();
        row.setId(81L);
        row.setTenantId(17L);
        row.setName("Orders API");
        row.setType("API");
        row.setConnectorType("API");
        row.setConnectionMode("EXTRACT");
        row.setDriverClassName("HTTP_JSON");
        row.setUrl("https://api.example.com/orders?token=url-secret");
        row.setUsername("Authorization");
        row.setPassword(CIPHER.encrypt("bearer-token"));
        row.setConnectorConfigJson(connectorConfigJson);
        row.setEnabled(1);
        return row;
    }

    private static BiDatasourceRuntimeService.JdbcConnectionFactory failIfJdbcOpened() {
        return (source, password) -> {
            throw new AssertionError("API datasource runtime must not open JDBC connections");
        };
    }
}
