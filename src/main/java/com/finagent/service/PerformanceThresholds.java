package com.finagent.service;

public final class PerformanceThresholds {
    private PerformanceThresholds() {}

    public static double MIN_SUCCESS_RATE = 80.0;
    public static double MIN_AVERAGE_CONTRIBUTION = 50.0;
    public static double MIN_AVERAGE_EFFICIENCY = 25.0;
    public static double MAX_AVERAGE_REDUNDANCY = 60.0;
    public static int MAX_WORKLOAD_UNITS = 8;
    public static long MIN_RUNTIME_BUDGET_MS = 10L;
    public static long RUNTIME_BUDGET_PER_AGENT_MS = 5L;

    public static double LIQUIDITY_CURRENT_RATIO = 1.0;
    public static double CREDIT_DEBT_TO_ASSETS = 0.60;

    public static double REMOVE_MAX_CONTRIBUTION = 35.0;
    public static double REMOVE_MIN_REDUNDANCY = 70.0;
    public static double REMOVE_MAX_EFFICIENCY = 35.0;
    public static double REMOVE_MIN_CURRENT_RATIO = 1.8;
    public static double REMOVE_MAX_DEBT_TO_ASSETS = 0.35;
    public static double REMOVE_MIN_OPERATING_MARGIN = 20.0;

    public static double MERGE_MIN_REDUNDANCY = 70.0;
    public static int MERGE_MAX_MERGED_WORKLOAD = 2;
    public static long MERGE_MIN_COMBINED_TIME_MS = 2L;
    public static double MERGE_MAX_COMBINED_EFFICIENCY = 35.0;
    public static double MERGE_MIN_AVERAGE_REDUNDANCY = 40.0;
}
