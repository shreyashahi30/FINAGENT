package com.finagent.service;

import com.finagent.agent.*;
import com.finagent.model.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AnalysisService {
    private final FinancialStatementAgent financial;
    private final MarketAgent market;
    private final NewsSentimentAgent news;
    private final RiskAssessmentAgent risk;
    private final LiquidityRiskAgent liquidity;
    private final CreditRiskAgent credit;
    private final RegulatoryRiskAgent regulatory;
    private final ExternalConditionsAgent externalConditions;
    private final EvolutionDecisionService evolution;
    private final PerformanceMonitor monitor;
    private final AnalysisHistoryService history;

    public AnalysisService(FinancialStatementAgent f, MarketAgent m, NewsSentimentAgent n,
                           RiskAssessmentAgent r, LiquidityRiskAgent l, CreditRiskAgent c,
                           RegulatoryRiskAgent g, ExternalConditionsAgent ec,
                           EvolutionDecisionService e, PerformanceMonitor pm,
                           AnalysisHistoryService history) {
        financial=f; market=m; news=n; risk=r; liquidity=l; credit=c; regulatory=g;
        externalConditions=ec; evolution=e; monitor=pm; this.history=history;
    }

    public AnalysisResponse analyze(AnalysisRequest r) {
        return analyze(r, true);
    }

    public AnalysisResponse analyze(AnalysisRequest r, boolean recordHistory) {
        long start=System.nanoTime();
        List<AgentResult> baseline=new ArrayList<>();
        List<AgentResult> finalResults=new ArrayList<>();
        List<String> active=new ArrayList<>();

        addInitial(baseline, active, financial, r);
        addInitial(baseline, active, market, r);
        addInitial(baseline, active, news, r);
        addInitial(baseline, active, risk, r);

        PerformanceSnapshot baselinePerformance=monitor.snapshot(baseline);
        int initialTeamSize=active.size();
        int initialWorkload=baselinePerformance.workloadUnits;

        String decision="CONTINUE";
        String reason="Standard analysis uses the fixed team.";
        String rule="Fixed baseline: Standard Analysis never changes the team.";
        String evidence="Baseline performance was recorded for comparison.";
        List<AdaptationEvent> adaptationHistory=new ArrayList<>();

        if ("ADAPTIVE".equalsIgnoreCase(r.mode)) {
            var d=evolution.decide(r, baseline, monitor);
            decision=d.type(); reason=d.reason(); rule=d.rule(); evidence=d.performanceEvidence();

            int beforeTeam=active.size();
            int beforeWorkload=workloadUnitsForCurrentTeam(baseline, active);

            // Rebuild the active specialist result list after the lifecycle decision.
            if ("CREATE".equals(d.type())) {
                if ("Liquidity Risk Agent".equals(d.agentToAdd())) { active.add(liquidity.getName()); }
                else if ("Credit Risk Agent".equals(d.agentToAdd())) { active.add(credit.getName()); }
                else if ("Regulatory Risk Agent".equals(d.agentToAdd())) { active.add(regulatory.getName()); }
            }
            if ("REMOVE".equals(d.type())) {
                active.removeAll(d.agentsToRemove());
            }
            if ("MERGE".equals(d.type())) {
                active.removeAll(d.agentsToMerge());
                active.add(externalConditions.getName());
            }

            int afterTeam=active.size();
            int afterWorkload=workloadAfterDecision(d, baseline);
            adaptationHistory.add(new AdaptationEvent(1, d.type(), d.reason(), d.performanceEvidence(),
                    beforeTeam, afterTeam, beforeWorkload, afterWorkload));
        }

        // Execute the final team again. This makes the comparison explicit:
        // baseline team performance -> evolution decision -> final team performance.
        finalResults.clear();
        if (active.contains(financial.getName())) addResult(finalResults, financial, r);
        if (active.contains(market.getName())) addResult(finalResults, market, r);
        if (active.contains(news.getName())) addResult(finalResults, news, r);
        if (active.contains(liquidity.getName())) addResult(finalResults, liquidity, r);
        if (active.contains(credit.getName())) addResult(finalResults, credit, r);
        if (active.contains(regulatory.getName())) addResult(finalResults, regulatory, r);
        if (active.contains(externalConditions.getName())) addResult(finalResults, externalConditions, r);

        // Keep Risk Assessment as the final aggregator in the team.
        active.remove(risk.getName());
        active.add(risk.getName());
        finalResults.removeIf(a -> risk.getName().equals(a.agent));
        finalResults.add(risk.analyze(r));

        PerformanceSnapshot finalPerformance=monitor.snapshot(finalResults);
        int finalTeamSize=active.size();
        int finalWorkload=finalPerformance.workloadUnits;
        long totalMs=Math.max(1L,(System.nanoTime()-start)/1_000_000L);

        String overall=calculateRisk(r);
        AnalysisResponse x=new AnalysisResponse();
        x.companyName=r.companyName;
        x.mode="ADAPTIVE".equalsIgnoreCase(r.mode)?"Adaptive Analysis":"Standard Analysis";
        x.overallRisk=overall;
        x.financialRisk=financialRisk(r);
        x.marketRisk=marketRisk(r);
        x.newsRisk=newsRisk(r);
        x.decision=decision; x.decisionReason=reason; x.decisionRule=rule;
        x.performanceEvidence=evidence;
        x.activeAgents=active; x.agentResults=finalResults;
        x.adaptationHistory=adaptationHistory;
        x.baselinePerformance=baselinePerformance;
        x.finalPerformance=finalPerformance;
        x.recommendations=recommendations(overall);
        x.totalExecutionTimeMs=totalMs;
        x.initialTeamSize=initialTeamSize; x.finalTeamSize=finalTeamSize;
        x.initialWorkloadUnits=initialWorkload; x.finalWorkloadUnits=finalWorkload;
        x.workloadChangePercent=monitor.percentageChange(initialWorkload,finalWorkload);
        double baseEfficiency=monitor.efficiency(baselinePerformance);
        double finalEfficiency=monitor.efficiency(finalPerformance);
        x.efficiencyChangePercent=baseEfficiency==0?0:((finalEfficiency-baseEfficiency)/baseEfficiency)*100.0;
        x.performanceSummary=performanceSummary(x.mode,decision,baselinePerformance,finalPerformance);
        x.status="Completed";
        if (recordHistory) history.add(x);
        return x;
    }

    private void addInitial(List<AgentResult> results,List<String> active,Agent agent,AnalysisRequest r){
        results.add(agent.analyze(r));
        if (!active.contains(agent.getName())) active.add(agent.getName());
    }
    private void addResult(List<AgentResult> results,Agent agent,AnalysisRequest r){ results.add(agent.analyze(r)); }

    private int workloadUnitsForCurrentTeam(List<AgentResult> baseline,List<String> active){
        int total=0;
        for(AgentResult a:baseline) if(active.contains(a.agent)) total+=monitor.workloadUnits(a.workload);
        return total;
    }

    private int workloadAfterDecision(EvolutionDecisionService.Decision d,List<AgentResult> baseline){
        List<AgentResult> copy=new ArrayList<>(baseline);
        if("CREATE".equals(d.type())) {
            String workload="MEDIUM";
            if("Regulatory Risk Agent".equals(d.agentToAdd())) workload="LOW";
            copy.add(new AgentResult(d.agentToAdd(),"",1,workload,true));
        } else if("REMOVE".equals(d.type())) copy.removeIf(a->d.agentsToRemove().contains(a.agent));
        else if("MERGE".equals(d.type())) {
            copy.removeIf(a->d.agentsToMerge().contains(a.agent));
            copy.add(new AgentResult("External Conditions Agent","",1,"MEDIUM",true));
        }
        return monitor.snapshot(copy).workloadUnits;
    }

    private String performanceSummary(String mode,String decision,PerformanceSnapshot before,PerformanceSnapshot after){
        if("Standard Analysis".equals(mode))
            return "Fixed baseline completed with no lifecycle change. The baseline metrics are retained as the reference point.";
        double workloadDelta=after.workloadUnits-before.workloadUnits;
        String workload=workloadDelta==0?"Workload remained unchanged.":workloadDelta<0
                ?String.format("Workload proxy decreased by %.0f%%.",Math.abs(workloadDelta)*100.0/before.workloadUnits)
                :String.format("Workload proxy increased by %.0f%% to add specialist coverage.",workloadDelta*100.0/before.workloadUnits);
        return "Adaptive decision " + decision + ". " + workload + " Baseline success rate was "
                + String.format("%.0f",before.successRate) + "% and final success rate was "
                + String.format("%.0f",after.successRate) + "%.";
    }

    private String calculateRisk(AnalysisRequest r){
        int s=0;
        if(r.hasLiquidityData && r.currentRatio<1)s+=2; else if(r.hasLiquidityData && r.currentRatio<1.5)s++;
        if(r.hasDebtData && r.debtToAssets>.60)s+=2; else if(r.hasDebtData && r.debtToAssets>.45)s++;
        if(r.hasMarginData && r.operatingMargin<10)s+=2; else if(r.hasMarginData && r.operatingMargin<20)s++;
        String m=r.marketSignal==null?"":r.marketSignal.toLowerCase();
        String n=r.newsSignal==null?"":r.newsSignal.toLowerCase();
        if(m.contains("negative")||m.contains("weak")||m.contains("down"))s++;
        if(n.contains("negative")||n.contains("bad")||n.contains("regulatory"))s++;
        return s>=5?"HIGH":s>=3?"MEDIUM":"LOW";
    }
    private String financialRisk(AnalysisRequest r){return (r.hasDebtData&&r.debtToAssets>.60)||(r.hasLiquidityData&&r.currentRatio<1)||(r.hasMarginData&&r.operatingMargin<10)?"Elevated":"Moderate to Low";}
    private String marketRisk(AnalysisRequest r){String s=r.marketSignal==null?"":r.marketSignal.toLowerCase();return s.contains("negative")||s.contains("weak")||s.contains("down")?"Elevated":"Moderate to Low";}
    private String newsRisk(AnalysisRequest r){String s=r.newsSignal==null?"":r.newsSignal.toLowerCase();return s.contains("negative")||s.contains("bad")||s.contains("regulatory")?"Elevated":"Moderate to Low";}
    private String recommendations(String risk){return "HIGH".equals(risk)?"Review leverage and liquidity, monitor market/news conditions, and consider additional risk controls.":"MEDIUM".equals(risk)?"Monitor financial ratios, debt levels, market conditions and external developments regularly.":"Continue monitoring financial indicators and external conditions.";}
}
