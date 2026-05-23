package org.chovy.canvas.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.common.R;
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

/**
 * 元数据聚合控制器：
 * 为前端节点配置面板提供下拉选项、schema 和上下文字段能力。
 */
@Slf4j
@RestController
@RequestMapping("/meta")
@RequiredArgsConstructor
public class MetaController {

    private final MetaService metaService;
    private final ApiDefinitionMapper apiDefinitionMapper;
    private final AbExperimentMapper abExperimentMapper;
    private final TagDefinitionMapper tagDefinitionMapper;
    private final MqMessageDefinitionMapper mqMapper;
    private final EventDefinitionMapper eventDefinitionMapper;
    private final ObjectMapper objectMapper;
    private final SystemOptionService systemOptionService;
    private final AbExperimentGroupService abExperimentGroupService;

    @Value("${canvas.integration.tagger-service-url}")
    private String taggerUrl;

    /**
     * 获取所有注册的画布节点类型定义
     *
     * @return 节点类型定义列表
     */
    @GetMapping("/node-types")
    public Mono<R<List<NodeTypeRegistry>>> getNodeTypes() {
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
    public Mono<R<NodeTypeRegistry>> getNodeTypeSchema(@PathVariable String typeKey) {
        return Mono.fromCallable(() -> metaService.getNodeTypeSchema(typeKey))
                .subscribeOn(Schedulers.boundedElastic())
                .map(nt -> nt != null ? R.ok(nt) : R.<NodeTypeRegistry>fail("节点类型不存在: " + typeKey));
    }

    /**
     * 获取全局上下文中的所有可引用字段
     *
     * @return 字段列表
     */
    @GetMapping("/context-fields")
    public Mono<R<List<ContextField>>> getContextFields() {
        return Mono.fromCallable(metaService::getAllContextFields)
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    @GetMapping("/options")
    public Mono<R<List<StubOption>>> getOptions(@RequestParam String category) {
        return Mono.fromCallable(() -> systemOptionService.activeOptions(category))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    @GetMapping("/options/batch")
    public Mono<R<Map<String, List<StubOption>>>> getOptionsBatch(@RequestParam List<String> categories) {
        return Mono.fromCallable(() -> categories.stream()
                        .distinct()
                        .collect(Collectors.toMap(
                                category -> category,
                                systemOptionService::activeOptions,
                                (left, right) -> left,
                                java.util.LinkedHashMap::new)))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    /**
     * 获取 MQ 触发器可选主题列表
     *
     * @return 选项列表
     */
    @GetMapping("/mq-topics")
    public Mono<R<List<StubOption>>> getMqTopics() {
        return Mono.fromCallable(() -> systemOptionService.activeOptions("mq_topic_legacy"))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    /**
     * MQ 消息定义列表（带 requestSchema，供 SEND_MQ 节点动态渲染参数）
     */
    @GetMapping("/mq-definitions")
    public Mono<R<List<Map<String, Object>>>> getMqDefinitions() {
        return Mono.fromCallable(() -> {
            List<org.chovy.canvas.domain.meta.MqMessageDefinition> defs =
                    mqMapper.selectList(
                            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                                    org.chovy.canvas.domain.meta.MqMessageDefinition>()
                                    .eq(org.chovy.canvas.domain.meta.MqMessageDefinition::getEnabled, 1)
                                    .orderByAsc(org.chovy.canvas.domain.meta.MqMessageDefinition::getId));
            return defs.stream().map(d -> {
                Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("value", d.getMessageCode());
                m.put("label", d.getName());
                m.put("requestSchema", d.getRequestSchema() != null ? d.getRequestSchema() : "[]");
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
        return Mono.fromCallable(() -> systemOptionService.activeOptions("coupon_type"))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    /**
     * 获取用户触达场景列表
     *
     * @return 选项列表
     */
    @GetMapping("/reach-scenes")
    public Mono<R<List<StubOption>>> getReachScenes() {
        return Mono.fromCallable(() -> systemOptionService.activeOptions("reach_scene"))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    /**
     * 获取所有已启用的 AB 实验列表
     *
     * @return 选项列表
     */
    @GetMapping("/ab-experiments")
    public Mono<R<List<StubOption>>> getAbExperiments() {
        return Mono.fromCallable(() -> {
            List<AbExperiment> experiments = abExperimentMapper.selectList(
                    new LambdaQueryWrapper<AbExperiment>()
                            .eq(AbExperiment::getEnabled, 1)
                            .orderByAsc(AbExperiment::getId)
            );
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
                    AbExperiment experiment = abExperimentMapper.selectOne(
                            new LambdaQueryWrapper<AbExperiment>()
                                    .eq(AbExperiment::getExperimentKey, key)
                                    .eq(AbExperiment::getEnabled, 1)
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
            List<ApiDefinition> defs = apiDefinitionMapper.selectList(
                    new LambdaQueryWrapper<ApiDefinition>()
                            .eq(ApiDefinition::getEnabled, 1)
                            .orderByAsc(ApiDefinition::getId)
            );
            return defs.stream().map(def -> {
                Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("value", def.getApiKey());
                m.put("label", def.getName());
                m.put("requestSchema", def.getRequestSchema() != null ? def.getRequestSchema() : "[]");
                m.put("includeContextPayload", def.getIncludeContextPayload() != null ? def.getIncludeContextPayload() : 0);
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
                    List<org.chovy.canvas.domain.meta.TagDefinition> defs =
                            tagDefinitionMapper.selectList(
                                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                                            org.chovy.canvas.domain.meta.TagDefinition>()
                                            .eq(org.chovy.canvas.domain.meta.TagDefinition::getTagType, type)
                                            .eq(org.chovy.canvas.domain.meta.TagDefinition::getEnabled, 1)
                                            .orderByAsc(org.chovy.canvas.domain.meta.TagDefinition::getId));
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
                    return WebClient.builder().baseUrl(taggerUrl).build()
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

    /** 获取业务线列表。 */
    @GetMapping("/biz-lines")
    public Mono<R<List<StubOption>>> getBizLines() {
        return Mono.fromCallable(() -> systemOptionService.activeOptions("biz_line"))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    /**
     * 事件定义列表（带 attributes schema，供 EVENT_TRIGGER 节点下拉选择）
     */
    @GetMapping("/event-definitions")
    public Mono<R<List<Map<String, Object>>>> getEventDefinitions() {
        return Mono.fromCallable(() -> {
            List<org.chovy.canvas.domain.meta.EventDefinition> defs =
                    eventDefinitionMapper.selectList(
                            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                                    org.chovy.canvas.domain.meta.EventDefinition>()
                                    .eq(org.chovy.canvas.domain.meta.EventDefinition::getEnabled, 1)
                                    .orderByAsc(org.chovy.canvas.domain.meta.EventDefinition::getId));
            return defs.stream().map(d -> {
                Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("value", d.getEventCode());
                m.put("label", d.getName());
                m.put("requestSchema", d.getAttributes() != null ? d.getAttributes() : "[]");
                return m;
            }).collect(Collectors.toList());
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic()).map(R::ok);
    }

    /** 获取业务线对应可选 API 列表。 */
    @GetMapping("/biz-lines/{key}/apis")
    public Mono<R<List<StubOption>>> getBizLineApis(@PathVariable String key) {
        return Mono.fromCallable(() -> systemOptionService.activeOptions("biz_line_api"))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    /** 获取行为策略类型列表。 */
    @GetMapping("/behavior-strategy-types")
    public Mono<R<List<StubOption>>> getBehaviorStrategyTypes() {
        return Mono.fromCallable(() -> systemOptionService.activeOptions("behavior_strategy_type"))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    /** 按类型获取消息编码列表。 */
    @GetMapping("/message-codes")
    public Mono<R<List<StubOption>>> getMessageCodes(
            @RequestParam(defaultValue = "IN_APP") String type) {
        String category = "MQ".equals(type) ? "message_code_mq" : "message_code_in_app";
        return Mono.fromCallable(() -> systemOptionService.activeOptions(category))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    /**
     * 根据画布中使用的事件/API 节点动态推导可用上下文字段。
     * 供 IF_CONDITION、SELECTOR 等节点的条件规则面板使用。
     *
     * @param eventCodes     EVENT_TRIGGER 节点的 eventCode 列表
     * @param apiKeys        API_CALL 节点的 apiKey 列表（与 outputPrefixes 按索引对应）
     * @param outputPrefixes API_CALL 节点的 outputPrefix 列表（与 apiKeys 按索引对应）
     */
    @GetMapping("/canvas-context-fields")
    @SuppressWarnings("unchecked")
    public Mono<R<List<ContextField>>> getCanvasContextFields(
            @RequestParam(required = false) List<String> eventCodes,
            @RequestParam(required = false) List<String> apiKeys,
            @RequestParam(required = false) List<String> outputPrefixes) {
        return Mono.fromCallable(() -> {
            List<ContextField> fields = new ArrayList<>();

            // 1. 基础触发字段（所有画布共有）
            fields.add(field("userId", "用户ID", "STRING", "trigger"));

            // 2. 从事件定义解析属性字段
            if (eventCodes != null && !eventCodes.isEmpty()) {
                List<EventDefinition> events = eventDefinitionMapper.selectList(
                        new LambdaQueryWrapper<EventDefinition>()
                                .in(EventDefinition::getEventCode, eventCodes));
                for (EventDefinition evt : events) {
                    if (evt.getAttributes() == null) continue;
                    try {
                        List<Map<String, Object>> attrs =
                                objectMapper.readValue(evt.getAttributes(), List.class);
                        for (Map<String, Object> a : attrs) {
                            String key = str(a.get("name"));
                            String name = str(a.getOrDefault("displayName", a.get("name")));
                            String type = str(a.getOrDefault("type", "STRING"));
                            fields.add(field(key, name + "（" + evt.getName() + "）", type, "EVENT_TRIGGER"));
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            // 3. 从 API response_schema 解析输出字段
            if (apiKeys != null && !apiKeys.isEmpty()) {
                List<ApiDefinition> apis = apiDefinitionMapper.selectList(
                        new LambdaQueryWrapper<ApiDefinition>()
                                .in(ApiDefinition::getApiKey, apiKeys)
                                .eq(ApiDefinition::getEnabled, 1));
                Map<String, ApiDefinition> apiMap = apis.stream()
                        .collect(Collectors.toMap(ApiDefinition::getApiKey, a -> a));

                for (int i = 0; i < apiKeys.size(); i++) {
                    ApiDefinition def = apiMap.get(apiKeys.get(i));
                    if (def == null || def.getResponseSchema() == null) continue;
                    String prefix = (outputPrefixes != null && i < outputPrefixes.size()
                            && !outputPrefixes.get(i).isBlank())
                            ? outputPrefixes.get(i) + "." : "";
                    try {
                        List<Map<String, Object>> schema =
                                objectMapper.readValue(def.getResponseSchema(), List.class);
                        for (Map<String, Object> f : schema) {
                            String key = prefix + str(f.get("name"));
                            String desc = str(f.getOrDefault("desc", f.get("name")));
                            String type = str(f.getOrDefault("type", "STRING"));
                            fields.add(field(key, desc + "（" + def.getName() + "）", type, "API_CALL"));
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            return fields;
        }).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
    }

    private static ContextField field(String key, String name, String type, String source) {
        ContextField f = new ContextField();
        f.setFieldKey(key);
        f.setFieldName(name);
        f.setDataType(type);
        f.setSourceNodeType(source);
        return f;
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}
