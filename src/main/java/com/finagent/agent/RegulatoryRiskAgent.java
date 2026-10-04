package com.finagent.agent;

import com.finagent.model.*;
import org.springframework.stereotype.Component;

@Component
public class RegulatoryRiskAgent implements Agent {
    public String getName(){return "Regulatory Risk Agent";}
    public AgentResult analyze(AnalysisRequest r){
        long s=System.nanoTime(); String x=r.newsSignal==null?"":r.newsSignal.toLowerCase();
        boolean risk=x.contains("regulatory");
        String out=risk?"The supplied information contains a regulatory concern requiring attention.":"No specific regulatory concern was identified.";
        return new AgentResult(getName(),out,Math.max(1L,(System.nanoTime()-s)/1_000_000L),"LOW",true,risk?95:35,5,risk?95:35,"Regulatory");
    }
}
