package com.finagent.service;

import com.finagent.model.AgentResult;
import com.finagent.model.AnalysisRequest;
import com.finagent.model.PerformanceSnapshot;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EvolutionDecisionService {

    /**
     * Decides whether the baseline team should change.
     * Financial signals identify the type of additional coverage that may be needed.
     * Runtime signals decide whether that change is worthwhile from a team-performance point of view.
     */
    public Decision decide(AnalysisRequest r, List<AgentResult> baselineResults, PerformanceMonitor monitor) {
        PerformanceSnapshot p = monitor.snapshot(baselineResults);
        AgentResult newsResult = find(baselineResults, "News/Sentiment Agent");
        AgentResult marketResult = find(baselineResults, "Market Agent");

        boolean performanceHealthyForExpansion = p.successRate >= PerformanceThresholds.MIN_SUCCESS_RATE
                && p.averageContribution >= PerformanceThresholds.MIN_AVERAGE_CONTRIBUTION
                && p.averageEfficiency >= PerformanceThresholds.MIN_AVERAGE_EFFICIENCY
                && p.averageRedundancy <= PerformanceThresholds.MAX_AVERAGE_REDUNDANCY
                && p.workloadUnits <= PerformanceThresholds.MAX_WORKLOAD_UNITS
                && p.totalExecutionTimeMs <= Math.max(PerformanceThresholds.MIN_RUNTIME_BUDGET_MS, baselineResults.size() * PerformanceThresholds.RUNTIME_BUDGET_PER_AGENT_MS);

        boolean externalOverlap = marketResult != null && newsResult != null
                && marketResult.success && newsResult.success
                && marketResult.redundancyScore >= PerformanceThresholds.MERGE_MIN_REDUNDANCY
                && newsResult.redundancyScore >= PerformanceThresholds.MERGE_MIN_REDUNDANCY;

        // CREATE: a financial condition creates a coverage gap, but the runtime team must
        // still be healthy enough that the extra specialist is worth the additional cost.
        if (r.hasLiquidityData && r.currentRatio < PerformanceThresholds.LIQUIDITY_CURRENT_RATIO && performanceHealthyForExpansion) {
            return new Decision("CREATE",
                    "Liquidity risk requires additional specialist analysis.",
                    "Current Ratio < 1.0 + healthy runtime performance: success ≥80%, contribution ≥50%, efficiency ≥25%, redundancy ≤60%.",
                    "Liquidity Risk Agent", List.of(), List.of(),
                    evidence(p, "A liquidity coverage gap was detected. Runtime performance is acceptable, so adding a specialist is worthwhile."));
        }

        if (r.hasDebtData && r.debtToAssets > PerformanceThresholds.CREDIT_DEBT_TO_ASSETS && performanceHealthyForExpansion) {
            return new Decision("CREATE",
                    "High leverage requires additional credit-risk analysis.",
                    "Debt-to-Assets > 0.60 + healthy runtime performance: success ≥80%, contribution ≥50%, efficiency ≥25%, redundancy ≤60%.",
                    "Credit Risk Agent", List.of(), List.of(),
                    evidence(p, "A credit-risk coverage gap was detected. Runtime performance is acceptable, so adding a specialist is worthwhile."));
        }

        String news = lower(r.newsSignal);
        if (news.contains("regulatory") && performanceHealthyForExpansion) {
            return new Decision("CREATE",
                    "A regulatory signal requires specialist regulatory analysis.",
                    "News Signal contains 'regulatory' + healthy runtime performance: success ≥80%, contribution ≥50%, efficiency ≥25%, redundancy ≤60%.",
                    "Regulatory Risk Agent", List.of(), List.of(),
                    evidence(p, "A regulatory coverage gap was detected. Runtime performance is acceptable, so adding a specialist is worthwhile."));
        }

        // REMOVE: the agent is successful, but its marginal contribution is low and overlap is high.
        if (newsResult != null
                && newsResult.success
                && newsResult.contributionScore <= PerformanceThresholds.REMOVE_MAX_CONTRIBUTION
                && newsResult.redundancyScore >= PerformanceThresholds.REMOVE_MIN_REDUNDANCY
                && newsResult.efficiencyScore <= PerformanceThresholds.REMOVE_MAX_EFFICIENCY
                && r.hasLiquidityData && r.hasDebtData && r.hasMarginData
                && r.currentRatio >= PerformanceThresholds.REMOVE_MIN_CURRENT_RATIO
                && r.debtToAssets < PerformanceThresholds.REMOVE_MAX_DEBT_TO_ASSETS
                && r.operatingMargin >= PerformanceThresholds.REMOVE_MIN_OPERATING_MARGIN) {
            return new Decision("REMOVE",
                    "The News/Sentiment Agent has low contribution and high redundancy under stable financial conditions.",
                    "Performance trigger: low contribution + high redundancy + low efficiency. Financial check: company is strongly stable.",
                    null, List.of("News/Sentiment Agent"), List.of(),
                    evidence(p, "News/Sentiment contribution is low, redundancy is high and efficiency is low; removing it reduces unnecessary workload."));
        }

        // MERGE: market and news both succeed, overlap heavily, and a single external-conditions
        // agent has a lower workload/cost than keeping both agents.
        if (externalOverlap && isStableOrPositive(lower(r.marketSignal)) && isStableOrPositive(news)) {
            int combinedWorkload = monitor.workloadUnits(marketResult.workload) + monitor.workloadUnits(newsResult.workload);
            int mergedWorkload = monitor.workloadUnits("MEDIUM");
            long combinedTime = marketResult.executionTimeMs + newsResult.executionTimeMs;
            double combinedEfficiency = (marketResult.efficiencyScore + newsResult.efficiencyScore) / 2.0;
            boolean mergeImprovesRuntime = combinedWorkload > PerformanceThresholds.MERGE_MAX_MERGED_WORKLOAD
                    && combinedTime >= PerformanceThresholds.MERGE_MIN_COMBINED_TIME_MS
                    && (combinedEfficiency < PerformanceThresholds.MERGE_MAX_COMBINED_EFFICIENCY || p.averageRedundancy >= PerformanceThresholds.MERGE_MIN_AVERAGE_REDUNDANCY);
            if (mergeImprovesRuntime) {
                return new Decision("MERGE",
                        "Market and news analysis overlap heavily and consolidation reduces overlapping workload.",
                        "High redundancy + successful agents + lower merged workload + efficiency/overlap benefit.",
                        null, List.of(), List.of("Market Agent", "News/Sentiment Agent"),
                        evidence(p, "High overlap makes consolidation beneficial: "
                                + combinedWorkload + " → " + mergedWorkload + " workload units; combined execution time="
                                + combinedTime + " ms; combined efficiency=" + format(combinedEfficiency) + "."));
            }
        }

        return new Decision("CONTINUE",
                "The current team is performing adequately and no beneficial adaptation was found.",
                "No financial coverage gap or runtime-performance benefit justified a team change.",
                null, List.of(), List.of(),
                evidence(p, "No adaptation produced a clear runtime-performance benefit, so the current team is retained."));
    }

    /** Backward-compatible helper used by unit tests and simple callers. */
    public Decision decide(AnalysisRequest r) {
        PerformanceMonitor monitor = new PerformanceMonitor();
        int marketContribution = isStableOrPositive(lower(r.marketSignal)) ? 35 : 60;
        int newsContribution = isStableOrPositive(lower(r.newsSignal)) ? 30 : 60;
        int overlap = isStableOrPositive(lower(r.marketSignal)) && isStableOrPositive(lower(r.newsSignal)) ? 85 : 20;
        List<AgentResult> baseline = List.of(
                new AgentResult("Financial Statement Agent", "", 1, "MEDIUM", true, 65, 10, 32, "Financial"),
                new AgentResult("Market Agent", "", 1, "MEDIUM", true, marketContribution, overlap, Math.max(20, marketContribution / 2), "Market"),
                new AgentResult("News/Sentiment Agent", "", 1, "LOW", true, newsContribution, overlap, newsContribution, "External / Sentiment"),
                new AgentResult("Risk Assessment Agent", "", 1, "HIGH", true, 80, 5, 27, "Overall Risk")
        );
        return decide(r, baseline, monitor);
    }

    private String evidence(PerformanceSnapshot p, String message) {
        return message + " Runtime: workload=" + p.workloadUnits + " units, contribution="
                + format(p.averageContribution) + ", redundancy=" + format(p.averageRedundancy)
                + ", efficiency=" + format(p.averageEfficiency) + ", success=" + format(p.successRate)
                + "%, execution time=" + p.totalExecutionTimeMs + " ms.";
    }

    private String format(double value) { return String.format("%.1f", value); }

    private AgentResult find(List<AgentResult> results, String name) {
        return results.stream().filter(a -> name.equals(a.agent)).findFirst().orElse(null);
    }

    private boolean isStableOrPositive(String signal) {
        return signal.contains("stable") || signal.contains("positive") || signal.contains("normal")
                || signal.contains("strong") || signal.contains("good") || signal.contains("healthy");
    }

    private String lower(String value) { return value == null ? "" : value.toLowerCase(); }

    public record Decision(String type, String reason, String rule, String agentToAdd,
                           List<String> agentsToRemove, List<String> agentsToMerge, String performanceEvidence) {}
}
