package org.chovy.canvas.marketing.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.TagImportFacade;
import org.chovy.canvas.marketing.domain.TagImportCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TagImportApplicationService implements TagImportFacade {

    private static final Long DEFAULT_TENANT_ID = 7L;

    private final TagImportCatalog catalog;

    public TagImportApplicationService() {
        this(new TagImportCatalog());
    }

    public TagImportApplicationService(TagImportCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> apiPush(Long tenantId, List<Map<String, Object>> rows) {
        return catalog.importRows(tenantIdOrDefault(tenantId), "API_PUSH", null, rows);
    }

    @Override
    public byte[] excelTemplate() {
        return catalog.excelTemplate();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importExcel(Long tenantId, String fileName, byte[] bytes) {
        return catalog.importRows(tenantIdOrDefault(tenantId), "EXCEL_IMPORT", fileName,
                catalog.readCsvLikeRows(bytes));
    }

    @Override
    public List<Map<String, Object>> listBatches(Long tenantId) {
        return catalog.listBatches(tenantIdOrDefault(tenantId));
    }

    @Override
    public List<Map<String, Object>> listErrors(Long tenantId, Long batchId) {
        return catalog.listErrors(tenantIdOrDefault(tenantId), batchId);
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }
}
