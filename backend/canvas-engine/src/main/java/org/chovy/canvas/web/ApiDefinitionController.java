package org.chovy.canvas.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.OutboundUrlValidator;
import org.chovy.canvas.common.R;
import org.chovy.canvas.dal.dataobject.ApiDefinitionDO;
import org.chovy.canvas.dal.mapper.ApiDefinitionMapper;
import org.chovy.canvas.infrastructure.cache.ApiDefinitionCache;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;


/**
 * 接口定义 HTTP 控制器，根路由为 {@code /canvas/api-definitions}。
 *
 * <p>负责接收前端或外部系统请求，完成参数绑定、基础校验和统一响应包装。
 * <p>具体业务规则委托给领域服务处理，控制器层保持薄封装以减少重复逻辑。
 */
@RestController
@RequestMapping("/canvas/api-definitions")
@RequiredArgsConstructor
public class ApiDefinitionController {

    /** API 定义 Mapper，用于读写接口元数据。 */
    private final ApiDefinitionMapper apiDefinitionMapper;
    /** API 定义缓存，用于刷新接口配置。 */
    private final ApiDefinitionCache apiDefinitionCache;
    /** JSON 转换器，用于请求体节点与实体互转。 */
    private final ObjectMapper objectMapper;

    /**
     * 分页查询 API 定义列表
     * @param page 页码
     * @param size 每页大小
     * @param enabled 启用状态（可选）
     * @return 分页结果
     */
    @GetMapping
    public Mono<R<PageResult<ApiDefinitionDO>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer enabled) {
        return Mono.fromCallable(() -> {
            LambdaQueryWrapper<ApiDefinitionDO> wrapper = new LambdaQueryWrapper<ApiDefinitionDO>()
                    .orderByDesc(ApiDefinitionDO::getId);
            if (enabled != null) {
                wrapper.eq(ApiDefinitionDO::getEnabled, enabled);
            }
            Page<ApiDefinitionDO> pageResult = apiDefinitionMapper.selectPage(new Page<>(page, size), wrapper);
            return PageResult.of(pageResult.getTotal(), pageResult.getRecords());
        }).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
    }

    /**
     * 创建 API 定义
     * @param body API 定义对象
     * @return 创建结果
     */
    @PostMapping
    public Mono<R<ApiDefinitionDO>> create(@RequestBody ApiDefinitionDO body) {
        return Mono.fromCallable(() -> {
            // API_CALL 节点会按该 URL 发起出站请求，入库前先做协议和内网地址校验。
            validateOutboundUrl(body, true);
            validateRateLimit(body);
            // 兼容前端未传的开关字段，避免数据库默认值和更新语义不一致。
            if (body.getEnabled() == null) body.setEnabled(1);
            if (body.getIncludeContextPayload() == null) body.setIncludeContextPayload(0);
            if (body.getReceiptEnabled() == null) body.setReceiptEnabled(0);
            if (body.getReceiptExpireMinutes() == null) body.setReceiptExpireMinutes(1440);
            if (body.getReceiptStatuses() == null) body.setReceiptStatuses("[]");
            apiDefinitionMapper.insert(body);
            apiDefinitionCache.invalidate(body.getApiKey());
            return body;
        }).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
    }

    /**
     * 更新 API 定义
     * @param id API 定义 ID
     * @param bodyNode API 定义信息
     * @return 成功响应
     */
    @PutMapping("/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody JsonNode bodyNode) {
        return Mono.<Void>fromRunnable(() -> {
            ApiDefinitionDO body = objectMapper.convertValue(bodyNode, ApiDefinitionDO.class);
            // 更新时只有显式传 url 才校验，支持局部更新其他字段。
            validateOutboundUrl(body, false);
            validateRateLimit(body);
            if (hasExplicitNullRateLimit(bodyNode)) {
                // MyBatis-Plus updateById 默认不会把 null 写入数据库，需单独构造 set null。
                body.setId(null);
                apiDefinitionMapper.update(body,
                        new LambdaUpdateWrapper<ApiDefinitionDO>()
                                .eq(ApiDefinitionDO::getId, id)
                                .set(ApiDefinitionDO::getRateLimitPerSec, null));
            } else {
                body.setId(id);
                apiDefinitionMapper.updateById(body);
            }
            apiDefinitionCache.invalidateAll();
        }).subscribeOn(Schedulers.boundedElastic()).thenReturn(R.ok());
    }

    /**
     * 删除 API 定义
     * @param id API 定义 ID
     * @return 成功响应
     */
    @DeleteMapping("/{id}")
    public Mono<R<Void>> delete(@PathVariable Long id) {
        return Mono.<Void>fromRunnable(() -> {
                apiDefinitionMapper.deleteById(id);
                apiDefinitionCache.invalidateAll();
            })
            .subscribeOn(Schedulers.boundedElastic())
            .thenReturn(R.ok());
    }

    /**
     * 校验 validate Rate Limit 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param body body 请求体、消息体或事件载荷
     */
    private static void validateRateLimit(ApiDefinitionDO body) {
        if (body.getRateLimitPerSec() != null && body.getRateLimitPerSec() <= 0) {
            throw new IllegalArgumentException("rateLimitPerSec 必须大于 0");
        }
    }

    /**
     * 校验 validate Outbound Url 相关的业务数据。
     *
     * <p>实现会调用外部 HTTP 服务，并对响应或异常进行业务化处理。
     *
     * @param body body 请求体、消息体或事件载荷
     * @param required required 方法执行所需的业务参数
     */
    private static void validateOutboundUrl(ApiDefinitionDO body, boolean required) {
        if (required || body.getUrl() != null) {
            // 统一走 common 校验器，避免各入口对 SSRF 防护规则产生分叉。
            OutboundUrlValidator.validateHttpUrl(body.getUrl());
        }
    }

    /**
     * 判断 has Explicit Null Rate Limit 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param bodyNode bodyNode 节点相关对象、标识或配置
     * @return 判断结果，true 表示校验通过或条件成立
     */
    private static boolean hasExplicitNullRateLimit(JsonNode bodyNode) {
        return bodyNode.has("rateLimitPerSec") && bodyNode.get("rateLimitPerSec").isNull();
    }
}
