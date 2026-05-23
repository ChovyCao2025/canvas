package org.chovy.cache.aop;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;

public class SpelKeyEvaluator {
    private final ExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    public Object evaluate(String expression, Method method, Object[] args) {
        return evaluate(expression, method, args, null);
    }

    public Object evaluate(String expression, Method method, Object[] args, Object result) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        String[] parameterNames = nameDiscoverer.getParameterNames(method);
        for (int i = 0; i < args.length; i++) {
            context.setVariable("p" + i, args[i]);
            context.setVariable("a" + i, args[i]);
            if (parameterNames != null && i < parameterNames.length) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }
        context.setVariable("result", result);
        return parser.parseExpression(expression).getValue(context);
    }

    public boolean evaluateBoolean(String expression, Method method, Object[] args, Object result, boolean defaultValue) {
        if (expression == null || expression.isBlank()) {
            return defaultValue;
        }
        Object value = evaluate(expression, method, args, result);
        return Boolean.TRUE.equals(value);
    }
}
