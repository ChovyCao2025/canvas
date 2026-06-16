package org.chovy.canvas.marketing.api;

import java.util.List;
import java.util.Map;

/**
 * 定义TagImportFacade的营销上下文访问契约。
 */
public interface TagImportFacade {

    /**
     * 执行apiPush业务操作。
     */
    Map<String, Object> apiPush(Long tenantId, List<Map<String, Object>> rows);

    /**
     * 执行excelTemplate业务操作。
     */
    byte[] excelTemplate();

    /**
     * 执行importExcel业务操作。
     */
    Map<String, Object> importExcel(Long tenantId, String fileName, byte[] bytes);

    /**
     * 查询batches列表。
     */
    List<Map<String, Object>> listBatches(Long tenantId);

    /**
     * 查询errors列表。
     */
    List<Map<String, Object>> listErrors(Long tenantId, Long batchId);
}
