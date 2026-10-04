package com.finagent.model;
public class AnalysisRequest {
    public String companyName;
    public double revenue, operatingProfit, cash, debt, currentRatio, debtToAssets, operatingMargin;
    public String marketSignal, newsSignal, question, mode, dataSource, cik;
    public boolean hasLiquidityData=true, hasDebtData=true, hasMarginData=true;
}
