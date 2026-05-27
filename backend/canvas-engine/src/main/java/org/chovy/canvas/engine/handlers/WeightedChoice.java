package org.chovy.canvas.engine.handlers;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 权重选择工具。
 *
 * <p>根据候选项中的 weight 字段完成随机或稳定分桶选择，用于随机分流、实验分组等需要按比例路由的节点。
 * <p>stable 模式下使用 seed 做确定性分桶，保证同一用户或同一上下文在多次执行中落到相同分支。
 */
final class WeightedChoice {
    private WeightedChoice() {
    }

    static Map<String, Object> choose(List<Map<String, Object>> items, String seed, boolean stable) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        int total = items.stream().mapToInt(item -> weight(item)).sum();
        if (total <= 0) {
            return items.getFirst();
        }
        int bucket = stable
                ? Math.floorMod(seed == null ? 0 : seed.hashCode(), total)
                : ThreadLocalRandom.current().nextInt(total);
        int cursor = 0;
        for (Map<String, Object> item : items) {
            cursor += weight(item);
            if (bucket < cursor) {
                return item;
            }
        }
        return items.getLast();
    }

    private static int weight(Map<String, Object> item) {
        Object weight = item.getOrDefault("weight", 1);
        return weight instanceof Number number ? Math.max(0, number.intValue()) : 1;
    }
}
