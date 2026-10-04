package com.finagent.service;

import com.finagent.model.AnalysisResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AnalysisHistoryService {
    private final List<AnalysisResponse> history = new ArrayList<>();
    private static final int MAX_ITEMS = 20;

    public synchronized void add(AnalysisResponse response) {
        history.add(0, response);
        if (history.size() > MAX_ITEMS) history.remove(history.size() - 1);
    }

    public synchronized List<AnalysisResponse> recent() {
        return new ArrayList<>(history);
    }

    public synchronized void clear() {
        history.clear();
    }
}
