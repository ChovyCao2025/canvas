package org.chovy.canvas.execution.domain;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 定义 NodeHandlerType 的执行上下文数据结构或业务契约。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NodeHandlerType {

    /**
     * 执行 value 对应的业务处理。
     * @return 处理后的结果
     */
    String value();
}
