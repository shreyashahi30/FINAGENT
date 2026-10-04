package com.finagent.service;

import com.finagent.model.CompanyFinancialProfile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

@Service
public class DatasetService {
    private final Map<String, CompanyFinancialProfile> companies = new LinkedHashMap<>();
    private volatile boolean loaded = false;
    private volatile String datasetStatus = "No SEC dataset loaded.";
    private volatile int companyCount = 0;
    private final Path dataDir;

    public DatasetService() { this(Paths.get("data", "sec-dataset")); }

    /** Visible so tests can point the service at an isolated temp directory instead of ./data/sec-dataset. */
    public DatasetService(Path dataDir) { this.dataDir = dataDir; }

    private static final List<String> TAGS = List.of(
            "RevenueFromContractWithCustomerExcludingAssessedTax", "Revenues", "SalesRevenueNet",
            "OperatingIncomeLoss", "NetIncomeLoss", "ProfitLoss",
            "CashAndCashEquivalentsAtCarryingValue", "CashCashEquivalentsRestrictedCashAndRestrictedCashEquivalents",
            "Assets", "AssetsCurrent", "Liabilities", "LiabilitiesCurrent", "StockholdersEquity",
            "StockholdersEquityIncludingPortionAttributableToNoncontrollingInterest",
            "AccountsReceivableNetCurrent", "InventoryNet", "NetCashProvidedByUsedInOperatingActivities",
            "LongTermDebtCurrent", "LongTermDebtNoncurrent", "LongTermDebt",
            "LongTermDebtAndFinanceLeaseObligationsCurrent", "LongTermDebtAndFinanceLeaseObligationsNoncurrent",
            "ShortTermBorrowings", "ShortTermDebt"
    );

    public synchronized Map<String,Object> upload(MultipartFile[] files) throws IOException {
        if (files == null || files.length == 0) throw new IOException("Upload the unzipped SEC dataset files.");
        Files.createDirectories(dataDir);
        int saved = 0;
        for (MultipartFile file : files) {
            String name = Paths.get(Objects.requireNonNullElse(file.getOriginalFilename(), "")).getFileName().toString().toLowerCase();
            if (List.of("num.txt","sub.txt","tag.txt","pre.txt","readme.htm").contains(name)) {
                Files.copy(file.getInputStream(), dataDir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
                saved++;
            }
        }
        if (!Files.exists(dataDir.resolve("num.txt")) || !Files.exists(dataDir.resolve("sub.txt")))
            throw new IOException("The unzipped dataset must contain at least num.txt and sub.txt.");
        loadFromDirectory();
        return status();
    }

    public synchronized void loadIfPresent() {
        if (loaded) return;
        try {
            if (Files.exists(dataDir.resolve("num.txt")) && Files.exists(dataDir.resolve("sub.txt"))) loadFromDirectory();
        } catch (Exception e) {
            datasetStatus = "Dataset found but could not be loaded: " + e.getMessage();
        }
    }

    private void loadFromDirectory() throws IOException {
        Map<String, Filing> filings = readSub(dataDir.resolve("sub.txt"));
        Map<String, Map<String, Double>> factsByAdsh = readNum(dataDir.resolve("num.txt"));
        Map<String, CompanyFinancialProfile> result = new LinkedHashMap<>();
        for (Filing f : filings.values()) {
            Map<String, Double> facts = factsByAdsh.get(f.adsh);
            if (facts == null || facts.isEmpty()) continue;
            CompanyFinancialProfile p = profile(f, facts);
            if (p.revenue == 0 && p.totalAssets == 0 && p.totalLiabilities == 0) continue;
            // One record per company: retain the most recently filed usable submission.
            CompanyFinancialProfile old = result.get(p.cik);
            if (old == null || safe(p.filed).compareTo(safe(old.filed)) > 0) result.put(p.cik, p);
        }
        companies.clear();
        companies.putAll(result);
        companyCount = companies.size();
        loaded = true;
        datasetStatus = "SEC dataset loaded successfully. " + companyCount + " company records are available.";
    }

    private Map<String, Filing> readSub(Path path) throws IOException {
        Map<String,Filing> out = new HashMap<>();
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line = br.readLine();
            if (line == null) return out;
            String[] header = split(line);
            Map<String,Integer> idx = indexes(header);
            String row;
            while ((row=br.readLine()) != null) {
                String[] a=split(row);
                String adsh=get(a,idx,"adsh"), cik=get(a,idx,"cik"), name=get(a,idx,"name"), filed=get(a,idx,"filed");
                if (adsh.isBlank() || cik.isBlank() || name.isBlank()) continue;
                Filing f=new Filing(adsh,cik,name,get(a,idx,"form"),filed,get(a,idx,"period"));
                // Prefer periodic filings over amendments when dates are equal.
                Filing old=out.get(adsh); if(old==null || safe(f.filed).compareTo(safe(old.filed))>=0) out.put(adsh,f);
            }
        }
        return out;
    }

    private Map<String, Map<String, Double>> readNum(Path path) throws IOException {
        Map<String,Map<String,Double>> out=new HashMap<>();
        Map<String,Map<String,FactKey>> chosen=new HashMap<>();
        try(BufferedReader br=Files.newBufferedReader(path, StandardCharsets.UTF_8)){
            String line=br.readLine(); if(line==null) return out;
            Map<String,Integer> idx=indexes(split(line)); String row;
            while((row=br.readLine())!=null){
                String[] a=split(row); String tag=get(a,idx,"tag");
                if(!TAGS.contains(tag)) continue;
                String adsh=get(a,idx,"adsh"), value=get(a,idx,"value"), ddate=get(a,idx,"ddate"), uom=get(a,idx,"uom"), segments=get(a,idx,"segments");
                String qtrs=get(a,idx,"qtrs");
                if(adsh.isBlank()||value.isBlank()||ddate.isBlank()) continue;
                if(!("USD".equalsIgnoreCase(uom)||uom.isBlank())) continue;
                try{
                    double v=Double.parseDouble(value); int d=Integer.parseInt(ddate); int q=parseInt(qtrs);
                    Map<String,FactKey> m=chosen.computeIfAbsent(adsh,k->new HashMap<>());
                    FactKey old=m.get(tag); FactKey now=new FactKey(d,q,segments.isBlank(),v);
                    if(old==null || better(now,old)) m.put(tag,now);
                }catch(NumberFormatException ignored){}
            }
        }
        for(Map.Entry<String,Map<String,FactKey>> e:chosen.entrySet()){
            Map<String,Double> m=new HashMap<>();
            for(Map.Entry<String,FactKey> x:e.getValue().entrySet()) m.put(x.getKey(),x.getValue().value);
            out.put(e.getKey(),m);
        }
        return out;
    }

    private boolean better(FactKey now, FactKey old){
        if(now.date!=old.date) return now.date>old.date;
        if(now.consolidated!=old.consolidated) return now.consolidated;
        // A single ddate can carry more than one duration for the same tag (e.g. a 10-Q filer
        // tags revenue with qtrs=1 for the quarter AND qtrs=2/3 for the year-to-date total, both
        // stamped with the same period-end date). Prefer the shortest reported duration so the
        // extracted figure represents that period's activity rather than a cumulative total;
        // qtrs<0 marks an unparsed/missing value and is always the worst choice.
        if(now.qtrs>=0 && old.qtrs>=0 && now.qtrs!=old.qtrs) return now.qtrs<old.qtrs;
        if((now.qtrs>=0) != (old.qtrs>=0)) return now.qtrs>=0;
        return false;
    }
    private int parseInt(String s){try{return Integer.parseInt(s);}catch(Exception e){return -1;}}

    private CompanyFinancialProfile profile(Filing f, Map<String,Double> x){
        CompanyFinancialProfile p=new CompanyFinancialProfile();
        p.cik=f.cik;p.companyName=f.name;p.accessionNumber=f.adsh;p.form=f.form;p.filed=f.filed;p.period=f.period;
        p.revenue=first(x,"RevenueFromContractWithCustomerExcludingAssessedTax","Revenues","SalesRevenueNet");
        p.operatingProfit=first(x,"OperatingIncomeLoss"); p.netIncome=first(x,"NetIncomeLoss","ProfitLoss");
        p.cash=first(x,"CashAndCashEquivalentsAtCarryingValue","CashCashEquivalentsRestrictedCashAndRestrictedCashEquivalents");
        p.totalAssets=first(x,"Assets");p.totalLiabilities=first(x,"Liabilities");
        p.currentAssets=first(x,"AssetsCurrent");p.currentLiabilities=first(x,"LiabilitiesCurrent");
        p.equity=first(x,"StockholdersEquityIncludingPortionAttributableToNoncontrollingInterest","StockholdersEquity");
        // Many filers never tag the consolidated "Liabilities" total explicitly and instead let it
        // be implied by the balance-sheet identity (Assets = Liabilities + Equity). Without this
        // fallback, a company with real liabilities shows "0" simply because that one tag is
        // absent, which is wrong on its face for any filer with nonzero assets and equity.
        if(p.totalLiabilities==0 && p.totalAssets>0 && p.equity!=0) p.totalLiabilities=p.totalAssets-p.equity;
        p.receivables=first(x,"AccountsReceivableNetCurrent");p.inventory=first(x,"InventoryNet");
        p.operatingCashFlow=first(x,"NetCashProvidedByUsedInOperatingActivities");
        p.debt=first(x,"LongTermDebtCurrent","LongTermDebtAndFinanceLeaseObligationsCurrent")
                +first(x,"LongTermDebtNoncurrent","LongTermDebtAndFinanceLeaseObligationsNoncurrent","LongTermDebt")
                +first(x,"ShortTermBorrowings","ShortTermDebt");
        p.currentRatio=p.currentLiabilities>0?p.currentAssets/p.currentLiabilities:0;
        p.debtToAssets=p.totalAssets>0?p.debt/p.totalAssets:0;
        p.operatingMargin=p.revenue!=0?p.operatingProfit/p.revenue*100:0;
        // A tag being absent from the filing is different from the company reporting zero for
        // it, and the two must not be conflated: "no debt tag filed" is not the same claim as
        // "this company carries zero debt". These flags record which case actually happened so
        // callers can tell a genuinely debt-free/margin-less company apart from one where the
        // relevant XBRL fact simply was not tagged in this filing.
        p.debtDataAvailable=anyPresent(x,"LongTermDebtCurrent","LongTermDebtAndFinanceLeaseObligationsCurrent",
                "LongTermDebtNoncurrent","LongTermDebtAndFinanceLeaseObligationsNoncurrent","LongTermDebt",
                "ShortTermBorrowings","ShortTermDebt");
        p.liquidityDataAvailable=anyPresent(x,"AssetsCurrent") && anyPresent(x,"LiabilitiesCurrent");
        p.marginDataAvailable=anyPresent(x,"RevenueFromContractWithCustomerExcludingAssessedTax","Revenues","SalesRevenueNet")
                && anyPresent(x,"OperatingIncomeLoss");
        return p;
    }

    private double first(Map<String,Double> x,String... tags){for(String t:tags){Double v=x.get(t);if(v!=null)return v;}return 0;}
    private boolean anyPresent(Map<String,Double> x,String... tags){for(String t:tags) if(x.containsKey(t)) return true; return false;}
    private String[] split(String s){return s.split("\\t",-1);}
    private Map<String,Integer> indexes(String[] h){Map<String,Integer> m=new HashMap<>();for(int i=0;i<h.length;i++)m.put(h[i].trim().toLowerCase(),i);return m;}
    private String get(String[] a,Map<String,Integer> idx,String key){Integer i=idx.get(key);return i==null||i>=a.length?"":a[i].trim();}
    private String safe(String s){return s==null?"":s;}

    public synchronized List<CompanyFinancialProfile> search(String q, int limit){
        loadIfPresent(); String query=q==null?"":q.trim().toLowerCase();
        List<CompanyFinancialProfile> out=new ArrayList<>();
        for(CompanyFinancialProfile p:companies.values()) if(query.isBlank()||p.companyName.toLowerCase().contains(query)||p.cik.equals(query)){out.add(p);if(out.size()>=Math.min(Math.max(limit,1),100))break;}
        return out;
    }
    public CompanyFinancialProfile get(String cik){loadIfPresent();return companies.get(cik);}
    public Map<String,Object> status(){return Map.of("loaded",loaded,"companyCount",companyCount,"status",datasetStatus);}
    public boolean isLoaded(){loadIfPresent();return loaded;}

    private record Filing(String adsh,String cik,String name,String form,String filed,String period){}
    private record FactKey(int date,int qtrs,boolean consolidated,double value){}
}
