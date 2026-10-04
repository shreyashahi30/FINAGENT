package com.finagent;

import com.finagent.agent.*;
import com.finagent.model.AnalysisRequest;
import com.finagent.model.AnalysisResponse;
import com.finagent.service.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end tests that run the real agents through AnalysisService, not the synthetic
 * baseline used by EvolutionDecisionServiceTests. These exist because the decision-rule
 * tests alone don't prove the wiring in AnalysisService (team rebuild, final re-execution,
 * before/after snapshot) actually produces the right result once real agent output is used.
 */
class AnalysisServiceTests {

    private AnalysisService service() {
        return new AnalysisService(
                new FinancialStatementAgent(), new MarketAgent(), new NewsSentimentAgent(),
                new RiskAssessmentAgent(), new LiquidityRiskAgent(), new CreditRiskAgent(),
                new RegulatoryRiskAgent(), new ExternalConditionsAgent(),
                new EvolutionDecisionService(), new PerformanceMonitor(),
                new AnalysisHistoryService());
    }

    private AnalysisRequest request(double currentRatio, double debtToAssets, double operatingMargin,
                                     String marketSignal, String newsSignal, String mode) {
        AnalysisRequest r = new AnalysisRequest();
        r.companyName = "Test Company";
        r.revenue = 1000; r.operatingProfit = 120; r.cash = 180; r.debt = 350;
        r.currentRatio = currentRatio; r.debtToAssets = debtToAssets; r.operatingMargin = operatingMargin;
        r.marketSignal = marketSignal; r.newsSignal = newsSignal;
        r.question = "Analyze the financial risk.";
        r.mode = mode;
        return r;
    }

    @Test
    void standardAnalysisNeverChangesTheFixedFourAgentTeam() {
        AnalysisResponse r = service().analyze(request(0.8, 0.45, 12, "Stable market conditions", "Mostly positive news", "STANDARD"));

        assertEquals("Standard Analysis", r.mode);
        assertEquals("CONTINUE", r.decision);
        assertEquals(4, r.activeAgents.size());
        assertEquals(4, r.agentResults.size());
        assertTrue(r.adaptationHistory.isEmpty());
        assertEquals(r.initialTeamSize, r.finalTeamSize);
    }

    @Test
    void adaptiveAnalysisAddsLiquidityAgentEndToEndWithRealAgents() {
        AnalysisResponse r = service().analyze(request(0.8, 0.45, 12, "Stable market conditions", "Mostly positive news", "ADAPTIVE"));

        assertEquals("CREATE", r.decision);
        assertTrue(r.activeAgents.contains("Liquidity Risk Agent"));
        assertTrue(r.agentResults.stream().anyMatch(a -> "Liquidity Risk Agent".equals(a.agent)));
        assertEquals(5, r.finalTeamSize);
        assertEquals(1, r.adaptationHistory.size());
        assertEquals("CREATE", r.adaptationHistory.get(0).decision);
    }

    @Test
    void adaptiveAnalysisAddsCreditAgentEndToEndWithRealAgents() {
        // currentRatio stays >=1.0 (so Liquidity doesn't pre-empt Credit in the priority chain)
        // and margin/ratio avoid the FinancialStatementAgent's low-contribution "strong" branch,
        // so the runtime-health gate (avg contribution >=50) is actually satisfied.
        AnalysisResponse r = service().analyze(request(1.2, 0.75, 15, "Moderate market conditions", "Mixed developments reported", "ADAPTIVE"));

        assertEquals("CREATE", r.decision);
        assertTrue(r.activeAgents.contains("Credit Risk Agent"));
        assertEquals(5, r.finalTeamSize);
    }

    @Test
    void adaptiveAnalysisRemovesNewsAgentEndToEndForAStrongStableCompany() {
        // Same scenario EvaluationService uses for its REMOVE demonstration row.
        AnalysisResponse r = service().analyze(request(2.0, 0.25, 25, "Stable market conditions", "Mostly positive news", "ADAPTIVE"));

        assertEquals("REMOVE", r.decision);
        assertFalse(r.activeAgents.contains("News/Sentiment Agent"));
        assertFalse(r.agentResults.stream().anyMatch(a -> "News/Sentiment Agent".equals(a.agent)));
        assertEquals(3, r.finalTeamSize);
        assertTrue(r.finalWorkloadUnits < r.initialWorkloadUnits);
    }

    @Test
    void adaptiveAnalysisMergesMarketAndNewsEndToEndForModerateStableCompany() {
        // Same scenario EvaluationService uses for its MERGE demonstration row: stable/positive
        // signals but financials not strong enough to satisfy the stricter REMOVE gate, so the
        // priority chain (CREATE -> REMOVE -> MERGE) should fall through to MERGE.
        AnalysisResponse r = service().analyze(request(1.4, 0.45, 12, "Stable market conditions", "Mostly positive news", "ADAPTIVE"));

        assertEquals("MERGE", r.decision);
        assertFalse(r.activeAgents.contains("Market Agent"));
        assertFalse(r.activeAgents.contains("News/Sentiment Agent"));
        assertTrue(r.activeAgents.contains("External Conditions Agent"));
    }

    @Test
    void adaptiveAnalysisContinuesWhenNoAdaptationIsJustified() {
        AnalysisResponse r = service().analyze(request(1.4, 0.45, 12, "Uncertain market conditions", "Mixed news", "ADAPTIVE"));

        assertEquals("CONTINUE", r.decision);
        assertEquals(r.initialTeamSize, r.finalTeamSize);
        assertEquals(r.initialWorkloadUnits, r.finalWorkloadUnits);
    }

    @Test
    void riskAssessmentAgentIsAlwaysTheFinalAggregatorRegardlessOfDecision() {
        AnalysisResponse r = service().analyze(request(0.8, 0.45, 12, "Stable market conditions", "Mostly positive news", "ADAPTIVE"));
        assertEquals("Risk Assessment Agent", r.activeAgents.get(r.activeAgents.size() - 1));
        assertEquals("Risk Assessment Agent", r.agentResults.get(r.agentResults.size() - 1).agent);
    }
}
