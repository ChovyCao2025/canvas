package org.chovy.canvas.marketing.api;

import java.util.List;
import java.util.Map;

public interface TagImportFacade {

    Map<String, Object> apiPush(Long tenantId, List<Map<String, Object>> rows);

    byte[] excelTemplate();

    Map<String, Object> importExcel(Long tenantId, String fileName, byte[] bytes);

    List<Map<String, Object>> listBatches(Long tenantId);

    List<Map<String, Object>> listErrors(Long tenantId, Long batchId);
}
