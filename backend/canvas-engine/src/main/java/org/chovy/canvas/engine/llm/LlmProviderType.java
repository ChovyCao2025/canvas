package org.chovy.canvas.engine.llm;

/**
 * LlmProviderType 参与 engine.llm 场景的画布执行引擎处理。
 */
public final class LlmProviderType {

    public static final String MOCK = "MOCK";
    public static final String OPENAI_COMPATIBLE = "OPENAI_COMPATIBLE";
    public static final String CUSTOM_OPENAI_COMPATIBLE = "CUSTOM_OPENAI_COMPATIBLE";

    /**
     * 常量类不允许实例化。
     */
    private LlmProviderType() {
    }
}
