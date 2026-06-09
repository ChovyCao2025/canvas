package org.chovy.canvas.engine.llm;

public class LlmInvalidJsonException extends RuntimeException {

    public LlmInvalidJsonException(String message) {
        super(message);
    }

    public LlmInvalidJsonException(String message, Throwable cause) {
        super(message, cause);
    }
}
