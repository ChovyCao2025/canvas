package org.chovy.canvas.engine.lane;

/**
 * ExecutionLane 枚举 engine.lane 场景中的固定业务取值。
 */
public enum ExecutionLane {
    LIGHT,
    STANDARD,
    HEAVY,
    RETRY;

    /**
     * key 处理 engine.lane 场景的业务逻辑。
     * @return 返回 key 生成的文本或业务键。
     */
    public String key() {
        return name().toLowerCase();
    }
}
