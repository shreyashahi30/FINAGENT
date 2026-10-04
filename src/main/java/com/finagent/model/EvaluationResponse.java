package com.finagent.model;

import java.util.List;

public class EvaluationResponse {
    public int repetitions;
    public List<EvaluationRow> rows;
    public double standardAverageWorkload, adaptiveAverageWorkload;
    public double workloadChangePercent;
    public double standardAverageTeamSize, adaptiveAverageTeamSize;
    public double standardAverageContribution, adaptiveAverageContribution;
    public double standardAverageRedundancy, adaptiveAverageRedundancy;
    public double standardAverageEfficiency, adaptiveAverageEfficiency;
    public double standardAverageSuccess, adaptiveAverageSuccess;
    public double standardAverageExecutionTime, adaptiveAverageExecutionTime;

    public EvaluationResponse(int repetitions, List<EvaluationRow> rows,
                              double standardAverageWorkload, double adaptiveAverageWorkload,
                              double workloadChangePercent, double standardAverageTeamSize,
                              double adaptiveAverageTeamSize,
                              double standardAverageContribution, double adaptiveAverageContribution,
                              double standardAverageRedundancy, double adaptiveAverageRedundancy,
                              double standardAverageEfficiency, double adaptiveAverageEfficiency,
                              double standardAverageSuccess, double adaptiveAverageSuccess,
                              double standardAverageExecutionTime, double adaptiveAverageExecutionTime) {
        this.repetitions = repetitions;
        this.rows = rows;
        this.standardAverageWorkload = standardAverageWorkload;
        this.adaptiveAverageWorkload = adaptiveAverageWorkload;
        this.workloadChangePercent = workloadChangePercent;
        this.standardAverageTeamSize = standardAverageTeamSize;
        this.adaptiveAverageTeamSize = adaptiveAverageTeamSize;
        this.standardAverageContribution = standardAverageContribution;
        this.adaptiveAverageContribution = adaptiveAverageContribution;
        this.standardAverageRedundancy = standardAverageRedundancy;
        this.adaptiveAverageRedundancy = adaptiveAverageRedundancy;
        this.standardAverageEfficiency = standardAverageEfficiency;
        this.adaptiveAverageEfficiency = adaptiveAverageEfficiency;
        this.standardAverageSuccess = standardAverageSuccess;
        this.adaptiveAverageSuccess = adaptiveAverageSuccess;
        this.standardAverageExecutionTime = standardAverageExecutionTime;
        this.adaptiveAverageExecutionTime = adaptiveAverageExecutionTime;
    }

    public record EvaluationRow(String scenario, String adaptiveDecision,
                                double standardWorkload, double adaptiveWorkload,
                                double standardTeamSize, double adaptiveTeamSize) {}
}
