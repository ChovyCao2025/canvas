package org.chovy.canvas.engine.handlers;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

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
