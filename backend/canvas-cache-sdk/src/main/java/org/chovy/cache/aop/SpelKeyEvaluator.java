package org.chovy.cache.aop;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;

/**
 * SpEL 缓存 key 计算器。
 *
 * <p>根据方法签名、参数名和注解表达式生成稳定缓存键，支持以业务参数组合缓存维度。
 * <p>该类集中封装 Spring Expression 解析细节，避免各个切面重复处理参数上下文。
 */
public class SpelKeyEvaluator {
    /** SpEL 表达式解析器。 */
    private final ExpressionParser parser = new SpelExpressionParser();
    /** 方法参数名发现器，用于暴露真实参数名变量。 */
    private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    /**
     * 执行 evaluate 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param expression expression 方法执行所需的业务参数
     * @param method method 方法执行所需的业务参数
     * @param args args 方法执行所需的业务参数
     * @return 方法执行后的业务结果
     */
    public Object evaluate(String expression, Method method, Object[] args) {
        return evaluate(expression, method, args, null);
    }

    /**
     * 执行 evaluate 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param expression expression 方法执行所需的业务参数
     * @param method method 方法执行所需的业务参数
     * @param args args 方法执行所需的业务参数
     * @param result result 方法执行所需的业务参数
     * @return 方法执行后的业务结果
     */
    public Object evaluate(String expression, Method method, Object[] args, Object result) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        String[] parameterNames = nameDiscoverer.getParameterNames(method);
        for (int i = 0; i < args.length; i++) {
            // 同时暴露 pN/aN 和真实参数名，兼容不同编译参数与 Spring 缓存表达式习惯。
            context.setVariable("p" + i, args[i]);
            context.setVariable("a" + i, args[i]);
            if (parameterNames != null && i < parameterNames.length) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }
        // result 只在 unless/写回判断等后置表达式中有值，前置 key/condition 解析时保持为 null。
        context.setVariable("result", result);
        return parser.parseExpression(expression).getValue(context);
    }

    /**
     * 执行 evaluate Boolean 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param expression expression 方法执行所需的业务参数
     * @param method method 方法执行所需的业务参数
     * @param args args 方法执行所需的业务参数
     * @param result result 方法执行所需的业务参数
     * @param defaultValue defaultValue 待写入、比较或转换的业务值
     * @return 判断结果，true 表示校验通过或条件成立
     */
    public boolean evaluateBoolean(String expression, Method method, Object[] args, Object result, boolean defaultValue) {
        if (expression == null || expression.isBlank()) {
            return defaultValue;
        }
        Object value = evaluate(expression, method, args, result);
        // 只有明确解析为 Boolean.TRUE 才视为命中条件，其他类型或 null 都按 false 处理。
        return Boolean.TRUE.equals(value);
    }
}
