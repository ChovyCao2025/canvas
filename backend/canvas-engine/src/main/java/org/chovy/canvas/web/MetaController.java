package org.chovy.canvas.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.ai.AiPromptTemplateService;
import org.chovy.canvas.domain.ai.AiProviderModelRegistryService;
import org.chovy.canvas.domain.meta.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.AbExperimentDO;
import org.chovy.canvas.dal.mapper.AbExperimentMapper;
import org.chovy.canvas.dal.dataobject.ApiDefinitionDO;
import org.chovy.canvas.dal.mapper.ApiDefinitionMapper;
import org.chovy.canvas.dal.dataobject.ContextFieldDO;
import org.chovy.canvas.dal.dataobject.EventDefinitionDO;
import org.chovy.canvas.dal.mapper.EventDefinitionMapper;
import org.chovy.canvas.dal.dataobject.MqMessageDefinitionDO;
import org.chovy.canvas.dal.mapper.MqMessageDefinitionMapper;
import org.chovy.canvas.dal.dataobject.NodeTypeRegistryDO;
import org.chovy.canvas.dal.dataobject.TagDefinitionDO;
import org.chovy.canvas.dto.StubOption;

/**
 * 元数据聚合控制器：
 * 为前端节点配置面板提供下拉选项、schema 和上下文字段能力。
 */
@Slf4j
@RestController
@RequestMapping("/meta")
@RequiredArgsConstructor
public class MetaController {

    /** 元数据服务，用于查询节点类型和数据源元信息。 */
    private final MetaService metaService;
    /** API 定义 Mapper，用于提供 API 节点配置选项。 */
    private final ApiDefinitionMapper apiDefinitionMapper;
    /** AB 实验 Mapper，用于提供 AB 节点配置选项。 */
    private final AbExperimentMapper abExperimentMapper;
    /** 标签定义服务，用于提供标签节点配置选项。 */
    private final TagDefinitionService tagDefinitionService;
    /** MQ 消息定义 Mapper，用于提供 MQ 节点配置选项。 */
    private final MqMessageDefinitionMapper mqMapper;
    /** 事件定义 Mapper，用于提供事件触发配置选项。 */
    private final EventDefinitionMapper eventDefinitionMapper;
    /** 身份类型服务，用于提供身份字段配置选项。 */
    private final IdentityTypeService identityTypeService;
    /** JSON 转换器，用于解析节点参数 schema。 */
    private final ObjectMapper objectMapper;
    /** 系统选项服务，用于读取枚举类配置。 */
    private final SystemOptionService systemOptionService;
    /** AB 实验分组服务，用于查询实验分组选项。 */
    private final AbExperimentGroupService abExperimentGroupService;
    private final TenantContextResolver tenantContextResolver;
    private final AiProviderModelRegistryService aiProviderModelRegistryService;
    private final AiPromptTemplateService aiPromptTemplateService;
    /** 统一 HTTP 客户端构建器，继承全局超时、连接池和响应大小限制。 */
    private final WebClient.Builder webClientBuilder;

    /** 标签服务地址，用于生成外部标签查询配置。 */
    @Value("${canvas.integration.tagger-service-url}")
    private String taggerUrl;

    /**
     * 获取所有注册的画布节点类型定义
     *
     * @return 节点类型定义列表
     */
    @GetMapping("/node-types")
    public Mono<R<List<NodeTypeRegistryDO>>> getNodeTypes() {
        return Mono.fromCallable(metaService::getAllNodeTypes)
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    /**
     * 获取指定节点类型的 Schema 配置
     *
     * @param typeKey 节点类型 Key
     * @return 节点类型定义
     */
    @GetMapping("/node-types/{typeKey}/schema")
    public Mono<R<NodeTypeRegistryDO>> getNodeTypeSchema(@PathVariable String typeKey) {
        return Mono.fromCallable(() -> metaService.getNodeTypeSchema(typeKey))
                .subscribeOn(Schedulers.boundedElastic())
                .map(nt -> nt != null ? R.ok(nt) : R.<NodeTypeRegistryDO>fail("节点类型不存在: " + typeKey));
    }

    /**
     * 获取全局上下文中的所有可引用字段
     *
     * @return 字段列表
     */
    @GetMapping("/context-fields")
    public Mono<R<List<ContextFieldDO>>> getContextFields() {
        return Mono.fromCallable(metaService::getAllContextFields)
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    /**
     * getOptions 查询 web 场景的业务数据。
     * @param category category 参数，用于 getOptions 流程中的校验、计算或对象转换。
     * @return 返回 get options 汇总后的集合、分页或映射视图。
     */
    @GetMapping("/options")
    public Mono<R<List<StubOption>>> getOptions(@RequestParam String category) {
        return currentTenant()
                .flatMap(context -> Mono.fromCallable(() -> systemOptionService.activeOptions(
                                category, context.tenantId()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }

    /**
     * getOptionsBatch 查询 web 场景的业务数据。
     * @param categories categories 参数，用于 getOptionsBatch 流程中的校验、计算或对象转换。
     * @return 返回 get options batch 汇总后的集合、分页或映射视图。
     */
    @GetMapping("/options/batch")
    public Mono<R<Map<String, List<StubOption>>>> getOptionsBatch(@RequestParam List<String> categories) {
        return currentTenant()
                .flatMap(context -> Mono.fromCallable(() -> categories.stream()
                                .distinct()
                                // 去重后按传入顺序返回，前端可一次加载多个下拉配置。
                                .collect(Collectors.toMap(
                                        category -> category,
                                        category -> systemOptionService.activeOptions(
                                                category, context.tenantId()),
                                        (left, right) -> left,
                                        java.util.LinkedHashMap::new)))
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }

    /**
     * 获取 MQ 触发器可选主题列表
     *
     * @return 选项列表
     */
    @GetMapping("/mq-topics")
    public Mono<R<List<StubOption>>> getMqTopics() {
        return currentTenant()
                .flatMap(context -> Mono.fromCallable(() -> systemOptionService.activeOptions(
                                "mq_topic_legacy", context.tenantId()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }
    /**
     * 获取Meta详情接口，对应 GET /ai-providers。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/ai-providers")
    public Mono<R<List<StubOption>>> getAiProviders() {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return currentTenant()
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .flatMap(context -> Mono.fromCallable(() -> aiProviderModelRegistryService
                                .listProviders(context.tenantId())
                                .stream()
                                .filter(AiProviderModelRegistryService.ProviderView::enabled)
                                .map(provider -> new StubOption(
                                        String.valueOf(provider.id()),
                                        provider.displayName() + " (" + provider.providerKey() + ")"))
                                .toList())
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }
    /**
     * 获取Meta详情接口，对应 GET /ai-templates。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/ai-templates")
    public Mono<R<List<StubOption>>> getAiTemplates() {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return currentTenant()
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .flatMap(context -> Mono.fromCallable(() -> aiPromptTemplateService
                                .listTemplates(context.tenantId())
                                .stream()
                                .map(template -> new StubOption(
                                        String.valueOf(template.id()),
                                        template.name() + " (" + template.category() + ")"))
                                .toList())
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }
    /**
     * 获取Meta详情接口，对应 GET /ai-models。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param providerId provider ID，可选。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/ai-models")
    public Mono<R<List<StubOption>>> getAiModels(@RequestParam(required = false) Long providerId) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return currentTenant()
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .flatMap(context -> Mono.fromCallable(() -> aiProviderModelRegistryService
                                .listModels(context.tenantId(), providerId)
                                .stream()
                                .filter(AiProviderModelRegistryService.ModelView::enabled)
                                .map(model -> new StubOption(model.modelKey(), model.displayName()))
                                .toList())
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }

    /**
     * MQ 消息定义列表（带 requestSchema，供 SEND_MQ 节点动态渲染参数）
     */
    @GetMapping("/mq-definitions")
    public Mono<R<List<Map<String, Object>>>> getMqDefinitions() {
        return Mono.fromCallable(() -> {
            List<org.chovy.canvas.dal.dataobject.MqMessageDefinitionDO> defs =
                    // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                    mqMapper.selectList(
                            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                                    org.chovy.canvas.dal.dataobject.MqMessageDefinitionDO>()
                                    .eq(org.chovy.canvas.dal.dataobject.MqMessageDefinitionDO::getEnabled, 1)
                                    .orderByAsc(org.chovy.canvas.dal.dataobject.MqMessageDefinitionDO::getId));
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            return defs.stream().map(d -> {
                Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put(MapFieldKeys.VALUE, d.getMessageCode());
                m.put(MapFieldKeys.LABEL, d.getName());
                m.put(MapFieldKeys.REQUEST_SCHEMA, d.getRequestSchema() != null ? d.getRequestSchema() : "[]");
                // 汇总前面计算出的状态和明细，返回给调用方。
                return m;
            }).collect(Collectors.toList());
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic()).map(R::ok);
    }

    /**
     * 获取优惠券发放类型列表
     *
     * @return 选项列表
     */
    @GetMapping("/coupon-types")
    public Mono<R<List<StubOption>>> getCouponTypes() {
        return currentTenant()
                .flatMap(context -> Mono.fromCallable(() -> systemOptionService.activeOptions(
                                "coupon_type", context.tenantId()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }

    /**
     * 获取用户触达场景列表
     *
     * @return 选项列表
     */
    @GetMapping("/reach-scenes")
    public Mono<R<List<StubOption>>> getReachScenes() {
        return currentTenant()
                .flatMap(context -> Mono.fromCallable(() -> systemOptionService.activeOptions(
                                "reach_scene", context.tenantId()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }

    /**
     * 获取所有已启用的 AB 实验列表
     *
     * @return 选项列表
     */
    @GetMapping("/ab-experiments")
    public Mono<R<List<StubOption>>> getAbExperiments() {
        return Mono.fromCallable(() -> {
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            List<AbExperimentDO> experiments = abExperimentMapper.selectList(
                    new LambdaQueryWrapper<AbExperimentDO>()
                            .eq(AbExperimentDO::getEnabled, 1)
                            .orderByAsc(AbExperimentDO::getId)
            );
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            return experiments.stream()
                    .map(e -> new StubOption(e.getExperimentKey(), e.getName()))
                    .collect(Collectors.toList());
        }).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
    }

    /**
     * 获取指定 AB 实验的分组列表
     *
     * @param key 实验 Key
     * @return 分组选项列表
     */
    @GetMapping("/ab-experiments/{key}/groups")
    public Mono<R<List<StubOption>>> getAbExperimentGroups(@PathVariable String key) {
        return Mono.fromCallable(() -> {
                    AbExperimentDO experiment = abExperimentMapper.selectOne(
                            new LambdaQueryWrapper<AbExperimentDO>()
                                    .eq(AbExperimentDO::getExperimentKey, key)
                                    .eq(AbExperimentDO::getEnabled, 1)
                                    .last("LIMIT 1"));
                    if (experiment == null) {
                        return List.<StubOption>of();
                    }
                    return abExperimentGroupService.activeGroupOptions(experiment.getId());
                })
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    /**
     * 获取所有已启用的 API 定义列表
     *
     * @return 选项列表
     */
    @GetMapping("/api-definitions")
    public Mono<R<List<Map<String, Object>>>> getApiDefinitions() {
        return Mono.fromCallable(() -> {
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            List<ApiDefinitionDO> defs = apiDefinitionMapper.selectList(
                    new LambdaQueryWrapper<ApiDefinitionDO>()
                            .eq(ApiDefinitionDO::getEnabled, 1)
                            .orderByAsc(ApiDefinitionDO::getId)
            );
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            return defs.stream().map(def -> {
                Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put(MapFieldKeys.VALUE, def.getApiKey());
                m.put(MapFieldKeys.LABEL, def.getName());
                m.put(MapFieldKeys.REQUEST_SCHEMA, def.getRequestSchema() != null ? def.getRequestSchema() : "[]");
                m.put(MapFieldKeys.INCLUDE_CONTEXT_PAYLOAD, def.getIncludeContextPayload() != null ? def.getIncludeContextPayload() : 0);
                // 汇总前面计算出的状态和明细，返回给调用方。
                return m;
            }).collect(Collectors.toList());
        }).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
    }


    /**
     * 动态获取 Tagger 标签列表（代理调 Tagger 服务）。
     * Tagger 服务不可用时降级返回 stub 数据，不影响页面加载。
     */
    @GetMapping("/tagger-tags")
    public Mono<R<List<StubOption>>> getTaggerTags(
            @RequestParam(defaultValue = "offline") String type) {
        // 优先从 tag_definition 表读取（管理员自定义标签）
        return Mono.fromCallable(() -> {
                    // 本地标签表有数据时直接返回，避免每次打开配置面板都依赖外部 Tagger 服务。
                    List<org.chovy.canvas.dal.dataobject.TagDefinitionDO> defs =
                            tagDefinitionService.list(type, 1);
                    if (!defs.isEmpty()) {
                        return defs.stream()
                                .map(d -> new StubOption(d.getTagCode(), d.getName()))
                                .collect(java.util.stream.Collectors.toList());
                    }
                    return null; // 空则 fallback Tagger 服务
                }).subscribeOn(Schedulers.boundedElastic())
                .flatMap(dbList -> {
                    if (dbList != null) return Mono.just(R.ok(dbList));
                    // fallback: 调 Tagger 服务
                    // 外部服务失败时降级为空列表，保持元数据页面可用。
                    return webClientBuilder.clone().baseUrl(taggerUrl).build()
                            .get()
                            .uri(u -> u.path("/tags").queryParam("type", type).build())
                            .retrieve()
                            .bodyToFlux(Map.class)
                            .map(m -> new StubOption(
                                    String.valueOf(m.getOrDefault("code", "")),
                                    String.valueOf(m.getOrDefault("name", ""))))
                            .collectList()
                            .map(R::ok)
                            .onErrorResume(e -> {
                                log.warn("[META] Tagger 标签拉取失败，返回空标签列表: {}", e.getMessage());
                                return Mono.just(R.ok(List.of()));
                            });
                });
    }

    /**
     * getTaggerTagValues 查询 web 场景的业务数据。
     * @param tagCode 业务编码，用于匹配对应类型或状态。
     * @return 返回 get tagger tag values 汇总后的集合、分页或映射视图。
     */
    @GetMapping("/tagger-tag-values")
    public Mono<R<List<StubOption>>> getTaggerTagValues(@RequestParam String tagCode) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return Mono.fromCallable(() -> tagDefinitionService.listValues(tagCode, 1).stream()
                        .map(item -> new StubOption(item.getValue(), item.getLabel()))
                        .collect(Collectors.toList()))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    /** 获取业务线列表。 */
    @GetMapping("/biz-lines")
    public Mono<R<List<StubOption>>> getBizLines() {
        return currentTenant()
                .flatMap(context -> Mono.fromCallable(() -> systemOptionService.activeOptions(
                                "biz_line", context.tenantId()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }
    /**
     * 获取Meta详情接口，对应 GET /identity-types。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 主要委托 identityTypeService.list 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param allowImport 请求参数，默认值为 1。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/identity-types")
    public Mono<R<List<StubOption>>> getIdentityTypes(
            @RequestParam(defaultValue = "1") Integer allowImport) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return Mono.fromCallable(() -> identityTypeService.list(1, allowImport).stream()
                        .map(item -> new StubOption(item.getCode(), item.getName()))
                        .collect(Collectors.toList()))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    /**
     * 事件定义列表（带 attributes schema，供 EVENT_TRIGGER 节点下拉选择）
     */
    @GetMapping("/event-definitions")
    public Mono<R<List<Map<String, Object>>>> getEventDefinitions() {
        return Mono.fromCallable(() -> {
            List<org.chovy.canvas.dal.dataobject.EventDefinitionDO> defs =
                    // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                    eventDefinitionMapper.selectList(
                            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                                    org.chovy.canvas.dal.dataobject.EventDefinitionDO>()
                                    .eq(org.chovy.canvas.dal.dataobject.EventDefinitionDO::getEnabled, 1)
                                    .orderByAsc(org.chovy.canvas.dal.dataobject.EventDefinitionDO::getId));
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            return defs.stream().map(d -> {
                Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put(MapFieldKeys.VALUE, d.getEventCode());
                m.put(MapFieldKeys.LABEL, d.getName());
                m.put(MapFieldKeys.REQUEST_SCHEMA, d.getAttributes() != null ? d.getAttributes() : "[]");
                // 汇总前面计算出的状态和明细，返回给调用方。
                return m;
            }).collect(Collectors.toList());
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic()).map(R::ok);
    }

    /** 获取业务线对应可选 API 列表。 */
    @GetMapping("/biz-lines/{key}/apis")
    public Mono<R<List<StubOption>>> getBizLineApis(@PathVariable String key) {
        return currentTenant()
                .flatMap(context -> Mono.fromCallable(() -> systemOptionService.activeOptions(
                                "biz_line_api", context.tenantId()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }

    /** 获取行为策略类型列表。 */
    @GetMapping("/behavior-strategy-types")
    public Mono<R<List<StubOption>>> getBehaviorStrategyTypes() {
        return currentTenant()
                .flatMap(context -> Mono.fromCallable(() -> systemOptionService.activeOptions(
                                "behavior_strategy_type", context.tenantId()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }

    /** 按类型获取消息编码列表。 */
    @GetMapping("/message-codes")
    public Mono<R<List<StubOption>>> getMessageCodes(
            @RequestParam(defaultValue = "IN_APP") String type) {
        String category = "MQ".equals(type) ? "message_code_mq" : "message_code_in_app";
        return currentTenant()
                .flatMap(context -> Mono.fromCallable(() -> systemOptionService.activeOptions(
                                category, context.tenantId()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }

    /**
     * 根据画布中使用的事件/API 节点动态推导可用上下文字段。
     * 供 IF_CONDITION 等节点的条件规则面板使用。
     *
     * @param eventCodes     EVENT_TRIGGER 节点的 eventCode 列表
     * @param apiKeys        API_CALL 节点的 apiKey 列表（与 outputPrefixes 按索引对应）
     * @param outputPrefixes API_CALL 节点的 outputPrefix 列表（与 apiKeys 按索引对应）
     */
    @GetMapping("/canvas-context-fields")
    @SuppressWarnings("unchecked")
    public Mono<R<List<ContextFieldDO>>> getCanvasContextFields(
            @RequestParam(required = false) List<String> eventCodes,
            @RequestParam(required = false) List<String> apiKeys,
            @RequestParam(required = false) List<String> outputPrefixes) {
        return Mono.fromCallable(() -> {
            List<ContextFieldDO> fields = new ArrayList<>();

            // 1. 基础触发字段（所有画布共有）
            fields.add(field("userId", "用户ID", "STRING", "trigger"));

            // 2. 从事件定义解析属性字段
            if (eventCodes != null && !eventCodes.isEmpty()) {
                List<EventDefinitionDO> events = eventDefinitionMapper.selectList(
                        new LambdaQueryWrapper<EventDefinitionDO>()
                                .in(EventDefinitionDO::getEventCode, eventCodes));
                for (EventDefinitionDO evt : events) {
                    if (evt.getAttributes() == null) continue;
                    try {
                        // 事件属性 schema 是 JSON 数组，解析失败时跳过该事件，不影响其他字段推导。
                        List<Map<String, Object>> attrs =
                                objectMapper.readValue(evt.getAttributes(), List.class);
                        for (Map<String, Object> a : attrs) {
                            String key = str(a.get("name"));
                            String name = str(a.getOrDefault("displayName", a.get("name")));
                            String type = str(a.getOrDefault("type", "STRING"));
                            fields.add(field(key, name + "（" + evt.getName() + "）", type, "EVENT_TRIGGER"));
                        }
                    // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
                    } catch (Exception ignored) {
                    }
                }
            }

            // 3. 从 API response_schema 解析输出字段
            if (apiKeys != null && !apiKeys.isEmpty()) {
                List<ApiDefinitionDO> apis = apiDefinitionMapper.selectList(
                        new LambdaQueryWrapper<ApiDefinitionDO>()
                                .in(ApiDefinitionDO::getApiKey, apiKeys)
                                .eq(ApiDefinitionDO::getEnabled, 1));
                Map<String, ApiDefinitionDO> apiMap = apis.stream()
                        .collect(Collectors.toMap(ApiDefinitionDO::getApiKey, a -> a));

                for (int i = 0; i < apiKeys.size(); i++) {
                    ApiDefinitionDO def = apiMap.get(apiKeys.get(i));
                    if (def == null || def.getResponseSchema() == null) continue;
                    String prefix = (outputPrefixes != null && i < outputPrefixes.size()
                            && !outputPrefixes.get(i).isBlank())
                            ? outputPrefixes.get(i) + "." : "";
                    try {
                        // API response_schema 结合 outputPrefix 生成可在后续节点引用的扁平字段名。
                        List<Map<String, Object>> schema =
                                objectMapper.readValue(def.getResponseSchema(), List.class);
                        for (Map<String, Object> f : schema) {
                            String key = prefix + str(f.get("name"));
                            String desc = str(f.getOrDefault("desc", f.get("name")));
                            String type = str(f.getOrDefault("type", "STRING"));
                            fields.add(field(key, desc + "（" + def.getName() + "）", type, "API_CALL"));
                        }
                    // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
                    } catch (Exception ignored) {
                    }
                }
            }

            return fields;
        }).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
    }

    /**
     * 执行 field 流程，围绕 field 完成校验、计算或结果组装。
     *
     * @param key 业务键，用于在同一租户下定位资源。
     * @param name 名称文本，用于展示或唯一性校验。
     * @param type 类型标识，用于选择对应处理分支。
     * @param source source 参数，用于 field 流程中的校验、计算或对象转换。
     * @return 返回 field 流程生成的业务结果。
     */
    private static ContextFieldDO field(String key, String name, String type, String source) {
        ContextFieldDO f = new ContextFieldDO();
        f.setFieldKey(key);
        f.setFieldName(name);
        f.setDataType(type);
        f.setSourceNodeType(source);
        return f;
    }

    /**
     * 获取当前请求的登录上下文或租户信息。
     *
     * @return 返回 currentTenant 流程生成的业务结果。
     */
    private Mono<TenantContext> currentTenant() {
        return tenantContextResolver.current();
    }

    /**
     * 执行 str 流程，围绕 str 完成校验、计算或结果组装。
     *
     * @param o o 参数，用于 str 流程中的校验、计算或对象转换。
     * @return 返回 str 生成的文本或业务键。
     */
    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}
