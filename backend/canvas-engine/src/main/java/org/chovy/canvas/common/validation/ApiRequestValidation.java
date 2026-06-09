package org.chovy.canvas.common.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 原始请求体接口的边界校验工具，用于在验签之后、业务处理之前执行 Bean Validation。
 */
public final class ApiRequestValidation {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * 执行 ApiRequestValidation 流程，围绕 api request validation 完成校验、计算或结果组装。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     */
    private ApiRequestValidation() {}

    /**
     * validate 校验或转换 common.validation 场景的数据。
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回布尔判断结果。
     */
    public static <T> T validate(T request) {
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(request);
        if (!violations.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message(violations));
        }
        return request;
    }

    /**
     * 将校验错误按字段路径排序后拼接为稳定错误文案。
     *
     * @param violations Bean Validation 校验错误集合
     * @return 面向接口响应的错误说明
     */
    private static <T> String message(Set<ConstraintViolation<T>> violations) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return violations.stream()
                .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .collect(Collectors.joining("; "));
    }
}
