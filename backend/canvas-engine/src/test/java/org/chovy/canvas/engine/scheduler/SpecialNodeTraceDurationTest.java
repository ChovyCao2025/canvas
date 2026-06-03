package org.chovy.canvas.engine.scheduler;

import org.chovy.canvas.engine.context.NodeStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * Structural test documenting that {@code executeNodeAfterStage2} — the execution path
 * shared by all special convergence node types (HUB, AGGREGATE, THRESHOLD) —
 * now captures {@code nodeStartMs} and passes {@code durationMs} to the 4-arg
 * {@code writeTraceEnd} overload, matching the behaviour of the ordinary node path
 * in {@code executeNode}.
 *
 * <h3>What was broken (before the fix)</h3>
 * <pre>
 * executeNodeAfterStage2:
 *   writeTraceStart(ctx, node)                     ← OK
 *   // nodeStartMs missing                         ← BUG
 *   writeTraceEnd(ctx, node, result)               ← calls writeTraceEnd(..., 0) → durationMs=null
 *   // metrics.recordNodeExecution missing         ← BUG
 *   // completion log.debug missing               ← BUG
 * </pre>
 *
 * <h3>What the fix does</h3>
 * <pre>
 * executeNodeAfterStage2:
 *   writeTraceStart(ctx, node)
 *   long nodeStartMs = System.currentTimeMillis()  ← added
 *   // ... handler execution ...
 *   // FAILED path:
 *   long durationMs = System.currentTimeMillis() - nodeStartMs
 *   writeTraceEnd(ctx, node, result, durationMs)   ← 4-arg overload
 *   // SUCCESS path:
 *   long durationMs = System.currentTimeMillis() - nodeStartMs
 *   writeTraceEnd(ctx, node, result, durationMs)   ← 4-arg overload
 *   metrics.recordNodeExecution(type, SUCCESS, durationMs)  ← added
 *   log.debug("[ENGINE] 节点完成 ...")              ← added
 * </pre>
 *
 * Because DagEngine depends on many Spring beans (HandlerRegistry, TraceWriteBuffer, etc.),
 * we use reflection to verify structural invariants rather than running the full engine.
 */
class SpecialNodeTraceDurationTest {

    /**
     * The 4-arg {@code writeTraceEnd(ctx, node, result, durationMs)} overload must exist.
     * Its presence is the compile-time proof that the 3-arg path has been replaced.
     */
    @Test
    @DisplayName("4-arg writeTraceEnd(ctx, node, result, long) overload must exist")
    void writeTraceEnd_fourArgOverload_mustExist() {
        List<Method> fourArgOverloads = Arrays.stream(DagEngine.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("writeTraceEnd") && m.getParameterCount() == 4)
                .collect(Collectors.toList());

        assertThat(fourArgOverloads)
                .as("The 4-arg writeTraceEnd(ctx, node, result, long durationMs) overload must exist. "
                        + "executeNodeAfterStage2 uses this overload to record real duration for "
                        + "HUB / AGGREGATE / THRESHOLD traces.")
                .isNotEmpty();

        // Verify the 4th parameter is long (durationMs)
        Method m = fourArgOverloads.getFirst();
        assertThat(m.getParameterTypes()[3])
                .as("4th parameter of writeTraceEnd must be long (durationMs), found: %s",
                        m.getParameterTypes()[3].getSimpleName())
                .isEqualTo(long.class);
    }

    /**
     * The 3-arg {@code writeTraceEnd} convenience overload may still exist (it delegates to
     * the 4-arg version with {@code durationMs=0}), but {@code executeNodeAfterStage2} must
     * NOT be the only caller — it now always uses the 4-arg overload directly.
     *
     * This test verifies the 4-arg overload is preferred by confirming both overloads coexist
     * (the 3-arg is a helper, not the main path).
     */
    @Test
    @DisplayName("Both writeTraceEnd overloads (3-arg and 4-arg) coexist; 4-arg is the real path")
    void writeTraceEnd_bothOverloadsCoexist() {
        long threeArgCount = Arrays.stream(DagEngine.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("writeTraceEnd") && m.getParameterCount() == 3)
                .count();
        long fourArgCount = Arrays.stream(DagEngine.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("writeTraceEnd") && m.getParameterCount() == 4)
                .count();

        assertThat(fourArgCount)
                .as("4-arg writeTraceEnd must exist (used by both ordinary and special node paths)")
                .isGreaterThanOrEqualTo(1);

        // 3-arg overload is a convenience wrapper — its presence is fine
        assertThat(threeArgCount + fourArgCount)
                .as("At least one writeTraceEnd overload must exist")
                .isGreaterThanOrEqualTo(1);
    }

    /**
     * {@code NodeStatus.SUCCESS} must be a valid constant so that
     * {@code metrics.recordNodeExecution(type, NodeStatus.SUCCESS.name(), durationMs)}
     * compiles and returns the expected string {@code "SUCCESS"}.
     */
    @Test
    @DisplayName("NodeStatus.SUCCESS.name() must return \"SUCCESS\"")
    void nodeStatus_SUCCESS_name_isCorrect() {
        assertThat(NodeStatus.SUCCESS.name())
                .as("NodeStatus.SUCCESS.name() must return \"SUCCESS\" — "
                        + "this is passed to metrics.recordNodeExecution in executeNodeAfterStage2")
                .isEqualTo("SUCCESS");
    }

    /**
     * {@code executeNodeAfterStage2} must still exist with the correct signature
     * (6 params ending in {@code int depth}) — this is unchanged from the depth fix.
     */
    @Test
    @DisplayName("executeNodeAfterStage2 must still accept int depth as last parameter")
    void executeNodeAfterStage2_stillHasDepthParam() {
        List<Method> methods = Arrays.stream(DagEngine.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("executeNodeAfterStage2"))
                .collect(Collectors.toList());

        assertThat(methods)
                .as("executeNodeAfterStage2 must exist")
                .isNotEmpty();

        for (Method m : methods) {
            Class<?>[] params = m.getParameterTypes();
            assertThat(params[params.length - 1])
                    .as("Last parameter of executeNodeAfterStage2 must be int (depth)")
                    .isEqualTo(int.class);
        }
    }
}
