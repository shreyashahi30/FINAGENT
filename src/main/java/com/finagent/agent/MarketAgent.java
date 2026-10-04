package com.finagent.agent;

import com.finagent.model.*;
import org.springframework.stereotype.Component;

@Component
public class MarketAgent implements Agent {
    public String getName(){return "Market Agent";}
    public AgentResult analyze(AnalysisRequest r){
        long s=System.nanoTime(); String x=r.marketSignal==null?"":r.marketSignal.toLowerCase();
        boolean risk=x.contains("negative")||x.contains("weak")||x.contains("down");
        boolean stable=isStable(x);
        String out=risk?"The supplied market signal indicates elevated market-related risk.":"The supplied market signal does not indicate severe market pressure.";
        int contribution=risk?85:stable?35:60;
        int redundancy=stable?85:20;
        int efficiency=Math.min(100, contribution/2);
        return new AgentResult(getName(),out,Math.max(1L,(System.nanoTime()-s)/1_000_000L),"MEDIUM",true,contribution,redundancy,efficiency,"Market");
    }
    private boolean isStable(String s){return s.contains("stable")||s.contains("positive")||s.contains("normal")||s.contains("strong")||s.contains("good")||s.contains("healthy");}
}
