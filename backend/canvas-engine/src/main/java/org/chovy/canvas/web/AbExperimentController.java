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
            // 默认启用：新建后可立即被 AB_SPLIT 节点选择
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

    @GetMapping("/{id}/groups")
    public Mono<R<List<AbExperimentGroupDO>>> listGroups(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean includeDisabled) {
        return Mono.fromCallable(() -> abExperimentGroupService.list(id, includeDisabled))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    @PostMapping("/{id}/groups")
    public Mono<R<AbExperimentGroupDO>> createGroup(
            @PathVariable Long id,
            @RequestBody AbExperimentGroupDO body) {
        return Mono.fromCallable(() -> abExperimentGroupService.create(id, body))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    @PutMapping("/{id}/groups/{groupId}")
    public Mono<R<Void>> updateGroup(
            @PathVariable Long id,
            @PathVariable Long groupId,
            @RequestBody AbExperimentGroupDO body) {
        return Mono.<Void>fromRunnable(() -> abExperimentGroupService.update(id, groupId, body))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.ok());
    }

    @DeleteMapping("/{id}/groups/{groupId}")
    public Mono<R<Void>> deleteGroup(@PathVariable Long id, @PathVariable Long groupId) {
        return Mono.<Void>fromRunnable(() -> abExperimentGroupService.disable(id, groupId))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.ok());
    }

}
