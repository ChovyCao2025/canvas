package org.chovy.canvas.canvas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.chovy.canvas.canvas.api.DataSourceConfigFacade;
import org.chovy.canvas.canvas.api.DataSourceConfigFacade.DataSourceConfigCommand;
import org.chovy.canvas.canvas.api.DataSourceConfigFacade.DataSourceConfigView;
import org.chovy.canvas.canvas.api.DataSourceConfigFacade.DataSourceListQuery;
import org.chovy.canvas.canvas.api.DataSourceConfigFacade.PageView;
import org.chovy.canvas.canvas.api.DataSourceConfigFacade.TenantIdentity;
import org.junit.jupiter.api.Test;

/**
 * 封装DataSourceConfigApplicationServiceTest相关的业务逻辑。
 */
class DataSourceConfigApplicationServiceTest {

    /**
     * 处理filtersByTenantTypeAndEnabledWhileKeepingPasswordsRedacted。
     */
    @Test
    void filtersByTenantTypeAndEnabledWhileKeepingPasswordsRedacted() {
        DataSourceConfigFacade service = new DataSourceConfigApplicationService();
        TenantIdentity tenantSeven = TenantIdentity.tenant(7L, "alice");

        DataSourceConfigView disabled = service.create(tenantSeven, command(
                null,
                "disabled-mysql",
                "JDBC",
                "jdbc:mysql://localhost:3306/disabled",
                "canvas",
                "disabled-secret",
                null,
                0));
        DataSourceConfigView enabled = service.create(tenantSeven, command(
                null,
                "orders-mysql",
                null,
                "jdbc:mysql://localhost:3306/orders",
                "canvas",
                "secret",
                null,
                null));
        service.create(TenantIdentity.tenant(8L, "bob"), command(
                null,
                "tenant-eight",
                "JDBC",
                "jdbc:mysql://localhost:3306/other",
                "other",
                "hidden",
                null,
                1));

        assertThat(disabled.id()).isEqualTo(1L);
        assertThat(enabled)
                .returns(7L, DataSourceConfigView::tenantId)
                .returns("JDBC", DataSourceConfigView::type)
                .returns("com.mysql.cj.jdbc.Driver", DataSourceConfigView::driverClassName)
                .returns(1, DataSourceConfigView::enabled)
                .returns("******", DataSourceConfigView::password);

        PageView<DataSourceConfigView> page = service.list(tenantSeven,
                new DataSourceListQuery(1, 20, "JDBC", 1, null));
        assertThat(page.total()).isEqualTo(1L);
        assertThat(page.records()).singleElement()
                .satisfies(row -> assertThat(row)
                        .returns(enabled.id(), DataSourceConfigView::id)
                        .returns("orders-mysql", DataSourceConfigView::name)
                        .returns("******", DataSourceConfigView::password));
    }

    /**
     * 处理supportsSuperAdminTenantFilterButPreventsTenantCrossAccess。
     */
    @Test
    void supportsSuperAdminTenantFilterButPreventsTenantCrossAccess() {
        DataSourceConfigFacade service = new DataSourceConfigApplicationService();
        DataSourceConfigView tenantSevenSource = service.create(TenantIdentity.tenant(7L, "alice"), command(
                null,
                "tenant-seven",
                null,
                "jdbc:mysql://localhost:3306/seven",
                "canvas",
                "secret",
                null,
                null));
        service.create(TenantIdentity.tenant(8L, "bob"), command(
                null,
                "tenant-eight",
                null,
                "jdbc:mysql://localhost:3306/eight",
                "canvas",
                "secret",
                null,
                null));

        PageView<DataSourceConfigView> superAdminPage = service.list(TenantIdentity.superAdmin("root"),
                new DataSourceListQuery(1, 20, null, null, 8L));
        assertThat(superAdminPage.total()).isEqualTo(1L);
        assertThat(superAdminPage.records()).singleElement()
                .returns(8L, DataSourceConfigView::tenantId);

        assertThatThrownBy(() -> service.update(TenantIdentity.tenant(8L, "bob"), tenantSevenSource.id(), command(
                null,
                "stolen",
                null,
                "jdbc:mysql://localhost:3306/seven",
                "canvas",
                "secret",
                null,
                null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Data source not found");
        assertThatThrownBy(() -> service.delete(TenantIdentity.tenant(8L, "bob"), tenantSevenSource.id()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Data source not found");
    }

    /**
     * 更新sAndDeletesWithinTenantScope。
     */
    @Test
    void updatesAndDeletesWithinTenantScope() {
        DataSourceConfigFacade service = new DataSourceConfigApplicationService();
        TenantIdentity tenantSeven = TenantIdentity.tenant(7L, "alice");
        DataSourceConfigView created = service.create(tenantSeven, command(
                null,
                "orders-mysql",
                null,
                "jdbc:mysql://localhost:3306/orders",
                "canvas",
                "secret",
                null,
                null));

        DataSourceConfigView updated = service.update(tenantSeven, created.id(), command(
                null,
                "orders-mysql-v2",
                null,
                "jdbc:mysql://localhost:3306/orders_v2",
                "canvas",
                "new-secret",
                null,
                0));

        assertThat(updated)
                .returns(created.id(), DataSourceConfigView::id)
                .returns(7L, DataSourceConfigView::tenantId)
                .returns("orders-mysql-v2", DataSourceConfigView::name)
                .returns(0, DataSourceConfigView::enabled)
                .returns("******", DataSourceConfigView::password);

        service.delete(tenantSeven, created.id());
        assertThat(service.list(tenantSeven, new DataSourceListQuery(1, 20, null, null, null)).total())
                .isZero();
    }

    /**
     * 处理validatesUnsupportedTypesMissingFieldsAndTableMetadataFailures。
     */
    @Test
    void validatesUnsupportedTypesMissingFieldsAndTableMetadataFailures() {
        DataSourceConfigFacade service = new DataSourceConfigApplicationService();
        TenantIdentity tenantSeven = TenantIdentity.tenant(7L, "alice");
        DataSourceConfigView created = service.create(tenantSeven, command(
                null,
                "orders-mysql",
                null,
                "jdbc:mysql://localhost:3306/orders",
                "canvas",
                "secret",
                null,
                null));

        assertThat(service.getTables(tenantSeven, created.id()))
                .extracting(DataSourceConfigFacade.DataSourceTableMetaView::name)
                .containsExactly("orders", "users");
        assertThat(service.getTables(tenantSeven, created.id()).getFirst().columns())
                .isEqualTo(List.of("id", "user_id", "amount"));

        DataSourceConfigView disabled = service.create(tenantSeven, command(
                null,
                "disabled-mysql",
                null,
                "jdbc:mysql://localhost:3306/disabled",
                "canvas",
                "secret",
                null,
                0));
        assertThatThrownBy(() -> service.getTables(tenantSeven, disabled.id()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Data source disabled");
        assertThatThrownBy(() -> service.create(tenantSeven, command(
                null,
                "s3",
                "S3",
                "s3://bucket",
                "canvas",
                "secret",
                null,
                null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported data source type: S3");
        assertThatThrownBy(() -> service.create(tenantSeven, command(
                null,
                "missing-password",
                null,
                "jdbc:mysql://localhost:3306/orders",
                "canvas",
                null,
                null,
                null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing data source field: password");
        assertThatThrownBy(() -> service.getTables(TenantIdentity.tenant(8L, "bob"), created.id()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Data source not found");
    }

    /**
     * 创建测试使用的数据源配置命令。
     */
    private static DataSourceConfigCommand command(Long tenantId,
                                                   String name,
                                                   String type,
                                                   String url,
                                                   String username,
                                                   String password,
                                                   String driverClassName,
                                                   Integer enabled) {
        return new DataSourceConfigCommand(
                tenantId,
                name,
                type,
                url,
                username,
                password,
                driverClassName,
                "managed by tests",
                enabled,
                "tester");
    }
}
