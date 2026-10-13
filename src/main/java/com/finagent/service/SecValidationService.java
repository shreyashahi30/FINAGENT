package com.finagent.service;

import com.finagent.model.*;
import org.springframework.stereotype.Service;
import java.util.*;

/** Runs the unchanged analysis pipeline across a reproducible slice of loaded SEC company records. */
@Service
public class SecValidationService {
    private final DatasetService dataset; private final AnalysisService analysis;
    public SecValidationService(DatasetService dataset, AnalysisService analysis){this.dataset=dataset;this.analysis=analysis;}
    public Map<String,Object> run(int requestedLimit){
        int limit=Math.min(Math.max(requestedLimit,1),50);
        List<CompanyFinancialProfile> companies=dataset.search("",limit);
        List<Map<String,Object>> rows=new ArrayList<>(); Map<String,Integer> decisions=new TreeMap<>(), risks=new TreeMap<>();
        int withLiquidity=0,withDebt=0,withMargin=0,completed=0;
        for(CompanyFinancialProfile p:companies){
            AnalysisRequest r=new AnalysisRequest();r.companyName=p.companyName;r.cik=p.cik;r.revenue=p.revenue;r.operatingProfit=p.operatingProfit;r.cash=p.cash;r.debt=p.debt;r.currentRatio=p.currentRatio;r.debtToAssets=p.debtToAssets;r.operatingMargin=p.operatingMargin;
            r.hasLiquidityData=p.liquidityDataAvailable;r.hasDebtData=p.debtDataAvailable;r.hasMarginData=p.marginDataAvailable;r.marketSignal="SEC dataset does not contain live market-price data";r.newsSignal="SEC dataset does not contain live news sentiment data";r.dataSource="SEC dataset";r.mode="ADAPTIVE";r.question="Validate adaptive analysis across SEC company financial profiles.";
            AnalysisResponse out=analysis.analyze(r,false);completed++;decisions.merge(out.decision,1,Integer::sum);risks.merge(out.overallRisk,1,Integer::sum);
            if(p.liquidityDataAvailable)withLiquidity++;if(p.debtDataAvailable)withDebt++;if(p.marginDataAvailable)withMargin++;
            Map<String,Object> row=new LinkedHashMap<>();row.put("cik",p.cik);row.put("companyName",p.companyName);row.put("form",p.form);row.put("filed",p.filed);row.put("decision",out.decision);row.put("overallRisk",out.overallRisk);row.put("finalTeamSize",out.finalTeamSize);row.put("finalWorkloadUnits",out.finalWorkloadUnits);row.put("liquidityDataAvailable",p.liquidityDataAvailable);row.put("debtDataAvailable",p.debtDataAvailable);row.put("marginDataAvailable",p.marginDataAvailable);rows.add(row);
        }
        Map<String,Object> result=new LinkedHashMap<>();result.put("requestedLimit",limit);result.put("companiesAvailable",dataset.status().get("companyCount"));result.put("companiesValidated",completed);result.put("decisionCounts",decisions);result.put("riskCounts",risks);result.put("companiesWithLiquidityData",withLiquidity);result.put("companiesWithDebtData",withDebt);result.put("companiesWithMarginData",withMargin);result.put("note","This validates execution and decision behavior on SEC-derived financial profiles. It is not ground-truth predictive accuracy; live market and news data are not included in the SEC dataset.");result.put("companies",rows);return result;
    }
}
