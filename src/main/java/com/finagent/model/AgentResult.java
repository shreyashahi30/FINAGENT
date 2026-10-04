package com.finagent.model;

public class AgentResult {
    public String agent, summary, workload, riskArea;
    public long executionTimeMs;
    public boolean success;
    public int contributionScore, redundancyScore, efficiencyScore;

    public AgentResult(String agent, String summary, long executionTimeMs, String workload, boolean success) {
        this(agent, summary, executionTimeMs, workload, success, 50, 0, 50, "General");
    }

    public AgentResult(String agent, String summary, long executionTimeMs, String workload, boolean success,
                       int contributionScore, int redundancyScore, int efficiencyScore, String riskArea) {
        this.agent = agent;
        this.summary = summary;
        this.executionTimeMs = executionTimeMs;
        this.workload = workload;
        this.success = success;
        this.contributionScore = contributionScore;
        this.redundancyScore = redundancyScore;
        this.efficiencyScore = efficiencyScore;
        this.riskArea = riskArea;
    }
}
