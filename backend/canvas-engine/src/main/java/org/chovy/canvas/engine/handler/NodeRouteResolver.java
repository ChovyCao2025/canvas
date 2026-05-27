package org.chovy.canvas.engine.handler;

import org.chovy.canvas.common.MapFieldKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Node Route Resolver 执行引擎基础类型。
 *
 * <p>定义节点执行结果、路由判断或处理器契约，是 DAG 调度与具体节点实现之间的稳定接口。
 * <p>该层不依赖具体业务节点，保持通用语义以便新增节点复用。
 */
public final class NodeRouteResolver {
    /**
     * 构造 NodeRouteResolver 实例。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     */
    private NodeRouteResolver() {
    }

    /**
     * 构建、解析或转换 resolve Targets 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param result result 方法执行所需的业务参数
     * @return 查询、转换或计算得到的结果集合
     */
    public static List<String> resolveTargets(NodeResult result) {
        List<String> targets = new ArrayList<>();
        if (result.routes() != null && !result.routes().isEmpty()) {
            // routes 是新版多分支出口，先收集显式分支，else 兜底最后追加以保持调度优先级。
            String elseTarget = resolveFallbackTarget(result);
            for (Map.Entry<String, String> entry : result.routes().entrySet()) {
                if (MapFieldKeys.ELSE.equals(entry.getKey())) {
                    continue;
                } else {
                    addIfPresent(targets, entry.getValue());
                }
            }
            addIfPresent(targets, elseTarget);
            return targets;
        }
        addIfPresent(targets, result.nextNodeId());
        addIfPresent(targets, result.successNodeId());
        addIfPresent(targets, result.failNodeId());
        addIfPresent(targets, result.elseNodeId());
        if (result.branchMap() != null) {
            // branchMap 是旧版多分支结构，保留兼容以避免存量节点丢失下游。
            for (Map.Entry<String, String> entry : result.branchMap().entrySet()) {
                addIfPresent(targets, entry.getValue());
            }
        }
        return targets;
    }

    /**
     * 构建、解析或转换 resolve Priority Branch Targets 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param result result 方法执行所需的业务参数
     * @return 查询、转换或计算得到的结果集合
     */
    public static List<String> resolvePriorityBranchTargets(NodeResult result) {
        List<String> targets = new ArrayList<>();
        if (result.routes() != null && !result.routes().isEmpty()) {
            for (Map.Entry<String, String> entry : result.routes().entrySet()) {
                if (!MapFieldKeys.ELSE.equals(entry.getKey())) {
                    // 优先级节点只返回候选分支，else 由所有候选失败后再处理。
                    addIfPresent(targets, entry.getValue());
                }
            }
            return targets;
        }
        if (result.branchMap() != null) {
            for (Map.Entry<String, String> entry : result.branchMap().entrySet()) {
                addIfPresent(targets, entry.getValue());
            }
        }
        return targets;
    }

    /**
     * 构建、解析或转换 resolve Fallback Target 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param result result 方法执行所需的业务参数
     * @return 转换或查询得到的字符串结果
     */
    public static String resolveFallbackTarget(NodeResult result) {
        String target = null;
        if (result.routes() != null && result.routes().containsKey(MapFieldKeys.ELSE)) {
            // 新版 routes 中的 else 优先级高于历史 elseNodeId。
            target = result.routes().get(MapFieldKeys.ELSE);
        } else {
            target = result.elseNodeId();
        }
        return target == null || target.isBlank() ? null : target;
    }

    /**
     * 创建或新增 add If Present 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param targets targets 方法执行所需的业务参数
     * @param target target 方法执行所需的业务参数
     */
    private static void addIfPresent(List<String> targets, String target) {
        if (target != null && !target.isBlank()) {
            targets.add(target);
        }
    }
}
