package org.chovy.canvas.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.meta.ApiDefinition;
import org.chovy.canvas.domain.meta.ApiDefinitionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;


/**
 * API 定义管理控制器：
 * 提供 API 定义的分页查询、创建、更新、删除接口。
 *
 * 角色定位：
 * - 这是配置中心接口，不直接发起真实业务 API 调用；
 * - 运行时节点（如 API_CALL）会按 apiKey/配置引用这些定义。
 */
@RestController
@RequestMapping("/canvas/api-definitions")
@RequiredArgsConstructor
public class ApiDefinitionController {

    /** API 定义表访问层。 */
    private final ApiDefinitionMapper apiDefinitionMapper;

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
            // 默认按 ID 倒序，保证最近维护的配置排在前面
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
            // 默认启用：新建后可立即被配置面板选择
            if (body.getEnabled() == null) body.setEnabled(1);
            apiDefinitionMapper.insert(body);
            return body;
        }).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
    }

    /**
     * 更新 API 定义
     * @param id API 定义 ID
     * @param body API 定义信息
     * @return 成功响应
     */
    @PutMapping("/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody ApiDefinition body) {
        return Mono.<Void>fromRunnable(() -> {
            // URL 中的 id 优先，避免请求体篡改跨记录更新
            body.setId(id);
            apiDefinitionMapper.updateById(body);
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

}
