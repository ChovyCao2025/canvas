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
     * 解析不依赖返回值的 SpEL 表达式。
     *
     * <p>常用于缓存 key 和 condition 前置表达式，变量上下文包含真实参数名、{@code #pN} 和 {@code #aN}。
     *
     * @param expression SpEL 表达式
     * @param method 被拦截的方法
     * @param args 方法实参数组
     * @return 表达式解析结果
     */
    public Object evaluate(String expression, Method method, Object[] args) {
        return evaluate(expression, method, args, null);
    }

    /**
     * 解析可引用返回值的 SpEL 表达式。
     *
     * <p>后置 unless、put key 等场景可通过 {@code #result} 访问目标方法返回值。
     *
     * @param expression SpEL 表达式
     * @param method 被拦截的方法
     * @param args 方法实参数组
     * @param result 目标方法返回值，前置表达式可传 null
     * @return 表达式解析结果
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
     * 解析布尔型 SpEL 表达式。
     *
     * <p>表达式为空时返回默认值；表达式结果只有精确等于 {@link Boolean#TRUE} 才视为 true。
     *
     * @param expression SpEL 表达式
     * @param method 被拦截的方法
     * @param args 方法实参数组
     * @param result 目标方法返回值
     * @param defaultValue 表达式为空时使用的默认值
     * @return 布尔表达式结果
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
