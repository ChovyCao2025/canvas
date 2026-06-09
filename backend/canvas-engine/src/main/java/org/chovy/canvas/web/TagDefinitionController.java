package org.chovy.canvas.web;

import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.dal.dataobject.TagDefinitionDO;
import org.chovy.canvas.domain.meta.TagDefinitionService;
import org.chovy.canvas.dal.dataobject.TagValueDefinitionDO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * 标签定义管理接口。
 *
 * 作用：
 * - 维护标签元数据（tagCode/tagName/tagType/启停）；
 * - 供标签节点配置时下拉选择，不直接参与标签值计算。
 */
@RestController
@RequestMapping("/canvas/tag-definitions")
@RequiredArgsConstructor
public class TagDefinitionController {

    /** 标签定义服务，用于管理标签元数据。 */
    private final TagDefinitionService tagDefinitionService;

    /** 分页查询标签定义。 */
    @GetMapping
    public Mono<R<PageResult<TagDefinitionDO>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String tagType,
            @RequestParam(required = false) Integer enabled) {
        return Mono.fromCallable(() -> tagDefinitionService.page(page, size, tagType, enabled))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    /** 创建标签定义。 */
    @PostMapping
    public Mono<R<TagDefinitionDO>> create(@RequestBody TagDefinitionDO body) {
        return Mono.fromCallable(() -> {
                    return R.ok(tagDefinitionService.create(body));
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /** 更新标签定义。 */
    @PutMapping("/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody TagDefinitionDO body) {
        return Mono.<Void>fromRunnable(() -> tagDefinitionService.update(id, body))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.ok());
    }

    /** 删除标签定义。 */
    @DeleteMapping("/{id}")
    public Mono<R<Void>> delete(@PathVariable Long id) {
        return Mono.<Void>fromRunnable(() -> tagDefinitionService.delete(id))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.ok());
    }
    /**
     * 查询标签定义列表接口，对应 GET /{tagCode}/values。
     * 接口在控制器或服务层执行资源权限校验后再处理请求。
     * 主要委托 tagDefinitionService.listValues 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param tagCode 标签编码。
     * @param enabled 请求参数，可选。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/{tagCode}/values")
    public Mono<R<List<TagValueDefinitionDO>>> listValues(
            @PathVariable String tagCode,
            @RequestParam(required = false) Integer enabled) {
        return Mono.fromCallable(() -> tagDefinitionService.listValues(tagCode, enabled))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    /**
     * createValue 创建或触发 web 场景的业务处理。
     * @param tagCode 业务编码，用于匹配对应类型或状态。
     * @param body 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回流程执行后的业务结果。
     */
    @PostMapping("/{tagCode}/values")
    public Mono<R<TagValueDefinitionDO>> createValue(@PathVariable String tagCode, @RequestBody TagValueDefinitionDO body) {
        return Mono.fromCallable(() -> tagDefinitionService.createValue(tagCode, body))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    /**
     * 处理 update Value 对应的 HTTP 接口请求。
     *
     * <p>方法负责接收控制层参数、调用领域服务并封装统一响应。
     *
     * @param id id 对应的业务主键或标识
     * @param body body 请求体、消息体或事件载荷
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @PutMapping("/values/{id}")
    public Mono<R<Void>> updateValue(@PathVariable Long id, @RequestBody TagValueDefinitionDO body) {
        return Mono.<Void>fromRunnable(() -> tagDefinitionService.updateValue(id, body))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.ok());
    }

    /**
     * 处理 delete Value 对应的 HTTP 接口请求。
     *
     * <p>方法负责接收控制层参数、调用领域服务并封装统一响应。
     *
     * @param id id 对应的业务主键或标识
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @DeleteMapping("/values/{id}")
    public Mono<R<Void>> deleteValue(@PathVariable Long id) {
        return Mono.<Void>fromRunnable(() -> tagDefinitionService.deleteValue(id))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.ok());
    }
}
