package com.finagent.agent;

import com.finagent.model.*;
import org.springframework.stereotype.Component;

@Component
public class NewsSentimentAgent implements Agent {
    public String getName(){return "News/Sentiment Agent";}
    public AgentResult analyze(AnalysisRequest r){
        long s=System.nanoTime(); String x=r.newsSignal==null?"":r.newsSignal.toLowerCase();
        boolean risk=x.contains("negative")||x.contains("bad")||x.contains("regulatory");
        boolean stable=isStable(x);
        String out=risk?"The supplied news signal suggests additional external or reputational risk.":"The supplied news signal does not indicate severe external risk.";
        int contribution=risk?90:stable?30:60;
        int redundancy=stable?85:20;
        int efficiency=Math.min(100, contribution);
        return new AgentResult(getName(),out,Math.max(1L,(System.nanoTime()-s)/1_000_000L),"LOW",true,contribution,redundancy,efficiency,"External / Sentiment");
    }
    private boolean isStable(String s){return s.contains("stable")||s.contains("positive")||s.contains("normal")||s.contains("strong")||s.contains("good")||s.contains("healthy");}
}
