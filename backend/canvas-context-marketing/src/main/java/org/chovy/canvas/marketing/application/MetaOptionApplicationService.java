package org.chovy.canvas.marketing.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.MetaOptionFacade;
import org.chovy.canvas.marketing.api.MetaOptionView;
import org.chovy.canvas.marketing.domain.MetaOptionCatalog;
import org.springframework.stereotype.Service;

/**
 * 编排MetaOption相关的应用层用例。
 */
@Service
public class MetaOptionApplicationService implements MetaOptionFacade {

    private final MetaOptionCatalog catalog = new MetaOptionCatalog();

    /**
     * 执行options业务操作。
     */
    @Override
    public List<MetaOptionView> options(Long tenantId, String category) {
        return catalog.options(safeTenantId(tenantId), category);
    }

    /**
     * 执行optionsBatch业务操作。
     */
    @Override
    public Map<String, List<MetaOptionView>> optionsBatch(Long tenantId, List<String> categories) {
        return catalog.optionsBatch(safeTenantId(tenantId), categories);
    }

    /**
     * 执行abExperiments业务操作。
     */
    @Override
    public List<MetaOptionView> abExperiments(Long tenantId) {
        return catalog.abExperiments(safeTenantId(tenantId));
    }

    /**
     * 执行abExperimentGroups业务操作。
     */
    @Override
    public List<MetaOptionView> abExperimentGroups(Long tenantId, String experimentKey) {
        return catalog.abExperimentGroups(safeTenantId(tenantId), experimentKey);
    }

    /**
     * 执行bizLines业务操作。
     */
    @Override
    public List<MetaOptionView> bizLines(Long tenantId) {
        return catalog.bizLines(safeTenantId(tenantId));
    }

    /**
     * 执行bizLineApis业务操作。
     */
    @Override
    public List<MetaOptionView> bizLineApis(Long tenantId, String bizLineKey) {
        return catalog.bizLineApis(safeTenantId(tenantId), bizLineKey);
    }

    /**
     * 执行aiProviders业务操作。
     */
    @Override
    public List<MetaOptionView> aiProviders(Long tenantId) {
        return catalog.aiProviders(safeTenantId(tenantId));
    }

    /**
     * 执行aiTemplates业务操作。
     */
    @Override
    public List<MetaOptionView> aiTemplates(Long tenantId) {
        return catalog.aiTemplates(safeTenantId(tenantId));
    }

    /**
     * 执行aiModels业务操作。
     */
    @Override
    public List<MetaOptionView> aiModels(Long tenantId, Long providerId) {
        return catalog.aiModels(safeTenantId(tenantId), providerId);
    }

    /**
     * 执行identityTypes业务操作。
     */
    @Override
    public List<MetaOptionView> identityTypes(Integer allowImport) {
        return catalog.identityTypes(allowImport);
    }

    /**
     * 执行apiDefinitions业务操作。
     */
    @Override
    public List<Map<String, Object>> apiDefinitions() {
        return catalog.apiDefinitions();
    }

    /**
     * 执行eventDefinitions业务操作。
     */
    @Override
    public List<Map<String, Object>> eventDefinitions() {
        return catalog.eventDefinitions();
    }

    /**
     * 执行contextFields业务操作。
     */
    @Override
    public List<Map<String, Object>> contextFields() {
        return catalog.contextFields();
    }

    /**
     * 执行canvasContextFields业务操作。
     */
    @Override
    public List<Map<String, Object>> canvasContextFields(
            List<String> eventCodes,
            List<String> apiKeys,
            List<String> outputPrefixes) {
        return catalog.canvasContextFields(eventCodes, apiKeys, outputPrefixes);
    }

    /**
     * 执行mqDefinitions业务操作。
     */
    @Override
    public List<Map<String, Object>> mqDefinitions() {
        return catalog.mqDefinitions();
    }

    /**
     * 执行taggerTags业务操作。
     */
    @Override
    public List<MetaOptionView> taggerTags(String type) {
        return catalog.taggerTags(type);
    }

    /**
     * 执行taggerTagValues业务操作。
     */
    @Override
    public List<MetaOptionView> taggerTagValues(String tagCode) {
        return catalog.taggerTagValues(tagCode);
    }

    /**
     * 执行safeTenantId业务操作。
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }
}
