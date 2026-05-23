package org.chovy.cache.strategy;

public enum BreakdownProtectionStrategy {
    NONE,
    LOCAL_SINGLE_FLIGHT,
    DISTRIBUTED_LOCK,
    STALE_WHILE_REVALIDATE,
    LOCAL_AND_DISTRIBUTED,
    FULL
}
