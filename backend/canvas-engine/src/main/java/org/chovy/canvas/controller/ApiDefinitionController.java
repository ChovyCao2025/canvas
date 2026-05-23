package org.chovy.canvas.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.meta.ApiDefinition;
import org.chovy.canvas.domain.meta.ApiDefinitionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;


@RestController
@RequestMapping("/canvas/api-definitions")
@RequiredArgsConstructor
public class ApiDefinitionController {

    private final ApiDefinitionMapper apiDefinitionMapper;
    private final ObjectMapper objectMapper;

    /**
     * 分页查询 API 定义列表
     * @param page 页码
     * @param size 每页大小
     * @param enabled 启用状态（可选）
     * @return 分页结果
     */
    @GetMapping
    public Mono<R<PageResult<ApiDefinition>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer enabled) {
        return Mono.fromCallable(() -> {
            LambdaQueryWrapper<ApiDefinition> wrapper = new LambdaQueryWrapper<ApiDefinition>()
                    .orderByDesc(ApiDefinition::getId);
            if (enabled != null) {
                wrapper.eq(ApiDefinition::getEnabled, enabled);
            }
            Page<ApiDefinition> pageResult = apiDefinitionMapper.selectPage(new Page<>(page, size), wrapper);
            return PageResult.of(pageResult.getTotal(), pageResult.getRecords());
        }).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
    }

    /**
     * 创建 API 定义
     * @param body API 定义对象
     * @return 创建结果
     */
    @PostMapping
    public Mono<R<ApiDefinition>> create(@RequestBody ApiDefinition body) {
        return Mono.fromCallable(() -> {
            validateRateLimit(body);
            if (body.getEnabled() == null) body.setEnabled(1);
            apiDefinitionMapper.insert(body);
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
            ApiDefinition body = objectMapper.convertValue(bodyNode, ApiDefinition.class);
            validateRateLimit(body);
            if (hasExplicitNullRateLimit(bodyNode)) {
                body.setId(null);
                apiDefinitionMapper.update(body,
                        new LambdaUpdateWrapper<ApiDefinition>()
                                .eq(ApiDefinition::getId, id)
                                .set(ApiDefinition::getRateLimitPerSec, null));
            } else {
                body.setId(id);
                apiDefinitionMapper.updateById(body);
            }
        }).subscribeOn(Schedulers.boundedElastic()).thenReturn(R.ok());
    }

    /**
     * 删除 API 定义
     * @param id API 定义 ID
     * @return 成功响应
     */
    @DeleteMapping("/{id}")
    public Mono<R<Void>> delete(@PathVariable Long id) {
        return Mono.<Void>fromRunnable(() -> apiDefinitionMapper.deleteById(id))
            .subscribeOn(Schedulers.boundedElastic())
            .thenReturn(R.ok());
    }

    private static void validateRateLimit(ApiDefinition body) {
        if (body.getRateLimitPerSec() != null && body.getRateLimitPerSec() <= 0) {
            throw new IllegalArgumentException("rateLimitPerSec 必须大于 0");
        }
    }

    private static boolean hasExplicitNullRateLimit(JsonNode bodyNode) {
        return bodyNode.has("rateLimitPerSec") && bodyNode.get("rateLimitPerSec").isNull();
    }
}
