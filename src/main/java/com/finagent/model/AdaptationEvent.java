package com.finagent.model;

public class AdaptationEvent {
    public int iteration;
    public String decision, reason, performanceEvidence;
    public int teamSizeBefore, teamSizeAfter, workloadBefore, workloadAfter;

    public AdaptationEvent(int iteration, String decision, String reason, String performanceEvidence,
                           int teamSizeBefore, int teamSizeAfter, int workloadBefore, int workloadAfter) {
        this.iteration = iteration;
        this.decision = decision;
        this.reason = reason;
        this.performanceEvidence = performanceEvidence;
        this.teamSizeBefore = teamSizeBefore;
        this.teamSizeAfter = teamSizeAfter;
        this.workloadBefore = workloadBefore;
        this.workloadAfter = workloadAfter;
    }
}
