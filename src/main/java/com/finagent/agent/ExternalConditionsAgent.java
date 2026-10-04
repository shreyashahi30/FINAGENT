package com.finagent.agent;

import com.finagent.model.*;
import org.springframework.stereotype.Component;

@Component
public class ExternalConditionsAgent implements Agent {
    @Override public String getName(){return "External Conditions Agent";}
    @Override public AgentResult analyze(AnalysisRequest r){
        long start=System.nanoTime();
        String market=r.marketSignal==null?"":r.marketSignal.toLowerCase();
        String news=r.newsSignal==null?"":r.newsSignal.toLowerCase();
        String summary=(market.contains("stable")&&(news.contains("positive")||news.contains("stable")))
                ?"Merged market and news signals indicate stable external conditions with limited additional external risk."
                :"Merged external-condition analysis combines the supplied market and news signals.";
        return new AgentResult(getName(),summary,Math.max(1L,(System.nanoTime()-start)/1_000_000L),"MEDIUM",true,70,5,35,"External Conditions");
    }
}
