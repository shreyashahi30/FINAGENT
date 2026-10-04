package com.finagent.agent;

import com.finagent.model.*;
import org.springframework.stereotype.Component;

@Component
public class FinancialStatementAgent implements Agent {
    public String getName(){return "Financial Statement Agent";}
    public AgentResult analyze(AnalysisRequest r){
        long s=System.nanoTime();
        boolean pressure = r.debt > r.cash && r.operatingMargin < 10;
        boolean strong = r.operatingMargin >= 20 && r.currentRatio >= 1.5;
        String x=pressure ? "Financial pressure is elevated because debt is high relative to cash and margin is low."
                : strong ? "Financial indicators appear relatively strong." : "Financial indicators show a mixed risk profile.";
        int contribution = pressure ? 90 : strong ? 45 : 65;
        return result(x, s, contribution, pressure ? "Financial" : "Financial Stability");
    }
    private AgentResult result(String x,long s,int contribution,String area){
        long ms=Math.max(1L,(System.nanoTime()-s)/1_000_000L);
        int efficiency=Math.min(100, contribution/2);
        return new AgentResult(getName(),x,ms,"MEDIUM",true,contribution,10,efficiency,area);
    }
}
