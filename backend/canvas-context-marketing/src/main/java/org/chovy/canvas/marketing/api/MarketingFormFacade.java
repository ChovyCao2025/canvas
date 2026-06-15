package org.chovy.canvas.marketing.api;

import java.util.List;
import java.util.Map;

public interface MarketingFormFacade {

    List<Map<String, Object>> listForms(Long tenantId);

    Map<String, Object> getForm(Long tenantId, Long id);

    Map<String, Object> createForm(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> updateForm(Long tenantId, Long id, Map<String, Object> payload, String actor);

    Map<String, Object> setStatus(Long tenantId, Long id, Map<String, Object> payload, String actor);

    List<Map<String, Object>> submissions(Long tenantId, Long formId, Integer limit);
}
