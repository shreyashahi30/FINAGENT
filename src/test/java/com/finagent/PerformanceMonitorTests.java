package com.finagent;

import com.finagent.model.AgentResult;
import com.finagent.model.PerformanceSnapshot;
import com.finagent.service.PerformanceMonitor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PerformanceMonitorTests {
    private final PerformanceMonitor monitor = new PerformanceMonitor();

    @Test
    void calculatesWorkloadAndSuccessRate() {
        PerformanceSnapshot s = monitor.snapshot(List.of(
                new AgentResult("A", "", 4, "LOW", true, 80, 10, 80, "Test"),
                new AgentResult("B", "", 6, "HIGH", true, 60, 20, 20, "Test")
        ));
        assertEquals(4, s.workloadUnits);
        assertEquals(100.0, s.successRate);
        assertEquals(70.0, s.averageContribution);
    }

    @Test
    void identifiesHighRedundancyAsPerformanceSignal() {
        PerformanceSnapshot s = monitor.snapshot(List.of(
                new AgentResult("Market Agent", "", 1, "MEDIUM", true, 35, 85, 17, "Market"),
                new AgentResult("News/Sentiment Agent", "", 1, "LOW", true, 30, 85, 30, "External")
        ));
        assertTrue(s.averageRedundancy >= 70);
        assertFalse(s.bottlenecks.isEmpty());
    }
}
