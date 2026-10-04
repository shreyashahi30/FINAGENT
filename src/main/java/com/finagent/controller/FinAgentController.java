package com.finagent.controller;

import com.finagent.model.AnalysisRequest;
import com.finagent.model.AnalysisResponse;
import com.finagent.service.AnalysisHistoryService;
import com.finagent.service.AnalysisService;
import com.finagent.service.EvaluationService;
import com.finagent.service.DatasetService;
import com.finagent.model.CompanyFinancialProfile;
import com.finagent.model.EvaluationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FinAgentController {
    private final AnalysisService service;
    private final AnalysisHistoryService history;
    private final EvaluationService evaluation;
    private final DatasetService dataset;

    public FinAgentController(AnalysisService service, AnalysisHistoryService history, EvaluationService evaluation, DatasetService dataset) {
        this.service=service;
        this.history=history;
        this.evaluation=evaluation;
        this.dataset=dataset;
    }

    @PostMapping("/analyze")
    public AnalysisResponse analyze(@RequestBody AnalysisRequest request) {
        applyDatasetRecord(request);
        validate(request);
        return service.analyze(request);
    }

    @GetMapping("/history")
    public List<AnalysisResponse> history() { return history.recent(); }

    @DeleteMapping("/history")
    public Map<String,String> clearHistory() {
        history.clear();
        return Map.of("status","History cleared");
    }

    @GetMapping("/evaluate")
    public EvaluationResponse evaluate(@RequestParam(defaultValue="3") int repetitions) { return evaluation.run(repetitions); }

    @PostMapping("/dataset/upload")
    public Map<String,Object> uploadDataset(@RequestParam("files") org.springframework.web.multipart.MultipartFile[] files) {
        try { return dataset.upload(files); }
        catch (Exception e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e); }
    }

    @GetMapping("/dataset/status")
    public Map<String,Object> datasetStatus() { return dataset.status(); }

    @GetMapping("/dataset/companies")
    public List<CompanyFinancialProfile> companies(@RequestParam(defaultValue="") String q, @RequestParam(defaultValue="30") int limit) {
        return dataset.search(q, limit);
    }

    @GetMapping("/dataset/company/{cik}")
    public CompanyFinancialProfile company(@PathVariable String cik) {
        CompanyFinancialProfile p=dataset.get(cik);
        if(p==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found in loaded SEC dataset.");
        return p;
    }

    @GetMapping("/health")
    public Map<String,String> health() { return Map.of("status","UP","service","FinAgent"); }

    private void applyDatasetRecord(AnalysisRequest r) {
        if (r == null || r.cik == null || r.cik.isBlank()) return;
        CompanyFinancialProfile p=dataset.get(r.cik);
        if(p==null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected company is not available in the loaded SEC dataset.");
        r.companyName=p.companyName; r.revenue=p.revenue; r.operatingProfit=p.operatingProfit; r.cash=p.cash; r.debt=p.debt;
        r.currentRatio=p.currentRatio; r.debtToAssets=p.debtToAssets; r.operatingMargin=p.operatingMargin;
        r.hasLiquidityData=p.liquidityDataAvailable;
        r.hasDebtData=p.debtDataAvailable;
        r.hasMarginData=p.marginDataAvailable;
        r.dataSource="SEC 2026 Q2";
        if(r.marketSignal==null||r.marketSignal.isBlank()) r.marketSignal="SEC dataset does not contain live market-price data";
        if(r.newsSignal==null||r.newsSignal.isBlank()) r.newsSignal="SEC dataset does not contain live news sentiment data";
    }

    private void validate(AnalysisRequest r) {
        if (r == null) bad("Request body is required.");
        if (r.companyName == null || r.companyName.isBlank()) bad("Company name is required.");
        // Revenue, cash and debt are magnitudes extracted from XBRL facts and cannot legitimately
        // be negative. Operating profit is deliberately NOT bounded here: real SEC filings
        // routinely report an operating loss (OperatingIncomeLoss < 0), and that is exactly the
        // kind of company this tool needs to be able to analyze rather than reject.
        if (r.revenue < 0 || r.cash < 0 || r.debt < 0) bad("Financial amounts cannot be negative.");
        // Current ratio and debt-to-assets are non-negative by construction (see DatasetService),
        // but no upper bound is enforced: highly leveraged real companies can and do exceed a
        // debt-to-assets ratio of 1.0, and operating margin can be far below -100% for
        // low-revenue, high-loss companies. Rejecting those is rejecting real data, not bad input.
        if (r.currentRatio < 0 || r.debtToAssets < 0) bad("Please enter valid financial ratios.");
        if (!Double.isFinite(r.currentRatio) || !Double.isFinite(r.debtToAssets) || !Double.isFinite(r.operatingMargin))
            bad("Please enter valid financial ratios.");
        if (r.mode == null || !("STANDARD".equalsIgnoreCase(r.mode) || "ADAPTIVE".equalsIgnoreCase(r.mode)))
            bad("Mode must be STANDARD or ADAPTIVE.");
    }

    private void bad(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
