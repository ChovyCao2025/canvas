package org.chovy.canvas.conversation.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 为领域记录复制可变集合的工具类。
 */
final class DomainMaps {

    /**
     * 禁止实例化纯静态工具类。
     */
    private DomainMaps() {
    }

    /**
     * 复制 Map，避免外部调用方继续持有并修改领域对象内部状态。
     *
     * @param source 原始 Map
     * @return 可由领域对象独占的 Map 副本
     */
    static Map<String, Object> copy(Map<String, Object> source) {
        return source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
    }

    /**
     * 复制字符串列表，避免外部调用方继续持有并修改领域对象内部状态。
     *
     * @param source 原始字符串列表
     * @return 可由领域对象独占的列表副本
     */
    static List<String> copyList(List<String> source) {
        return source == null ? new ArrayList<>() : new ArrayList<>(source);
    }
}
