package org.chovy.canvas.canvas.application;

import org.chovy.canvas.canvas.api.ApiDefinitionFacade;
import org.chovy.canvas.canvas.domain.ApiDefinitionCatalog;
import org.springframework.stereotype.Service;

@Service
public class ApiDefinitionApplicationService implements ApiDefinitionFacade {

    private final ApiDefinitionCatalog catalog;

    public ApiDefinitionApplicationService() {
        this(new ApiDefinitionCatalog());
    }

    ApiDefinitionApplicationService(ApiDefinitionCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public PageView<ApiDefinitionView> list(ApiDefinitionListQuery query) {
        return catalog.list(query == null ? new ApiDefinitionListQuery(1, 20, null) : query);
    }

    @Override
    public ApiDefinitionView create(ApiDefinitionCommand command) {
        return catalog.create(command);
    }

    @Override
    public ApiDefinitionView update(Long id, ApiDefinitionCommand command) {
        return catalog.update(id, command);
    }

    @Override
    public void delete(Long id) {
        catalog.delete(id);
    }
}
