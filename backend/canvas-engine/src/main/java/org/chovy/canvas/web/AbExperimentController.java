package org.chovy.canvas.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.dal.dataobject.AbExperimentDO;
import org.chovy.canvas.dal.dataobject.AbExperimentGroupDO;
import org.chovy.canvas.domain.meta.AbExperimentGroupService;
import org.chovy.canvas.dal.mapper.AbExperimentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * AB 实验定义管理控制器。
 *
 * 定位：
 * - 管理实验元数据（名称、分流配置、启停状态）；
 * - 不负责实验流量分配，分配逻辑在执行引擎节点中完成。
 */
@RestController
@RequestMapping("/canvas/ab-experiments")
@RequiredArgsConstructor
public class AbExperimentController {

    /** AB 实验表访问层。 */
    private final AbExperimentMapper abExperimentMapper;
    /** AB 实验分组服务，用于维护实验默认分组。 */
    private final AbExperimentGroupService abExperimentGroupService;

    /**
     * 分页查询 AB 实验列表
     * @param page 页码
     * @param size 每页大小
     * @param enabled 启用状态（可选）
     * @return 分页结果
     */
    @GetMapping
    public Mono<R<PageResult<AbExperimentDO>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer enabled) {
        return Mono.fromCallable(() -> {
            // 默认按 ID 倒序，便于运营优先看到最近维护的实验
            LambdaQueryWrapper<AbExperimentDO> wrapper = new LambdaQueryWrapper<AbExperimentDO>()
                    .orderByDesc(AbExperimentDO::getId);
            if (enabled != null) {
                wrapper.eq(AbExperimentDO::getEnabled, enabled);
            }
            Page<AbExperimentDO> pageResult = abExperimentMapper.selectPage(new Page<>(page, size), wrapper);
            return PageResult.of(pageResult.getTotal(), pageResult.getRecords());
        }).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
    }

    /**
     * 创建 AB 实验
     * @param body 实验定义对象
     * @return 创建结果
     */
    @PostMapping
    public Mono<R<AbExperimentDO>> create(@RequestBody AbExperimentDO body) {
        return Mono.fromCallable(() -> {
            // 默认启用：新建后可立即被 SPLIT 节点选择
            if (body.getEnabled() == null) body.setEnabled(1);
            abExperimentMapper.insert(body);
            abExperimentGroupService.ensureDefaultGroups(body.getId());
            return body;
        }).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
    }

    /**
     * 更新 AB 实验
     * @param id 实验 ID
     * @param body 实验信息
     * @return 成功响应
     */
    @PutMapping("/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody AbExperimentDO body) {
        return Mono.<Void>fromRunnable(() -> {
            // 强制使用路径参数，防止请求体带错 id 更新到其他记录
            body.setId(id);
            abExperimentMapper.updateById(body);
        }).subscribeOn(Schedulers.boundedElastic()).thenReturn(R.<Void>ok());
    }

    /**
     * 删除 AB 实验
     * @param id 实验 ID
     * @return 成功响应
     */
    @DeleteMapping("/{id}")
    public Mono<R<Void>> delete(@PathVariable Long id) {
        return Mono.<Void>fromRunnable(() -> abExperimentMapper.deleteById(id))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.<Void>ok());
    }
    /**
     * 查询 A/B 实验列表接口，对应 GET /{id}/groups。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 主要委托 abExperimentGroupService.list 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @param includeDisabled 请求参数，默认值为 false。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/{id}/groups")
    public Mono<R<List<AbExperimentGroupDO>>> listGroups(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean includeDisabled) {
        return Mono.fromCallable(() -> abExperimentGroupService.list(id, includeDisabled))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    /**
     * 处理 create Group 对应的 HTTP 接口请求。
     *
     * <p>方法负责接收控制层参数、调用领域服务并封装统一响应。
     *
     * @param id id 对应的业务主键或标识
     * @param body body 请求体、消息体或事件载荷
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @PostMapping("/{id}/groups")
    public Mono<R<AbExperimentGroupDO>> createGroup(
            @PathVariable Long id,
            @RequestBody AbExperimentGroupDO body) {
        return Mono.fromCallable(() -> abExperimentGroupService.create(id, body))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    /**
     * 处理 update Group 对应的 HTTP 接口请求。
     *
     * <p>方法负责接收控制层参数、调用领域服务并封装统一响应。
     *
     * @param id id 对应的业务主键或标识
     * @param groupId groupId 对应的业务主键或标识
     * @param body body 请求体、消息体或事件载荷
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @PutMapping("/{id}/groups/{groupId}")
    public Mono<R<Void>> updateGroup(
            @PathVariable Long id,
            @PathVariable Long groupId,
            @RequestBody AbExperimentGroupDO body) {
        return Mono.<Void>fromRunnable(() -> abExperimentGroupService.update(id, groupId, body))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.ok());
    }

    /**
     * 处理 delete Group 对应的 HTTP 接口请求。
     *
     * <p>方法负责接收控制层参数、调用领域服务并封装统一响应。
     *
     * @param id id 对应的业务主键或标识
     * @param groupId groupId 对应的业务主键或标识
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @DeleteMapping("/{id}/groups/{groupId}")
    public Mono<R<Void>> deleteGroup(@PathVariable Long id, @PathVariable Long groupId) {
        return Mono.<Void>fromRunnable(() -> abExperimentGroupService.disable(id, groupId))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.ok());
    }

}
