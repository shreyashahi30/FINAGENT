package com.finagent.service;

import com.finagent.model.AnalysisRequest;
import com.finagent.model.AnalysisResponse;
import com.finagent.model.EvaluationResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EvaluationService {
    private final AnalysisService analysis;

    public EvaluationService(AnalysisService analysis) { this.analysis = analysis; }

    public EvaluationResponse run(int repetitions) {
        int reps = Math.max(1, Math.min(repetitions, 10));
        List<EvaluationResponse.EvaluationRow> rows = new ArrayList<>();
        double sw = 0, aw = 0, st = 0, at = 0;
        double sc = 0, ac = 0, sr = 0, ar = 0, se = 0, ae = 0, ss = 0, as = 0, sx = 0, ax = 0;

        for (Scenario scenario : Scenario.values()) {
            double scenarioSw = 0, scenarioAw = 0, scenarioSt = 0, scenarioAt = 0;
            String decision = "CONTINUE";
            for (int i = 0; i < reps; i++) {
                AnalysisResponse standard = analysis.analyze(scenario.request("STANDARD"), false);
                AnalysisResponse adaptive = analysis.analyze(scenario.request("ADAPTIVE"), false);

                scenarioSw += standard.finalWorkloadUnits;
                scenarioAw += adaptive.finalWorkloadUnits;
                scenarioSt += standard.finalTeamSize;
                scenarioAt += adaptive.finalTeamSize;

                sc += standard.finalPerformance.averageContribution;
                ac += adaptive.finalPerformance.averageContribution;
                sr += standard.finalPerformance.averageRedundancy;
                ar += adaptive.finalPerformance.averageRedundancy;
                se += standard.finalPerformance.averageEfficiency;
                ae += adaptive.finalPerformance.averageEfficiency;
                ss += standard.finalPerformance.successRate;
                as += adaptive.finalPerformance.successRate;
                sx += standard.finalPerformance.totalExecutionTimeMs;
                ax += adaptive.finalPerformance.totalExecutionTimeMs;
                decision = adaptive.decision;
            }
            scenarioSw /= reps; scenarioAw /= reps; scenarioSt /= reps; scenarioAt /= reps;
            rows.add(new EvaluationResponse.EvaluationRow(scenario.label, decision, scenarioSw, scenarioAw, scenarioSt, scenarioAt));
            sw += scenarioSw; aw += scenarioAw; st += scenarioSt; at += scenarioAt;
        }

        int count = Scenario.values().length;
        int totalRuns = count * reps;
        sw /= count; aw /= count; st /= count; at /= count;
        sc /= totalRuns; ac /= totalRuns; sr /= totalRuns; ar /= totalRuns;
        se /= totalRuns; ae /= totalRuns; ss /= totalRuns; as /= totalRuns; sx /= totalRuns; ax /= totalRuns;

        double workloadChange = sw == 0 ? 0 : ((aw - sw) / sw) * 100.0;
        return new EvaluationResponse(reps, rows, sw, aw, workloadChange, st, at,
                sc, ac, sr, ar, se, ae, ss, as, sx, ax);
    }

    private enum Scenario {
        LIQUIDITY("Liquidity Stress", .8, .45, 12, "Stable market conditions", "Mostly positive news"),
        MERGE("Stable External Conditions", 1.4, .45, 12, "Stable market conditions", "Mostly positive news"),
        REMOVE("Stable Company", 2.0, .25, 25, "Stable market conditions", "Mostly positive news"),
        CONTINUE("Mixed Conditions", 1.4, .45, 12, "Uncertain market conditions", "Mixed news");

        final String label;
        final double ratio, debt, margin;
        final String market, news;

        Scenario(String label, double ratio, double debt, double margin, String market, String news) {
            this.label = label; this.ratio = ratio; this.debt = debt; this.margin = margin;
            this.market = market; this.news = news;
        }

        AnalysisRequest request(String mode) {
            AnalysisRequest r = new AnalysisRequest();
            r.companyName = label + " Demo";
            r.revenue = 1000; r.operatingProfit = 120; r.cash = 180; r.debt = 350;
            r.currentRatio = ratio; r.debtToAssets = debt; r.operatingMargin = margin;
            r.marketSignal = market; r.newsSignal = news;
            r.question = "Evaluate financial risk for the evaluation scenario.";
            r.mode = mode;
            return r;
        }
    }
}
