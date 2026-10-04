package com.finagent.agent;
import com.finagent.model.*;
public interface Agent {
    String getName();
    AgentResult analyze(AnalysisRequest request);
}
