package org.chovy.canvas.canvas.application;

import java.util.List;

import org.chovy.canvas.canvas.api.DataSourceConfigFacade;
import org.chovy.canvas.canvas.domain.DataSourceConfigCatalog;
import org.chovy.canvas.canvas.domain.DataSourceConfigCatalog.Command;
import org.chovy.canvas.canvas.domain.DataSourceConfigCatalog.DataSourceConfig;
import org.chovy.canvas.canvas.domain.DataSourceConfigCatalog.Query;
import org.chovy.canvas.canvas.domain.DataSourceConfigCatalog.TenantScope;
import org.springframework.stereotype.Service;

@Service
public class DataSourceConfigApplicationService implements DataSourceConfigFacade {

    private final DataSourceConfigCatalog catalog;

    public DataSourceConfigApplicationService() {
        this(new DataSourceConfigCatalog());
    }

    DataSourceConfigApplicationService(DataSourceConfigCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public PageView<DataSourceConfigView> list(TenantIdentity operator, DataSourceListQuery query) {
        DataSourceConfigCatalog.Page<DataSourceConfig> page = catalog.list(scope(operator), query(query));
        return new PageView<>(page.total(), page.records().stream()
                .map(this::view)
                .toList());
    }

    @Override
    public List<DataSourceTableMetaView> getTables(TenantIdentity operator, Long id) {
        return catalog.tables(scope(operator), id).stream()
                .map(table -> new DataSourceTableMetaView(table.name(), table.columns()))
                .toList();
    }

    @Override
    public DataSourceConfigView create(TenantIdentity operator, DataSourceConfigCommand command) {
        return view(catalog.create(scope(operator), command(command)));
    }

    @Override
    public DataSourceConfigView update(TenantIdentity operator, Long id, DataSourceConfigCommand command) {
        return view(catalog.update(scope(operator), id, command(command)));
    }

    @Override
    public void delete(TenantIdentity operator, Long id) {
        catalog.delete(scope(operator), id);
    }

    private DataSourceConfigView view(DataSourceConfig config) {
        return new DataSourceConfigView(
                config.id(),
                config.tenantId(),
                config.name(),
                config.type(),
                config.url(),
                config.username(),
                DataSourceConfigCatalog.MASKED_PASSWORD,
                config.driverClassName(),
                config.description(),
                config.enabled(),
                config.createdBy());
    }

    private static Query query(DataSourceListQuery query) {
        if (query == null) {
            return new Query(1, 20, null, null, null);
        }
        return new Query(query.page(), query.size(), query.type(), query.enabled(), query.tenantId());
    }

    private static Command command(DataSourceConfigCommand command) {
        if (command == null) {
            return null;
        }
        return new Command(
                command.tenantId(),
                command.name(),
                command.type(),
                command.url(),
                command.username(),
                command.password(),
                command.driverClassName(),
                command.description(),
                command.enabled(),
                command.createdBy());
    }

    private static TenantScope scope(TenantIdentity operator) {
        if (operator == null) {
            return new TenantScope(null, false);
        }
        return new TenantScope(operator.tenantId(), operator.isSuperAdmin());
    }
}
