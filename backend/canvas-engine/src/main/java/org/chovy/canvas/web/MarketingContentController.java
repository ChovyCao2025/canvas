package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.content.ContentEntryService;
import org.chovy.canvas.domain.content.ContentTemplateService;
import org.chovy.canvas.domain.content.MarketingAssetService;
import org.chovy.canvas.domain.content.MarketingAssetUploadService;
import org.chovy.canvas.domain.content.MarketingContentReleaseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

/**
 * MarketingContentController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/marketing/content")
public class MarketingContentController {

    /**
     * asset服务，用于承接对应业务能力和领域编排。
     */
    private final MarketingAssetService assetService;
    /**
     * 模板服务，用于承接对应业务能力和领域编排。
     */
    private final ContentTemplateService templateService;
    /**
     * entry服务，用于承接对应业务能力和领域编排。
     */
    private final ContentEntryService entryService;
    /**
     * upload服务，用于承接对应业务能力和领域编排。
     */
    private final MarketingAssetUploadService uploadService;
    /**
     * release服务，用于承接对应业务能力和领域编排。
     */
    private final MarketingContentReleaseService releaseService;
    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 MarketingContentController 实例并注入 web 场景依赖。
     * @param assetService 依赖组件，用于完成数据访问或外部能力调用。
     * @param templateService 依赖组件，用于完成数据访问或外部能力调用。
     * @param entryService 依赖组件，用于完成数据访问或外部能力调用。
     * @param uploadService 依赖组件，用于完成数据访问或外部能力调用。
     * @param releaseService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public MarketingContentController(MarketingAssetService assetService,
                                      ContentTemplateService templateService,
                                      ContentEntryService entryService,
                                      MarketingAssetUploadService uploadService,
                                      MarketingContentReleaseService releaseService,
                                      TenantContextResolver tenantContextResolver) {
        this.assetService = assetService;
        this.templateService = templateService;
        this.entryService = entryService;
        this.uploadService = uploadService;
        this.releaseService = releaseService;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 查询当前租户可见的素材文件夹，用于素材库导航和上传归类。
     * 租户来自请求上下文；没有有效租户时由上下文解析层拒绝访问。
     * 本接口不修改素材数据。
     *
     * @return 文件夹视图列表。
     */
    @GetMapping("/asset-folders")
    public Mono<R<List<MarketingAssetService.FolderView>>> assetFolders() {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(assetService.listFolders(context)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 在当前租户的素材库中创建文件夹，供后续素材上传或资产维护使用。
     * 文件夹名称、父级等约束由领域服务校验，创建结果仅在当前租户内可见。
     * 副作用是写入新的素材文件夹记录。
     *
     * @param command 文件夹创建参数。
     * @return 创建后的文件夹视图。
     */
    @PostMapping("/asset-folders")
    public Mono<R<MarketingAssetService.FolderView>> createAssetFolder(
            @RequestBody MarketingAssetService.FolderCommand command) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(assetService.createFolder(context, command)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 按关键字、素材类型和状态检索当前租户的营销素材。
     * 常用于素材选择器和素材管理页，查询结果不会跨租户泄露。
     * 本接口只读，不改变素材状态。
     *
     * @param keyword 搜索关键字，可选。
     * @param assetType 素材类型过滤条件，可选。
     * @param status 素材状态过滤条件，可选。
     * @return 匹配条件的素材列表。
     */
    @GetMapping("/assets")
    public Mono<R<List<MarketingAssetService.AssetView>>> assets(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String assetType,
            @RequestParam(required = false) String status) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(assetService.list(context, keyword, assetType, status)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 登记一条营销素材元数据，通常在上传完成后把素材地址、类型和归属信息写入素材库。
     * 租户上下文决定素材归属；调用方只能创建自己租户下的资产。
     * 副作用是新增素材记录。
     *
     * @param command 素材元数据和归类信息。
     * @return 创建后的素材视图。
     */
    @PostMapping("/assets")
    public Mono<R<MarketingAssetService.AssetView>> createAsset(@RequestBody MarketingAssetService.AssetCommand command) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(assetService.create(context, command)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 创建素材上传意图，返回客户端直传或后续提交所需的上传会话信息。
     * 上传意图绑定当前租户，后续上传完成和素材入库会以该租户为边界校验。
     * 副作用是写入一条待完成的上传会话。
     *
     * @param command 上传文件名、类型、大小和目标归类等信息。
     * @return 上传意图视图。
     */
    @PostMapping("/assets/upload-intents")
    public Mono<R<MarketingAssetUploadService.UploadIntentView>> createUploadIntent(
            @RequestBody MarketingAssetUploadService.UploadIntentCommand command) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(uploadService.createIntent(context, command)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 清理当前租户下长时间未完成的素材上传意图。
     * 可传入清理窗口等条件；未传时由领域服务使用默认过期策略。
     * 副作用是把符合条件的待上传会话置为过期，避免无效会话长期占用。
     *
     * @param command 清理条件，可选。
     * @return 上传意图清理结果。
     */
    @PostMapping("/assets/upload-intents/expire-stale")
    public Mono<R<MarketingAssetUploadService.UploadIntentCleanupResult>> expireStaleUploadIntents(
            @RequestBody(required = false) MarketingAssetUploadService.UploadIntentCleanupCommand command) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(uploadService.expireStalePendingUploads(context, command)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 调整指定营销素材的业务状态，例如启用、停用或下架。
     * 素材键必须属于当前租户；状态流转合法性由素材服务统一校验。
     * 副作用是更新素材状态，并可能记录状态变更信息。
     *
     * @param assetKey 素材业务键。
     * @param command 目标状态及变更原因。
     * @return 更新后的素材视图。
     */
    @PostMapping("/assets/{assetKey}/status")
    public Mono<R<MarketingAssetService.AssetView>> setAssetStatus(
            @PathVariable String assetKey,
            @RequestBody MarketingAssetService.AssetStatusCommand command) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(assetService.setStatus(context, assetKey, command)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询当前租户的内容模板，支持按关键字、渠道和状态筛选。
     * 用于模板管理、内容创建时的模板选择，以及发布前校验。
     * 本接口只读。
     *
     * @param keyword 搜索关键字，可选。
     * @param channel 渠道过滤条件，可选。
     * @param status 模板状态过滤条件，可选。
     * @return 模板视图列表。
     */
    @GetMapping("/templates")
    public Mono<R<List<ContentTemplateService.TemplateView>>> templates(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(templateService.list(context, keyword, channel, status)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 新建或更新当前租户的内容模板配置。
     * 模板结构、渠道适配和变量定义由领域服务校验；保存后可被内容条目引用。
     * 副作用是写入模板草稿或覆盖既有模板配置。
     *
     * @param command 模板主体、渠道、变量和状态等配置。
     * @return 保存后的模板视图。
     */
    @PostMapping("/templates")
    public Mono<R<ContentTemplateService.TemplateView>> saveTemplate(
            @RequestBody ContentTemplateService.TemplateCommand command) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(templateService.save(context, command)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 使用给定上下文预览模板渲染结果，便于发布前检查变量替换和渠道内容效果。
     * 模板键必须属于当前租户；预览输入只参与本次渲染，不保存为内容条目。
     * 本接口预期不改变模板状态。
     *
     * @param templateKey 模板业务键。
     * @param context 模板变量和预览上下文。
     * @return 模板预览结果。
     */
    @PostMapping("/templates/{templateKey}/preview")
    public Mono<R<ContentTemplateService.PreviewResult>> previewTemplate(
            @PathVariable String templateKey,
            @RequestBody Map<String, Object> context) {
        return tenantContextResolver.currentOrError()
                .flatMap(tenant -> Mono.fromCallable(() -> R.ok(templateService.preview(tenant, templateKey, context)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 修改内容模板状态，例如启用、停用或归档。
     * 模板必须归属当前租户；状态变更会影响后续内容创建和发布校验。
     * 副作用是更新模板状态，并可能生成审计记录。
     *
     * @param templateKey 模板业务键。
     * @param command 目标状态及变更说明。
     * @return 更新后的模板视图。
     */
    @PostMapping("/templates/{templateKey}/status")
    public Mono<R<ContentTemplateService.TemplateView>> setTemplateStatus(
            @PathVariable String templateKey,
            @RequestBody ContentTemplateService.TemplateStatusCommand command) {
        return tenantContextResolver.currentOrError()
                .flatMap(tenant -> Mono.fromCallable(() -> R.ok(templateService.setStatus(tenant, templateKey, command)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询当前租户的内容条目，可按标题关键字、内容类型和状态筛选。
     * 用于内容列表页和发布前定位待处理条目。
     * 本接口只读。
     *
     * @param keyword 搜索关键字，可选。
     * @param contentType 内容类型过滤条件，可选。
     * @param status 内容状态过滤条件，可选。
     * @return 内容条目列表。
     */
    @GetMapping("/entries")
    public Mono<R<List<ContentEntryService.EntryView>>> entries(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) String status) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(entryService.list(context, keyword, contentType, status)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 保存内容条目草稿，可创建新草稿或覆盖当前租户下已有草稿。
     * 草稿不会直接进入发布态，后续需通过发布接口推进状态。
     * 副作用是写入内容草稿及其关联素材、模板引用。
     *
     * @param command 内容正文、类型、模板和素材引用等草稿信息。
     * @return 保存后的内容条目视图。
     */
    @PostMapping("/entries")
    public Mono<R<ContentEntryService.EntryView>> saveEntry(@RequestBody ContentEntryService.EntryCommand command) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(entryService.saveDraft(context, command)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 将指定内容条目从草稿或可发布状态推进到发布态。
     * 内容条目必须属于当前租户；领域服务会校验模板、素材和状态流转是否合法。
     * 副作用是更新内容条目发布状态。
     *
     * @param entryKey 内容条目业务键。
     * @param command 发布状态参数和操作说明。
     * @return 发布后的内容条目视图。
     */
    @PostMapping("/entries/{entryKey}/publish")
    public Mono<R<ContentEntryService.EntryView>> publishEntry(
            @PathVariable String entryKey,
            @RequestBody ContentEntryService.EntryStatusCommand command) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(entryService.publish(context, entryKey, command)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 归档当前租户下的内容条目，使其退出后续编辑和发布流程。
     * 归档目标由内容业务键定位，状态流转约束由领域服务校验。
     * 副作用是更新条目状态为归档相关状态。
     *
     * @param entryKey 内容条目业务键。
     * @param command 归档状态参数和操作说明。
     * @return 归档后的内容条目视图。
     */
    @PostMapping("/entries/{entryKey}/archive")
    public Mono<R<ContentEntryService.EntryView>> archiveEntry(
            @PathVariable String entryKey,
            @RequestBody ContentEntryService.EntryStatusCommand command) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(entryService.archive(context, entryKey, command)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 校验内容发布请求是否满足发布条件。
     * 校验在当前租户边界内执行，会检查发布来源、目标渠道、内容状态等约束。
     * 本接口用于发布前预检，正常情况下不创建发布单。
     *
     * @param command 待发布来源、渠道和校验上下文。
     * @return 发布校验结果。
     */
    @PostMapping("/releases/validate")
    public Mono<R<MarketingContentReleaseService.ValidationResult>> validateRelease(
            @RequestBody MarketingContentReleaseService.ValidationCommand command) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(releaseService.validate(context, command)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 创建并执行内容发布单，将内容推送到指定营销渠道或目标系统。
     * 发布来源必须属于当前租户；发布服务负责校验状态、生成发布记录并推进发布流程。
     * 副作用是写入发布单、更新发布状态，并可能记录审计事件。
     *
     * @param command 发布来源、目标渠道和执行参数。
     * @return 发布单视图。
     */
    @PostMapping("/releases/publish")
    public Mono<R<MarketingContentReleaseService.ReleaseView>> publishRelease(
            @RequestBody MarketingContentReleaseService.ReleaseCommand command) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(releaseService.publish(context, command)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询当前租户的内容发布单。
     * 支持按发布来源类型、来源业务键和发布状态筛选，用于发布历史和故障排查。
     * 本接口只读。
     *
     * @param sourceType 发布来源类型，可选。
     * @param sourceKey 发布来源业务键，可选。
     * @param status 发布状态过滤条件，可选。
     * @return 发布单列表。
     */
    @GetMapping("/releases")
    public Mono<R<List<MarketingContentReleaseService.ReleaseView>>> releases(
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String sourceKey,
            @RequestParam(required = false) String status) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(releaseService.list(context, sourceType, sourceKey, status)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 解析指定发布单的当前结果，补齐渠道回执、失败原因或最终发布状态。
     * 发布单必须属于当前租户；缺省上下文按空参数处理。
     * 副作用可能包括更新发布单状态和写入审计事件。
     *
     * @param releaseKey 发布单业务键。
     * @param context 渠道回执或解析上下文，可选。
     * @return 发布解析结果。
     */
    @PostMapping("/releases/{releaseKey}/resolve")
    public Mono<R<MarketingContentReleaseService.ResolvedRelease>> resolveRelease(
            @PathVariable String releaseKey,
            @RequestBody(required = false) Map<String, Object> context) {
        return tenantContextResolver.currentOrError()
                .flatMap(tenant -> Mono.fromCallable(() -> R.ok(releaseService.resolve(
                                tenant,
                                releaseKey,
                                // 允许调用方省略回执上下文，由发布服务按空上下文解析当前发布单。
                                context == null ? Map.of() : context)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 回滚指定内容发布单，用于撤销或修正已发布内容。
     * 发布单必须属于当前租户；能否回滚取决于发布状态和目标渠道能力。
     * 副作用是推进发布单到回滚相关状态，并可能触发渠道撤回动作。
     *
     * @param releaseKey 发布单业务键。
     * @param command 回滚原因和渠道参数。
     * @return 回滚后的发布单视图。
     */
    @PostMapping("/releases/{releaseKey}/rollback")
    public Mono<R<MarketingContentReleaseService.ReleaseView>> rollbackRelease(
            @PathVariable String releaseKey,
            @RequestBody MarketingContentReleaseService.RollbackCommand command) {
        return tenantContextResolver.currentOrError()
                .flatMap(tenant -> Mono.fromCallable(() -> R.ok(releaseService.rollback(tenant, releaseKey, command)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询当前租户的内容发布审计事件。
     * 可按目标类型和目标业务键定位某个模板、内容条目或发布单的操作轨迹。
     * 本接口只读，用于审计追踪和问题排查。
     *
     * @param targetType 审计目标类型，可选。
     * @param targetKey 审计目标业务键，可选。
     * @param limit 返回数量上限，可选。
     * @return 审计事件列表。
     */
    @GetMapping("/audit-events")
    public Mono<R<List<MarketingContentReleaseService.AuditEventView>>> auditEvents(
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String targetKey,
            @RequestParam(required = false) Integer limit) {
        return tenantContextResolver.currentOrError()
                .flatMap(tenant -> Mono.fromCallable(() -> R.ok(releaseService.auditEvents(tenant, targetType, targetKey, limit)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
}
