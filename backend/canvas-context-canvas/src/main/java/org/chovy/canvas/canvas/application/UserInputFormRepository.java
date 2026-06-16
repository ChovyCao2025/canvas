package org.chovy.canvas.canvas.application;

import org.chovy.canvas.canvas.domain.UserInputForm;

/**
 * 定义UserInputFormRepository对外提供的能力契约。
 */
public interface UserInputFormRepository {

    /**
     * 保存。
     */
    UserInputForm save(UserInputForm form);
}
