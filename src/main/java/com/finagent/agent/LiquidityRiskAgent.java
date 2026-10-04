package com.finagent.agent;

import com.finagent.model.*;
import org.springframework.stereotype.Component;

@Component
public class LiquidityRiskAgent implements Agent {
    public String getName(){return "Liquidity Risk Agent";}
    public AgentResult analyze(AnalysisRequest r){
        long s=System.nanoTime();
        boolean risk=r.currentRatio<1;
        String x=risk?"Current ratio below 1 suggests elevated short-term liquidity pressure.":"Liquidity position appears more comfortable based on the supplied current ratio.";
        return new AgentResult(getName(),x,Math.max(1L,(System.nanoTime()-s)/1_000_000L),"MEDIUM",true,risk?95:40,5,risk?47:20,"Liquidity");
    }
}
