package org.chovy.canvas.marketing.api;

import java.util.List;
import java.util.Map;

/**
 * 定义MetaOptionFacade的营销上下文访问契约。
 */
public interface MetaOptionFacade {

    /**
     * 执行options业务操作。
     */
    List<MetaOptionView> options(Long tenantId, String category);

    /**
     * 执行optionsBatch业务操作。
     */
    Map<String, List<MetaOptionView>> optionsBatch(Long tenantId, List<String> categories);

    /**
     * 执行abExperiments业务操作。
     */
    List<MetaOptionView> abExperiments(Long tenantId);

    /**
     * 执行abExperimentGroups业务操作。
     */
    List<MetaOptionView> abExperimentGroups(Long tenantId, String experimentKey);

    /**
     * 执行bizLines业务操作。
     */
    List<MetaOptionView> bizLines(Long tenantId);

    /**
     * 执行bizLineApis业务操作。
     */
    List<MetaOptionView> bizLineApis(Long tenantId, String bizLineKey);

    /**
     * 执行aiProviders业务操作。
     */
    List<MetaOptionView> aiProviders(Long tenantId);

    /**
     * 执行aiTemplates业务操作。
     */
    List<MetaOptionView> aiTemplates(Long tenantId);

    /**
     * 执行aiModels业务操作。
     */
    List<MetaOptionView> aiModels(Long tenantId, Long providerId);

    /**
     * 执行identityTypes业务操作。
     */
    List<MetaOptionView> identityTypes(Integer allowImport);

    /**
     * 执行apiDefinitions业务操作。
     */
    List<Map<String, Object>> apiDefinitions();

    /**
     * 执行eventDefinitions业务操作。
     */
    List<Map<String, Object>> eventDefinitions();

    /**
     * 执行contextFields业务操作。
     */
    List<Map<String, Object>> contextFields();

    /**
     * 执行canvasContextFields业务操作。
     */
    List<Map<String, Object>> canvasContextFields(
            List<String> eventCodes,
            List<String> apiKeys,
            List<String> outputPrefixes);

    /**
     * 执行mqDefinitions业务操作。
     */
    List<Map<String, Object>> mqDefinitions();

    /**
     * 执行taggerTags业务操作。
     */
    List<MetaOptionView> taggerTags(String type);

    /**
     * 执行taggerTagValues业务操作。
     */
    List<MetaOptionView> taggerTagValues(String tagCode);
}
