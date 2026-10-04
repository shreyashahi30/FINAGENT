package com.finagent.model;

import java.util.List;

public class PerformanceSnapshot {
    public long totalExecutionTimeMs;
    public int workloadUnits;
    public double successRate;
    public double averageContribution;
    public double averageRedundancy;
    public double averageEfficiency;
    public List<String> bottlenecks;

    public PerformanceSnapshot(long totalExecutionTimeMs, int workloadUnits, double successRate,
                               double averageContribution, double averageRedundancy, double averageEfficiency,
                               List<String> bottlenecks) {
        this.totalExecutionTimeMs = totalExecutionTimeMs;
        this.workloadUnits = workloadUnits;
        this.successRate = successRate;
        this.averageContribution = averageContribution;
        this.averageRedundancy = averageRedundancy;
        this.averageEfficiency = averageEfficiency;
        this.bottlenecks = bottlenecks;
    }
}
