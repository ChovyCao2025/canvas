package org.chovy.canvas.marketing.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.MetaOptionFacade;
import org.chovy.canvas.marketing.api.MetaOptionView;
import org.chovy.canvas.marketing.domain.MetaOptionCatalog;
import org.springframework.stereotype.Service;

@Service
public class MetaOptionApplicationService implements MetaOptionFacade {

    private final MetaOptionCatalog catalog = new MetaOptionCatalog();

    @Override
    public List<MetaOptionView> options(Long tenantId, String category) {
        return catalog.options(safeTenantId(tenantId), category);
    }

    @Override
    public Map<String, List<MetaOptionView>> optionsBatch(Long tenantId, List<String> categories) {
        return catalog.optionsBatch(safeTenantId(tenantId), categories);
    }

    @Override
    public List<MetaOptionView> abExperiments(Long tenantId) {
        return catalog.abExperiments(safeTenantId(tenantId));
    }

    @Override
    public List<MetaOptionView> abExperimentGroups(Long tenantId, String experimentKey) {
        return catalog.abExperimentGroups(safeTenantId(tenantId), experimentKey);
    }

    @Override
    public List<MetaOptionView> bizLines(Long tenantId) {
        return catalog.bizLines(safeTenantId(tenantId));
    }

    @Override
    public List<MetaOptionView> bizLineApis(Long tenantId, String bizLineKey) {
        return catalog.bizLineApis(safeTenantId(tenantId), bizLineKey);
    }

    @Override
    public List<MetaOptionView> aiProviders(Long tenantId) {
        return catalog.aiProviders(safeTenantId(tenantId));
    }

    @Override
    public List<MetaOptionView> aiTemplates(Long tenantId) {
        return catalog.aiTemplates(safeTenantId(tenantId));
    }

    @Override
    public List<MetaOptionView> aiModels(Long tenantId, Long providerId) {
        return catalog.aiModels(safeTenantId(tenantId), providerId);
    }

    @Override
    public List<MetaOptionView> identityTypes(Integer allowImport) {
        return catalog.identityTypes(allowImport);
    }

    @Override
    public List<Map<String, Object>> apiDefinitions() {
        return catalog.apiDefinitions();
    }

    @Override
    public List<Map<String, Object>> eventDefinitions() {
        return catalog.eventDefinitions();
    }

    @Override
    public List<Map<String, Object>> contextFields() {
        return catalog.contextFields();
    }

    @Override
    public List<Map<String, Object>> canvasContextFields(
            List<String> eventCodes,
            List<String> apiKeys,
            List<String> outputPrefixes) {
        return catalog.canvasContextFields(eventCodes, apiKeys, outputPrefixes);
    }

    @Override
    public List<Map<String, Object>> mqDefinitions() {
        return catalog.mqDefinitions();
    }

    @Override
    public List<MetaOptionView> taggerTags(String type) {
        return catalog.taggerTags(type);
    }

    @Override
    public List<MetaOptionView> taggerTagValues(String tagCode) {
        return catalog.taggerTagValues(tagCode);
    }

    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }
}
