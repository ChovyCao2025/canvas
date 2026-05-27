package org.chovy.canvas.engine.rule;

import java.util.Set;

public record RuleValidationOptions(
        String scope,
        boolean allowEmpty,
        int maxDepth,
        int maxConditions,
        Set<String> allowedFields
) {
    public static RuleValidationOptions strict(String scope) {
        return new RuleValidationOptions(scope, false, 8, 100, Set.of());
    }

    public RuleValidationOptions withAllowEmpty() {
        return new RuleValidationOptions(scope, true, maxDepth, maxConditions, allowedFields);
    }

    public boolean hasFieldWhitelist() {
        return allowedFields != null && !allowedFields.isEmpty();
    }
}
