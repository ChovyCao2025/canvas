package org.chovy.canvas.marketing.api;

import java.util.List;
import java.util.Map;

public interface MetaOptionFacade {

    List<MetaOptionView> options(Long tenantId, String category);

    Map<String, List<MetaOptionView>> optionsBatch(Long tenantId, List<String> categories);

    List<MetaOptionView> abExperiments(Long tenantId);

    List<MetaOptionView> abExperimentGroups(Long tenantId, String experimentKey);

    List<MetaOptionView> bizLines(Long tenantId);

    List<MetaOptionView> bizLineApis(Long tenantId, String bizLineKey);

    List<MetaOptionView> aiProviders(Long tenantId);

    List<MetaOptionView> aiTemplates(Long tenantId);

    List<MetaOptionView> aiModels(Long tenantId, Long providerId);

    List<MetaOptionView> identityTypes(Integer allowImport);

    List<Map<String, Object>> apiDefinitions();

    List<Map<String, Object>> eventDefinitions();

    List<Map<String, Object>> contextFields();

    List<Map<String, Object>> canvasContextFields(
            List<String> eventCodes,
            List<String> apiKeys,
            List<String> outputPrefixes);

    List<Map<String, Object>> mqDefinitions();

    List<MetaOptionView> taggerTags(String type);

    List<MetaOptionView> taggerTagValues(String tagCode);
}
