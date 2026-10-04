package com.finagent.agent;

import com.finagent.model.*;
import org.springframework.stereotype.Component;

@Component
public class CreditRiskAgent implements Agent {
    public String getName(){return "Credit Risk Agent";}
    public AgentResult analyze(AnalysisRequest r){
        long s=System.nanoTime();
        boolean risk=r.debtToAssets>.60;
        String x=risk?"High debt-to-assets indicates elevated credit and leverage risk.":"Leverage is not exceptionally high based on the supplied ratio.";
        return new AgentResult(getName(),x,Math.max(1L,(System.nanoTime()-s)/1_000_000L),"MEDIUM",true,risk?95:40,5,risk?47:20,"Credit / Leverage");
    }
}
