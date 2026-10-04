package com.finagent.model;

public class CompanyFinancialProfile {
    public String cik;
    public String companyName;
    public String accessionNumber;
    public String form;
    public String filed;
    public String period;
    public double revenue;
    public double operatingProfit;
    public double cash;
    public double debt;
    public double totalAssets;
    public double totalLiabilities;
    public double equity;
    public double currentAssets;
    public double currentLiabilities;
    public double netIncome;
    public double operatingCashFlow;
    public double receivables;
    public double inventory;
    public double currentRatio;
    public double debtToAssets;
    public double operatingMargin;

    // True only when the underlying XBRL tag(s) were actually present in the filing, not
    // merely when the derived value happens to be non-zero. A company that never tags any
    // debt-related fact reads as debt=0 the same as a company that filed zero debt; without
    // this flag those two cases were indistinguishable and the app silently treated "missing"
    // as "known zero" for the purpose of triggering the Credit/Liquidity/Margin specialist agents.
    public boolean debtDataAvailable;
    public boolean liquidityDataAvailable;
    public boolean marginDataAvailable;

    public CompanyFinancialProfile() {}
}
