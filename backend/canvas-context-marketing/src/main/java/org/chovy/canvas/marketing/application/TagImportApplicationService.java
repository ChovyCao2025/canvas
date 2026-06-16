package org.chovy.canvas.marketing.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.TagImportFacade;
import org.chovy.canvas.marketing.domain.TagImportCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 编排TagImport相关的应用层用例。
 */
@Service
public class TagImportApplicationService implements TagImportFacade {

    /**
     * 保存DEFAULT_TENANT_ID字段值。
     */
    private static final Long DEFAULT_TENANT_ID = 7L;

    /**
     * 承载该应用服务的内存目录。
     */
    private final TagImportCatalog catalog;

    /**
     * 创建TagImportApplicationService实例。
     */
    public TagImportApplicationService() {
        this(new TagImportCatalog());
    }

    /**
     * 创建TagImportApplicationService实例。
     */
    public TagImportApplicationService(TagImportCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 执行apiPush业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> apiPush(Long tenantId, List<Map<String, Object>> rows) {
        return catalog.importRows(tenantIdOrDefault(tenantId), "API_PUSH", null, rows);
    }

    /**
     * 执行excelTemplate业务操作。
     */
    @Override
    public byte[] excelTemplate() {
        return catalog.excelTemplate();
    }

    /**
     * 执行importExcel业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importExcel(Long tenantId, String fileName, byte[] bytes) {
        return catalog.importRows(tenantIdOrDefault(tenantId), "EXCEL_IMPORT", fileName,
                catalog.readCsvLikeRows(bytes));
    }

    /**
     * 查询batches列表。
     */
    @Override
    public List<Map<String, Object>> listBatches(Long tenantId) {
        return catalog.listBatches(tenantIdOrDefault(tenantId));
    }

    /**
     * 查询errors列表。
     */
    @Override
    public List<Map<String, Object>> listErrors(Long tenantId, Long batchId) {
        return catalog.listErrors(tenantIdOrDefault(tenantId), batchId);
    }

    /**
     * 执行tenantIdOrDefault业务操作。
     */
    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }
}
