package org.chovy.canvas.platform.domain;

public final class PlatformWorkstreamReadinessPolicy {

    public static final String BLOCKED_CHILD_SPEC_REQUIRED = "BLOCKED_CHILD_SPEC_REQUIRED";
    public static final String READY_FOR_CHILD_EXECUTION = "READY_FOR_CHILD_EXECUTION";

    private PlatformWorkstreamReadinessPolicy() {
    }

    public static String statusFor(PlatformWorkstream workstream) {
        return requiresMissingChildSpec(workstream)
                ? BLOCKED_CHILD_SPEC_REQUIRED
                : READY_FOR_CHILD_EXECUTION;
    }

    public static boolean requiresMissingChildSpec(PlatformWorkstream workstream) {
        return workstream.requiresChildSpec()
                && (workstream.childSpecPath() == null || workstream.childSpecPath().isBlank());
    }
}
