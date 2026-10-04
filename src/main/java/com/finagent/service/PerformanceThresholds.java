package com.finagent.service;

/**
 * Initial heuristic thresholds used by the Demo Mode decision engine.
 *
 * These values are intentionally centralized so that the prototype has one
 * auditable policy instead of scattering magic numbers through the rules.
 * They are not presented as universal financial-industry standards.
 */
public final class PerformanceThresholds {
    private PerformanceThresholds() {}

    // Performance Health Gate: minimum/maximum acceptable baseline behavior.
    public static final double MIN_SUCCESS_RATE = 80.0;
    public static final double MIN_AVERAGE_CONTRIBUTION = 50.0;
    public static final double MIN_AVERAGE_EFFICIENCY = 25.0;
    public static final double MAX_AVERAGE_REDUNDANCY = 60.0;
    public static final int MAX_WORKLOAD_UNITS = 8;
    public static final long MIN_RUNTIME_BUDGET_MS = 10L;
    public static final long RUNTIME_BUDGET_PER_AGENT_MS = 5L;

    // CREATE triggers.
    public static final double LIQUIDITY_CURRENT_RATIO = 1.0;
    public static final double CREDIT_DEBT_TO_ASSETS = 0.60;

    // REMOVE trigger for News/Sentiment.
    public static final double REMOVE_MAX_CONTRIBUTION = 35.0;
    public static final double REMOVE_MIN_REDUNDANCY = 70.0;
    public static final double REMOVE_MAX_EFFICIENCY = 35.0;
    public static final double REMOVE_MIN_CURRENT_RATIO = 1.8;
    public static final double REMOVE_MAX_DEBT_TO_ASSETS = 0.35;
    public static final double REMOVE_MIN_OPERATING_MARGIN = 20.0;

    // MERGE trigger.
    public static final double MERGE_MIN_REDUNDANCY = 70.0;
    public static final int MERGE_MAX_MERGED_WORKLOAD = 2;
    public static final long MERGE_MIN_COMBINED_TIME_MS = 2L;
    public static final double MERGE_MAX_COMBINED_EFFICIENCY = 35.0;
    public static final double MERGE_MIN_AVERAGE_REDUNDANCY = 40.0;
}
