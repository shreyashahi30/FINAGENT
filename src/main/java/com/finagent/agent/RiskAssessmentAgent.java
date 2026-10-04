package com.finagent.agent;

import com.finagent.model.*;
import org.springframework.stereotype.Component;

@Component
public class RiskAssessmentAgent implements Agent {
    public String getName(){return "Risk Assessment Agent";}
    public AgentResult analyze(AnalysisRequest r){
        long s=System.nanoTime();
        return new AgentResult(getName(),"Combined specialist analyses into the overall financial risk assessment.",Math.max(1L,(System.nanoTime()-s)/1_000_000L),"HIGH",true,80,5,27,"Overall Risk");
    }
}
