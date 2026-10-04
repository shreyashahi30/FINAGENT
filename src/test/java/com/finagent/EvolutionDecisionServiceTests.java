package com.finagent;

import com.finagent.model.AgentResult;
import com.finagent.model.AnalysisRequest;
import com.finagent.service.PerformanceMonitor;

import java.util.List;
import com.finagent.service.EvolutionDecisionService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvolutionDecisionServiceTests {

    private final EvolutionDecisionService service = new EvolutionDecisionService();

    @Test
    void createsLiquidityAgentWhenCurrentRatioIsLow() {
        AnalysisRequest r = request(0.8, 0.45, 12, "Stable market conditions", "Mostly positive news");
        assertEquals("CREATE", service.decide(r).type());
        assertEquals("Liquidity Risk Agent", service.decide(r).agentToAdd());
        assertTrue(service.decide(r).performanceEvidence().contains("coverage gap"));
    }

    @Test
    void mergesStableMarketAndPositiveNews() {
        AnalysisRequest r = request(1.4, 0.45, 12, "Stable market conditions", "Mostly positive news");
        assertEquals("MERGE", service.decide(r).type());
        assertEquals(2, service.decide(r).agentsToMerge().size());
        assertTrue(service.decide(r).performanceEvidence().contains("redundancy"));
    }

    @Test
    void removesLowValueNewsAgentForVeryStrongCompany() {
        AnalysisRequest r = request(2.0, 0.25, 25, "Stable market conditions", "Mostly positive news");
        assertEquals("REMOVE", service.decide(r).type());
        assertEquals("News/Sentiment Agent", service.decide(r).agentsToRemove().get(0));
        assertTrue(service.decide(r).performanceEvidence().contains("low"));
    }

    @Test
    void continuesWhenNoAdaptationTriggerExists() {
        AnalysisRequest r = request(1.4, 0.45, 12, "Uncertain market conditions", "Mixed news");
        assertEquals("CONTINUE", service.decide(r).type());
    }


    @Test
    void doesNotMergeWhenRedundancyIsLow() {
        AnalysisRequest r = request(1.4, 0.45, 12, "Stable market conditions", "Mostly positive news");
        List<AgentResult> team = List.of(
                new AgentResult("Financial Statement Agent", "", 1, "MEDIUM", true, 65, 10, 32, "Financial"),
                new AgentResult("Market Agent", "", 1, "MEDIUM", true, 70, 20, 35, "Market"),
                new AgentResult("News/Sentiment Agent", "", 1, "LOW", true, 70, 20, 70, "External"),
                new AgentResult("Risk Assessment Agent", "", 1, "HIGH", true, 80, 5, 27, "Overall Risk")
        );

        assertEquals("CONTINUE", service.decide(r, team, new PerformanceMonitor()).type());
    }

    @Test
    void doesNotRemoveWhenNewsAgentHasUsefulContribution() {
        AnalysisRequest r = request(2.0, 0.25, 25, "Stable market conditions", "Mostly positive news");
        List<AgentResult> team = List.of(
                new AgentResult("Financial Statement Agent", "", 1, "MEDIUM", true, 65, 10, 32, "Financial"),
                new AgentResult("Market Agent", "", 1, "MEDIUM", true, 70, 20, 35, "Market"),
                new AgentResult("News/Sentiment Agent", "", 1, "LOW", true, 75, 20, 75, "External"),
                new AgentResult("Risk Assessment Agent", "", 1, "HIGH", true, 80, 5, 27, "Overall Risk")
        );

        assertEquals("CONTINUE", service.decide(r, team, new PerformanceMonitor()).type());
    }

    @Test
    void doesNotCreateWhenWorkloadIsAlreadyTooHigh() {
        AnalysisRequest r = request(0.8, 0.45, 12, "Stable market conditions", "Mostly positive news");
        List<AgentResult> team = List.of(
                new AgentResult("Financial Statement Agent", "", 1, "HIGH", true, 80, 10, 40, "Financial"),
                new AgentResult("Market Agent", "", 1, "HIGH", true, 80, 20, 40, "Market"),
                new AgentResult("News/Sentiment Agent", "", 1, "HIGH", true, 80, 20, 80, "External"),
                new AgentResult("Risk Assessment Agent", "", 1, "HIGH", true, 80, 5, 40, "Overall Risk")
        );

        assertEquals("CONTINUE", service.decide(r, team, new PerformanceMonitor()).type());
    }

    @Test
    void doesNotCreateWhenRuntimePerformanceIsPoor() {
        AnalysisRequest r = request(0.8, 0.45, 12, "Stable market conditions", "Mostly positive news");
        List<AgentResult> poorTeam = List.of(
                new AgentResult("Financial Statement Agent", "", 8, "HIGH", false, 20, 30, 10, "Financial"),
                new AgentResult("Market Agent", "", 8, "HIGH", true, 25, 20, 10, "Market"),
                new AgentResult("News/Sentiment Agent", "", 8, "HIGH", true, 25, 20, 10, "External"),
                new AgentResult("Risk Assessment Agent", "", 8, "HIGH", true, 20, 10, 10, "Overall Risk")
        );

        assertEquals("CONTINUE", service.decide(r, poorTeam, new PerformanceMonitor()).type());
        assertTrue(service.decide(r, poorTeam, new PerformanceMonitor()).performanceEvidence().contains("execution time"));
    }

    private AnalysisRequest request(double currentRatio, double debtToAssets, double operatingMargin,
                                    String marketSignal, String newsSignal) {
        AnalysisRequest r = new AnalysisRequest();
        r.companyName = "Test Company";
        r.revenue = 1000;
        r.operatingProfit = 120;
        r.cash = 180;
        r.debt = 350;
        r.currentRatio = currentRatio;
        r.debtToAssets = debtToAssets;
        r.operatingMargin = operatingMargin;
        r.marketSignal = marketSignal;
        r.newsSignal = newsSignal;
        r.question = "Analyze the financial risk.";
        r.mode = "ADAPTIVE";
        return r;
    }
}
