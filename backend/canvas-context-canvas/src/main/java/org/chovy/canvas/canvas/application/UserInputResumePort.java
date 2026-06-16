package org.chovy.canvas.canvas.application;

/**
 * 定义UserInputResumePort对外提供的能力契约。
 */
public interface UserInputResumePort {

    /**
     * 处理requestResume。
     */
    void requestResume(UserInputResumeRequest request);
}
