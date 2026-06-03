package org.chovy.canvas.domain.canvas;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CanvasTransactionAnnotationTest {

    @Test
    void canvasServiceTransactionsRollbackForCheckedExceptions() {
        List<Class<?>> serviceClasses = List.of(
                CanvasService.class,
                CanvasOpsService.class,
                CanvasTransactionService.class
        );

        serviceClasses.forEach(serviceClass ->
                Arrays.stream(serviceClass.getDeclaredMethods())
                        .filter(method -> method.isAnnotationPresent(Transactional.class))
                        .forEach(this::assertRollbackForException));
    }

    private void assertRollbackForException(Method method) {
        Transactional transactional = method.getAnnotation(Transactional.class);
        assertThat(transactional.rollbackFor())
                .as("%s#%s rollbackFor", method.getDeclaringClass().getSimpleName(), method.getName())
                .contains(Exception.class);
    }
}
