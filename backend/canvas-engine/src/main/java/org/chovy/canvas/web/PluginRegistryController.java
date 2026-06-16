package org.chovy.canvas.web;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.engine.plugin.PluginRegistryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

/**
 * PluginRegistryController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/canvas/plugins")
@RequiredArgsConstructor
public class PluginRegistryController {

    /**
     * 服务，用于承接对应业务能力和领域编排。
     */
    private final PluginRegistryService service;
    /**
     * 查询插件注册表目录接口，对应 GET 请求。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping
    public Mono<R<Map<String, List<PluginRegistryService.Plugin>>>> catalog() {
        return Mono.fromCallable(service::groupedCatalog)
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }
    /**
     * 处理 插件注册表 请求接口，对应 PUT /{pluginKey}/enabled。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param pluginKey plugin 唯一键。
     * @param canvasVersion 请求头参数，默认值为 1.0.0。
     * @param request 请求体。
     * @return 异步返回统一响应，表示操作完成。
     */
    @PutMapping("/{pluginKey}/enabled")
    public Mono<R<Void>> setEnabled(
            @PathVariable String pluginKey,
            @RequestHeader(name = "X-Canvas-Version", defaultValue = "1.0.0") String canvasVersion,
            @RequestBody EnableRequest request) {
        return Mono.<Void>fromRunnable(() -> service.setEnabled(pluginKey, request.enabled(), canvasVersion))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.ok());
    }

    /**
     * EnableRequest 数据记录。
     */
    public static final class EnableRequest {

        /**
         * 启用状态。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("enabled")
        private final boolean enabled;

        /**
         * 创建 EnableRequest 实例。
         *
         * @param enabled 启用状态
         */
        @com.fasterxml.jackson.annotation.JsonCreator
        public EnableRequest(@com.fasterxml.jackson.annotation.JsonProperty("enabled") boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 返回启用状态。
         *
         * @return 启用状态
         */
        public boolean enabled() {
            return enabled;
        }

        /**
         * 判断两个 EnableRequest 实例是否包含相同字段值。
         *
         * @param o 待比较对象
         * @return 字段值全部一致时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof EnableRequest that)) {
                return false;
            }
            return java.util.Objects.equals(enabled, that.enabled);
        }

        /**
         * 根据全部字段生成哈希值。
         *
         * @return 字段哈希值
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(enabled);
        }

        /**
         * 返回与原记录形态一致的调试字符串。
         *
         * @return 字段调试字符串
         */
        @Override
        public String toString() {
            return "EnableRequest[" + "enabled=" + enabled + "]";
        }
    }
}
