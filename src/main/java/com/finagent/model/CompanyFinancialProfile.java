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

    public boolean debtDataAvailable;
    public boolean liquidityDataAvailable;
    public boolean marginDataAvailable;

    public CompanyFinancialProfile() {}
}
