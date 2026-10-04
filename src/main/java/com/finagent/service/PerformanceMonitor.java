package com.finagent.service;

import com.finagent.model.AgentResult;
import com.finagent.model.PerformanceSnapshot;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PerformanceMonitor {

    /**
     * Aggregates deterministic Demo Mode proxy scores. These scores are not
     * claimed to be direct measurements of token usage, CPU utilization or
     * financial-analysis quality.
     */

    public PerformanceSnapshot snapshot(List<AgentResult> results) {
        long time = 0;
        int workload = 0;
        int success = 0;
        double contribution = 0;
        double redundancy = 0;
        double efficiency = 0;
        List<String> bottlenecks = new ArrayList<>();

        for (AgentResult result : results) {
            time += result.executionTimeMs;
            workload += workloadUnits(result.workload);
            if (result.success) success++;
            contribution += result.contributionScore;
            redundancy += result.redundancyScore;
            efficiency += result.efficiencyScore;
            if (result.redundancyScore >= 70) bottlenecks.add(result.agent + " has high overlap with another analysis area.");
            if (result.efficiencyScore < 40) bottlenecks.add(result.agent + " has low contribution relative to workload.");
        }

        int count = Math.max(1, results.size());
        return new PerformanceSnapshot(
                Math.max(1, time),
                workload,
                success * 100.0 / count,
                contribution / count,
                redundancy / count,
                efficiency / count,
                bottlenecks.stream().distinct().toList());
    }

    public int workloadUnits(String workload) {
        return switch (workload == null ? "" : workload) {
            case "LOW" -> 1;
            case "HIGH" -> 3;
            default -> 2;
        };
    }

    public double percentageChange(int before, int after) {
        if (before == 0) return 0;
        return ((after - before) * 100.0) / before;
    }

    public double efficiency(PerformanceSnapshot snapshot) {
        return snapshot.averageEfficiency;
    }
}
