package org.chovy.canvas.engine.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * Structural test verifying that DagEngine correctly threads the {@code depth} counter
 * through its recursive call chain so that {@code MAX_NODE_DEPTH} can actually fire.
 *
 * <h3>What was broken (before the fix)</h3>
 * <pre>
 * executeNode(3-arg)   → always called executeNode(4-arg, depth=0)
 * executeNodeAfterStage2 (no depth param) → triggerDownstream (no depth param)
 *   → executeNode(3-arg) → executeNode(4-arg, depth=0)   ← reset to 0 every hop
 * </pre>
 *
 * <h3>What the fix does</h3>
 * <pre>
 * - Removed the 3-arg executeNode overload entirely.
 * - Added {@code int depth} to executeNodeAfterStage2, triggerDownstream,
 *   tryPrioritySequentially, handleLogicRelation, handleHub, handleAggregate.
 * - Downstream calls pass {@code depth + 1}; PRIORITY lateral calls pass {@code depth}.
 * </pre>
 *
 * Because DagEngine depends on many Spring beans (HandlerRegistry, TraceWriteBuffer, etc.),
 * we use reflection to verify the structural invariants rather than running the full engine.
 */
class DagEngineDepthTest {

    /**
     * The dead 3-arg overload must not exist after the fix.
     * Its presence means depth is silently reset to 0 on every recursive call.
     */
    @Test
    @DisplayName("3-arg executeNode overload (the depth-reset bug) must not exist")
    void threeArgExecuteNode_mustNotExist() {
        List<Method> threeArgOverloads = Arrays.stream(DagEngine.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("executeNode") && m.getParameterCount() == 3)
                .collect(Collectors.toList());

        assertThat(threeArgOverloads)
                .as("The 3-arg executeNode(graph, nodeId, ctx) overload that reset depth=0 "
                        + "must have been deleted. Found: %s", threeArgOverloads)
                .isEmpty();
    }

    /**
     * executeNodeAfterStage2 must accept a depth parameter so the counter is not lost
     * when control transfers from executeNode into the stage-2 path.
     */
    @Test
    @DisplayName("executeNodeAfterStage2 must accept int depth as last parameter")
    void executeNodeAfterStage2_mustHaveDepthParam() {
        List<Method> methods = Arrays.stream(DagEngine.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("executeNodeAfterStage2"))
                .collect(Collectors.toList());

        assertThat(methods)
                .as("executeNodeAfterStage2 method must exist")
                .isNotEmpty();

        for (Method m : methods) {
            Class<?>[] params = m.getParameterTypes();
            assertThat(params[params.length - 1])
                    .as("Last parameter of executeNodeAfterStage2 must be int (depth), "
                            + "found: %s", params[params.length - 1].getSimpleName())
                    .isEqualTo(int.class);
        }
    }

    /**
     * triggerDownstream must accept a depth parameter so depth+1 can be passed
     * to the next executeNode call.
     */
    @Test
    @DisplayName("triggerDownstream must accept int depth as last parameter")
    void triggerDownstream_mustHaveDepthParam() {
        List<Method> methods = Arrays.stream(DagEngine.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("triggerDownstream"))
                .collect(Collectors.toList());

        assertThat(methods)
                .as("triggerDownstream method must exist")
                .isNotEmpty();

        for (Method m : methods) {
            Class<?>[] params = m.getParameterTypes();
            assertThat(params[params.length - 1])
                    .as("Last parameter of triggerDownstream must be int (depth), "
                            + "found: %s", params[params.length - 1].getSimpleName())
                    .isEqualTo(int.class);
        }
    }

    /**
     * tryPrioritySequentially must accept a depth parameter so the counter is threaded
     * through PRIORITY branch execution.
     */
    @Test
    @DisplayName("tryPrioritySequentially must accept int depth as last parameter")
    void tryPrioritySequentially_mustHaveDepthParam() {
        List<Method> methods = Arrays.stream(DagEngine.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("tryPrioritySequentially"))
                .collect(Collectors.toList());

        assertThat(methods)
                .as("tryPrioritySequentially method must exist")
                .isNotEmpty();

        for (Method m : methods) {
            Class<?>[] params = m.getParameterTypes();
            assertThat(params[params.length - 1])
                    .as("Last parameter of tryPrioritySequentially must be int (depth), "
                            + "found: %s", params[params.length - 1].getSimpleName())
                    .isEqualTo(int.class);
        }
    }

    /**
     * The 4-arg executeNode must still exist (it is the main recursive entry point).
     */
    @Test
    @DisplayName("4-arg executeNode(graph, nodeId, ctx, depth) must exist")
    void fourArgExecuteNode_mustExist() {
        List<Method> fourArgOverloads = Arrays.stream(DagEngine.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("executeNode") && m.getParameterCount() == 4)
                .collect(Collectors.toList());

        assertThat(fourArgOverloads)
                .as("The 4-arg executeNode(graph, nodeId, ctx, int depth) must exist")
                .isNotEmpty();

        Method m = fourArgOverloads.getFirst();
        assertThat(m.getParameterTypes()[3])
                .as("4th parameter must be int (depth)")
                .isEqualTo(int.class);
    }
}
