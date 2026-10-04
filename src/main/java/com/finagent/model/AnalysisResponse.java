package com.finagent.model;

import java.util.List;

public class AnalysisResponse {
    public String companyName, mode, overallRisk, financialRisk, marketRisk, newsRisk;
    public String decision, decisionReason, decisionRule, recommendations, status;
    public String performanceSummary, performanceEvidence;
    public List<String> activeAgents;
    public List<AgentResult> agentResults;
    public List<AdaptationEvent> adaptationHistory;
    public PerformanceSnapshot baselinePerformance, finalPerformance;
    public long totalExecutionTimeMs;
    public int initialTeamSize, finalTeamSize, initialWorkloadUnits, finalWorkloadUnits;
    public double workloadChangePercent, efficiencyChangePercent;
}
