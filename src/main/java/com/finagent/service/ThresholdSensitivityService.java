
package com.finagent.service;

import com.finagent.model.AgentResult;
import com.finagent.model.AnalysisRequest;
import com.finagent.model.PerformanceSnapshot;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ThresholdSensitivityService {

    private final AnalysisService analysis;
    private final EvolutionDecisionService evolution;
    private final PerformanceMonitor monitor;

    public ThresholdSensitivityService(
            AnalysisService analysis,
            EvolutionDecisionService evolution,
            PerformanceMonitor monitor) {
        this.analysis = analysis;
        this.evolution = evolution;
        this.monitor = monitor;
    }

    public synchronized Map<String, Object> run() {
        double oldRedundancy =
                PerformanceThresholds.MAX_AVERAGE_REDUNDANCY;
        double oldContribution =
                PerformanceThresholds.MIN_AVERAGE_CONTRIBUTION;
        double oldSuccess =
                PerformanceThresholds.MIN_SUCCESS_RATE;
        double oldEfficiency =
                PerformanceThresholds.MIN_AVERAGE_EFFICIENCY;
        double oldMergeRedundancy =
                PerformanceThresholds.MERGE_MIN_REDUNDANCY;
        double oldMergeAverageRedundancy =
                PerformanceThresholds.MERGE_MIN_AVERAGE_REDUNDANCY;
        double oldRemoveContribution =
                PerformanceThresholds.REMOVE_MAX_CONTRIBUTION;
        double oldRemoveRedundancy =
                PerformanceThresholds.REMOVE_MIN_REDUNDANCY;
        double oldRemoveEfficiency =
                PerformanceThresholds.REMOVE_MAX_EFFICIENCY;

        List<Map<String, Object>> createRows = new ArrayList<>();
        List<Map<String, Object>> lifecycleRows = new ArrayList<>();

        try {
            resetAll(oldRedundancy, oldContribution, oldSuccess,
                    oldEfficiency, oldMergeRedundancy,
                    oldMergeAverageRedundancy, oldRemoveContribution,
                    oldRemoveRedundancy, oldRemoveEfficiency);

            add(createRows, "CREATE", "Baseline",
                    "Default thresholds", runCreateScenarios());

            for (double value : new double[]{20, 50, 80}) {
                resetCreate(oldRedundancy, oldContribution,
                        oldSuccess, oldEfficiency);
                PerformanceThresholds.MAX_AVERAGE_REDUNDANCY = value;
                add(createRows, "CREATE", "Redundancy",
                        "Maximum average redundancy = " + value,
                        runCreateScenarios());
            }

            for (double value : new double[]{20, 50, 80}) {
                resetCreate(oldRedundancy, oldContribution,
                        oldSuccess, oldEfficiency);
                PerformanceThresholds.MIN_AVERAGE_CONTRIBUTION = value;
                add(createRows, "CREATE", "Contribution",
                        "Minimum average contribution = " + value,
                        runCreateScenarios());
            }

            for (double value : new double[]{50, 80, 100}) {
                resetCreate(oldRedundancy, oldContribution,
                        oldSuccess, oldEfficiency);
                PerformanceThresholds.MIN_SUCCESS_RATE = value;
                add(createRows, "CREATE", "Success rate",
                        "Minimum success rate = " + value + "%",
                        runCreateScenarios());
            }

            for (double value : new double[]{10, 30, 60}) {
                resetCreate(oldRedundancy, oldContribution,
                        oldSuccess, oldEfficiency);
                PerformanceThresholds.MIN_AVERAGE_EFFICIENCY = value;
                add(createRows, "CREATE", "Efficiency",
                        "Minimum average efficiency = " + value,
                        runCreateScenarios());
            }

            // MERGE baseline: stable financials, successful agents,
            // high individual overlap, and no REMOVE eligibility.
            resetAll(oldRedundancy, oldContribution, oldSuccess,
                    oldEfficiency, oldMergeRedundancy,
                    oldMergeAverageRedundancy, oldRemoveContribution,
                    oldRemoveRedundancy, oldRemoveEfficiency);

            add(lifecycleRows, "MERGE", "Baseline",
                    "Default thresholds", runMergeScenario());

            for (double value : new double[]{40, 60, 80}) {
                resetAll(oldRedundancy, oldContribution, oldSuccess,
                        oldEfficiency, oldMergeRedundancy,
                        oldMergeAverageRedundancy, oldRemoveContribution,
                        oldRemoveRedundancy, oldRemoveEfficiency);
                PerformanceThresholds.MERGE_MIN_REDUNDANCY = value;
                add(lifecycleRows, "MERGE", "Agent redundancy",
                        "Minimum agent redundancy = " + value,
                        runMergeScenario());
            }

            for (double value : new double[]{20, 40, 60}) {
                resetAll(oldRedundancy, oldContribution, oldSuccess,
                        oldEfficiency, oldMergeRedundancy,
                        oldMergeAverageRedundancy, oldRemoveContribution,
                        oldRemoveRedundancy, oldRemoveEfficiency);
                PerformanceThresholds.MERGE_MIN_AVERAGE_REDUNDANCY = value;
                add(lifecycleRows, "MERGE", "Average redundancy",
                        "Minimum average redundancy = " + value,
                        runMergeScenario());
            }

            // REMOVE baseline: stable financials and a News agent whose
            // contribution, redundancy, and efficiency meet the default rule.
            resetAll(oldRedundancy, oldContribution, oldSuccess,
                    oldEfficiency, oldMergeRedundancy,
                    oldMergeAverageRedundancy, oldRemoveContribution,
                    oldRemoveRedundancy, oldRemoveEfficiency);

            add(lifecycleRows, "REMOVE", "Baseline",
                    "Default thresholds", runRemoveScenario());

            for (double value : new double[]{20, 40, 60}) {
                resetAll(oldRedundancy, oldContribution, oldSuccess,
                        oldEfficiency, oldMergeRedundancy,
                        oldMergeAverageRedundancy, oldRemoveContribution,
                        oldRemoveRedundancy, oldRemoveEfficiency);
                PerformanceThresholds.REMOVE_MAX_CONTRIBUTION = value;
                add(lifecycleRows, "REMOVE", "Contribution",
                        "Maximum contribution = " + value,
                        runRemoveScenario());
            }

            for (double value : new double[]{40, 60, 80}) {
                resetAll(oldRedundancy, oldContribution, oldSuccess,
                        oldEfficiency, oldMergeRedundancy,
                        oldMergeAverageRedundancy, oldRemoveContribution,
                        oldRemoveRedundancy, oldRemoveEfficiency);
                PerformanceThresholds.REMOVE_MIN_REDUNDANCY = value;
                add(lifecycleRows, "REMOVE", "Redundancy",
                        "Minimum redundancy = " + value,
                        runRemoveScenario());
            }

            for (double value : new double[]{10, 20, 30}) {
                resetAll(oldRedundancy, oldContribution, oldSuccess,
                        oldEfficiency, oldMergeRedundancy,
                        oldMergeAverageRedundancy, oldRemoveContribution,
                        oldRemoveRedundancy, oldRemoveEfficiency);
                PerformanceThresholds.REMOVE_MAX_EFFICIENCY = value;
                add(lifecycleRows, "REMOVE", "Efficiency",
                        "Maximum efficiency = " + value,
                        runRemoveScenario());
            }

            List<Map<String, Object>> allRows = new ArrayList<>();
            allRows.addAll(createRows);
            allRows.addAll(lifecycleRows);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("method",
                    "Controlled synthetic decision-rule sensitivity tests. "
                    + "CREATE, MERGE and REMOVE are evaluated using fixed "
                    + "agent metrics while one threshold is varied at a time. "
                    + "These experiments test rule behavior, not financial "
                    + "predictive accuracy.");
            result.put("createEligibilityRows", createRows);
            result.put("lifecycleRows", lifecycleRows);
            result.put("rows", allRows);

            int runs = 0;
            for (Map<String, Object> row : allRows) {
                runs += ((Number) row.get("scenarioCount")).intValue();
            }
            result.put("runs", runs);

            return result;
        } finally {
            resetAll(oldRedundancy, oldContribution, oldSuccess,
                    oldEfficiency, oldMergeRedundancy,
                    oldMergeAverageRedundancy, oldRemoveContribution,
                    oldRemoveRedundancy, oldRemoveEfficiency);
        }
    }

    private Map<String, Object> runCreateScenarios() {
        List<AnalysisRequest> requests = List.of(
                request(0.8, 0.45, 25,
                        "Stable market conditions", "Mostly positive news"),
                request(1.4, 0.75, 25,
                        "Stable market conditions", "Mostly positive news"),
                request(1.4, 0.45, 25,
                        "Stable market conditions", "Regulatory news"),
                request(1.4, 0.45, 25,
                        "Stable market conditions", "Mostly positive news")
        );

        List<AgentResult> baseline = createHealthyBaseline();
        return evaluate(requests, baseline, "CREATE");
    }

    private Map<String, Object> runMergeScenario() {
        AnalysisRequest request = request(
                2.0, 0.20, 25,
                "Stable market conditions", "Mostly positive news");

        List<AgentResult> baseline = List.of(
                agent("Financial Statement Agent", "MEDIUM",
                        65, 10, 50, "Financial"),
                agent("Market Agent", "MEDIUM",
                        50, 75, 50, "Market"),
                agent("News/Sentiment Agent", "LOW",
                        50, 75, 50, "External / Sentiment"),
                agent("Risk Assessment Agent", "HIGH",
                        80, 5, 50, "Overall Risk")
        );

        return evaluate(List.of(request), baseline, "MERGE");
    }

    private Map<String, Object> runRemoveScenario() {
        AnalysisRequest request = request(
                2.0, 0.20, 25,
                "Weak market conditions", "Negative news");

        List<AgentResult> baseline = List.of(
                agent("Financial Statement Agent", "MEDIUM",
                        65, 10, 50, "Financial"),
                agent("Market Agent", "MEDIUM",
                        50, 20, 50, "Market"),
                agent("News/Sentiment Agent", "LOW",
                        30, 70, 20, "External / Sentiment"),
                agent("Risk Assessment Agent", "HIGH",
                        80, 5, 50, "Overall Risk")
        );

        return evaluate(List.of(request), baseline, "REMOVE");
    }

    private Map<String, Object> evaluate(
            List<AnalysisRequest> requests,
            List<AgentResult> baseline,
            String experiment) {

        int creates = 0;
        int merges = 0;
        int removes = 0;
        int continues = 0;
        double workload = 0;
        double teamSize = 0;
        double contribution = 0;
        double redundancy = 0;
        double efficiency = 0;
        double success = 0;

        PerformanceSnapshot snapshot = monitor.snapshot(baseline);

        for (AnalysisRequest request : requests) {
            EvolutionDecisionService.Decision decision =
                    evolution.decide(request, baseline, monitor);

            switch (decision.type()) {
                case "CREATE" -> creates++;
                case "MERGE" -> merges++;
                case "REMOVE" -> removes++;
                default -> continues++;
            }

            int finalSize = baseline.size();
            int finalWorkload = snapshot.workloadUnits;

            if ("CREATE".equals(decision.type())) {
                finalSize++;
                finalWorkload += monitor.workloadUnits(
                        "Regulatory Risk Agent".equals(decision.agentToAdd())
                                ? "LOW" : "MEDIUM");
            } else if ("REMOVE".equals(decision.type())) {
                finalSize -= decision.agentsToRemove().size();
                for (String name : decision.agentsToRemove()) {
                    AgentResult removed = baseline.stream()
                            .filter(a -> name.equals(a.agent))
                            .findFirst().orElse(null);
                    if (removed != null) {
                        finalWorkload -= monitor.workloadUnits(removed.workload);
                    }
                }
            } else if ("MERGE".equals(decision.type())) {
                finalSize -= decision.agentsToMerge().size();
                for (String name : decision.agentsToMerge()) {
                    AgentResult removed = baseline.stream()
                            .filter(a -> name.equals(a.agent))
                            .findFirst().orElse(null);
                    if (removed != null) {
                        finalWorkload -= monitor.workloadUnits(removed.workload);
                    }
                }
                finalSize++;
                finalWorkload += monitor.workloadUnits("MEDIUM");
            }

            workload += finalWorkload;
            teamSize += finalSize;
            contribution += snapshot.averageContribution;
            redundancy += snapshot.averageRedundancy;
            efficiency += snapshot.averageEfficiency;
            success += snapshot.successRate;
        }

        int count = requests.size();
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("scenarioCount", count);
        metrics.put("create", creates);
        metrics.put("merge", merges);
        metrics.put("remove", removes);
        metrics.put("continue", continues);
        metrics.put("averageFinalWorkload", workload / count);
        metrics.put("averageFinalTeamSize", teamSize / count);
        metrics.put("averageContribution", contribution / count);
        metrics.put("averageRedundancy", redundancy / count);
        metrics.put("averageEfficiency", efficiency / count);
        metrics.put("averageSuccessRate", success / count);

        metrics.put("testedDecision", experiment);
        return metrics;
    }

    private List<AgentResult> createHealthyBaseline() {
        return List.of(
                agent("Financial Statement Agent", "MEDIUM",
                        65, 10, 50, "Financial"),
                agent("Market Agent", "MEDIUM",
                        60, 40, 50, "Market"),
                agent("News/Sentiment Agent", "LOW",
                        60, 40, 50, "External / Sentiment"),
                agent("Risk Assessment Agent", "HIGH",
                        80, 5, 50, "Overall Risk")
        );
    }

    private AgentResult agent(
            String name, String workload,
            int contribution, int redundancy, int efficiency,
            String riskArea) {
        return new AgentResult(name, "", 1, workload, true,
                contribution, redundancy, efficiency, riskArea);
    }

    private AnalysisRequest request(
            double currentRatio,
            double debtToAssets,
            double operatingMargin,
            String marketSignal,
            String newsSignal) {

        AnalysisRequest r = new AnalysisRequest();
        r.companyName = "Synthetic Sensitivity Scenario";
        r.revenue = 1000;
        r.operatingProfit = 250;
        r.cash = 180;
        r.debt = 200;
        r.currentRatio = currentRatio;
        r.debtToAssets = debtToAssets;
        r.operatingMargin = operatingMargin;
        r.hasLiquidityData = true;
        r.hasDebtData = true;
        r.hasMarginData = true;
        r.marketSignal = marketSignal;
        r.newsSignal = newsSignal;
        r.dataSource = "Synthetic sensitivity scenario";
        r.question = "Controlled decision-rule test";
        r.mode = "ADAPTIVE";
        return r;
    }

    private void resetCreate(
            double redundancy,
            double contribution,
            double success,
            double efficiency) {
        PerformanceThresholds.MAX_AVERAGE_REDUNDANCY = redundancy;
        PerformanceThresholds.MIN_AVERAGE_CONTRIBUTION = contribution;
        PerformanceThresholds.MIN_SUCCESS_RATE = success;
        PerformanceThresholds.MIN_AVERAGE_EFFICIENCY = efficiency;
    }

    private void resetAll(
            double redundancy,
            double contribution,
            double success,
            double efficiency,
            double mergeRedundancy,
            double mergeAverageRedundancy,
            double removeContribution,
            double removeRedundancy,
            double removeEfficiency) {

        resetCreate(redundancy, contribution, success, efficiency);
        PerformanceThresholds.MERGE_MIN_REDUNDANCY = mergeRedundancy;
        PerformanceThresholds.MERGE_MIN_AVERAGE_REDUNDANCY =
                mergeAverageRedundancy;
        PerformanceThresholds.REMOVE_MAX_CONTRIBUTION = removeContribution;
        PerformanceThresholds.REMOVE_MIN_REDUNDANCY = removeRedundancy;
        PerformanceThresholds.REMOVE_MAX_EFFICIENCY = removeEfficiency;
    }

    private void add(
            List<Map<String, Object>> rows,
            String part,
            String family,
            String setting,
            Map<String, Object> metrics) {

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("part", part);
        row.put("family", family);
        row.put("setting", setting);
        row.putAll(metrics);
        rows.add(row);
    }
}
