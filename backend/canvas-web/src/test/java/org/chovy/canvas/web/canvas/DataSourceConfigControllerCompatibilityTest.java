package org.chovy.canvas.web.canvas;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.chovy.canvas.canvas.api.DataSourceConfigFacade;
import org.chovy.canvas.canvas.api.DataSourceConfigFacade.DataSourceConfigCommand;
import org.chovy.canvas.canvas.api.DataSourceConfigFacade.DataSourceConfigView;
import org.chovy.canvas.canvas.api.DataSourceConfigFacade.DataSourceListQuery;
import org.chovy.canvas.canvas.api.DataSourceConfigFacade.DataSourceTableMetaView;
import org.chovy.canvas.canvas.api.DataSourceConfigFacade.PageView;
import org.chovy.canvas.canvas.api.DataSourceConfigFacade.TenantIdentity;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class DataSourceConfigControllerCompatibilityTest {

    @Test
    void exposesLegacyDataSourceRoutesWithCompatibilityEnvelopeAndForwardedQuery() {
        RecordingDataSourceConfigFacade facade = new RecordingDataSourceConfigFacade();
        WebTestClient client = webClient(facade);

        client.get()
                .uri("/canvas/data-sources?page=2&size=3&type=JDBC&enabled=1&tenantId=9")
                .header("X-Tenant-Id", "7")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.total").isEqualTo(1)
                .jsonPath("$.data.records[0].id").isEqualTo(11)
                .jsonPath("$.data.records[0].password").isEqualTo("******");

        assertThat(facade.operations).containsExactly("list");
        assertThat(facade.lastOperator.tenantId()).isEqualTo(7L);
        assertThat(facade.lastQuery)
                .returns(2, DataSourceListQuery::page)
                .returns(3, DataSourceListQuery::size)
                .returns("JDBC", DataSourceListQuery::type)
                .returns(1, DataSourceListQuery::enabled)
                .returns(9L, DataSourceListQuery::tenantId);

        client.get()
                .uri("/canvas/data-sources/11/tables")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].name").isEqualTo("orders")
                .jsonPath("$.data[0].columns[0]").isEqualTo("id");

        assertThat(facade.operations).containsExactly("list", "getTables");
        assertThat(facade.lastOperator.tenantId()).isEqualTo(7L);
        assertThat(facade.lastId).isEqualTo(11L);
    }

    @Test
    void wrapsCreateUpdateAndDeleteInLegacyEnvelope() {
        RecordingDataSourceConfigFacade facade = new RecordingDataSourceConfigFacade();
        WebTestClient client = webClient(facade);

        client.post()
                .uri("/canvas/data-sources")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "name": "orders-mysql",
                          "url": "jdbc:mysql://localhost:3306/orders",
                          "username": "canvas",
                          "password": "secret"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.id").isEqualTo(11)
                .jsonPath("$.data.type").isEqualTo("JDBC")
                .jsonPath("$.data.driverClassName").isEqualTo("com.mysql.cj.jdbc.Driver")
                .jsonPath("$.data.enabled").isEqualTo(1)
                .jsonPath("$.data.password").isEqualTo("******");

        assertThat(facade.lastOperator.tenantId()).isEqualTo(7L);
        assertThat(facade.lastCommand)
                .returns("orders-mysql", DataSourceConfigCommand::name)
                .returns("jdbc:mysql://localhost:3306/orders", DataSourceConfigCommand::url)
                .returns("canvas", DataSourceConfigCommand::username)
                .returns("secret", DataSourceConfigCommand::password);

        client.put()
                .uri("/canvas/data-sources/11")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "name": "orders-mysql-v2",
                          "url": "jdbc:mysql://localhost:3306/orders_v2",
                          "username": "canvas",
                          "password": "new-secret",
                          "enabled": 0
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.id").isEqualTo(11)
                .jsonPath("$.data.name").isEqualTo("orders-mysql-v2")
                .jsonPath("$.data.enabled").isEqualTo(0)
                .jsonPath("$.data.password").isEqualTo("******");

        client.delete()
                .uri("/canvas/data-sources/11")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data").doesNotExist();

        assertThat(facade.operations).containsExactly("create", "update", "delete");
        assertThat(facade.lastId).isEqualTo(11L);
    }

    @Test
    void illegalTypeAndMissingSourceMapToApi001BadRequestEnvelope() {
        RecordingDataSourceConfigFacade facade = new RecordingDataSourceConfigFacade();
        WebTestClient client = webClient(facade);

        client.post()
                .uri("/canvas/data-sources")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "name": "api-source",
                          "type": "API",
                          "url": "https://example.test",
                          "username": "canvas",
                          "password": "secret"
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("Unsupported data source type: API")
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();

        client.get()
                .uri("/canvas/data-sources/404/tables")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("Data source not found: 404")
                .jsonPath("$.errorCode").isEqualTo("API_001");
    }

    private static WebTestClient webClient(DataSourceConfigFacade facade) {
        return WebTestClient.bindToController(new DataSourceConfigController(facade)).build();
    }

    private static final class RecordingDataSourceConfigFacade implements DataSourceConfigFacade {
        private final List<String> operations = new ArrayList<>();
        private TenantIdentity lastOperator;
        private DataSourceListQuery lastQuery;
        private DataSourceConfigCommand lastCommand;
        private Long lastId;

        @Override
        public PageView<DataSourceConfigView> list(TenantIdentity operator, DataSourceListQuery query) {
            operations.add("list");
            lastOperator = operator;
            lastQuery = query;
            return new PageView<>(1L, List.of(view(11L, "orders-mysql", 1)));
        }

        @Override
        public List<DataSourceTableMetaView> getTables(TenantIdentity operator, Long id) {
            operations.add("getTables");
            lastOperator = operator;
            lastId = id;
            if (id == 404L) {
                throw new IllegalArgumentException("Data source not found: 404");
            }
            return List.of(new DataSourceTableMetaView("orders", List.of("id", "user_id", "amount")));
        }

        @Override
        public DataSourceConfigView create(TenantIdentity operator, DataSourceConfigCommand command) {
            operations.add("create");
            lastOperator = operator;
            lastCommand = command;
            if ("API".equals(command.type())) {
                throw new IllegalArgumentException("Unsupported data source type: API");
            }
            return view(11L, command.name(), 1);
        }

        @Override
        public DataSourceConfigView update(TenantIdentity operator, Long id, DataSourceConfigCommand command) {
            operations.add("update");
            lastOperator = operator;
            lastId = id;
            lastCommand = command;
            return view(id, command.name(), command.enabled());
        }

        @Override
        public void delete(TenantIdentity operator, Long id) {
            operations.add("delete");
            lastOperator = operator;
            lastId = id;
        }

        private static DataSourceConfigView view(Long id, String name, Integer enabled) {
            return new DataSourceConfigView(
                    id,
                    7L,
                    name,
                    "JDBC",
                    "jdbc:mysql://localhost:3306/orders",
                    "canvas",
                    "******",
                    "com.mysql.cj.jdbc.Driver",
                    null,
                    enabled,
                    "tester");
        }
    }
}
